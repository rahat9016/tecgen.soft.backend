package com.familiemunshi;

import static org.junit.jupiter.api.Assertions.*;

import com.familiemunshi.common.utils.*;
import com.familiemunshi.jpa.daos.*;
import com.familiemunshi.jpa.repositories.*;
import com.familiemunshi.service.*;
import com.familiemunshi.service.domain.SaasCommands;
import com.familiemunshi.service.enums.RoleScope;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=${TEST_CORE_DATABASE_URL}",
      "spring.datasource.username=${TEST_DATABASE_USERNAME}",
      "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
      "jwt.secret=test-only-secret-at-least-thirty-two-characters",
      "internal.service-key=test-only-service-key-at-least-thirty-two-characters",
      "branch-admin.email-domain=legacy.invalid",
      "branch-admin.common-password=Test-only-pass-123!",
      "storage.type=s3",
      "aws.s3.region=eu-north-1",
      "aws.s3.bucket-name=test-only",
      "spring.cache.type=none"
    })
@EnabledIfEnvironmentVariable(named = "TEST_CORE_DATABASE_URL", matches = ".+")
class CoreIntegrationTest {
  @Autowired SaasService saas;
  @Autowired UserRepository users;
  @Autowired RoleRepository roles;
  @Autowired UserPlatformRoleRepository assignments;
  @Autowired JwtUtil jwt;
  @Autowired JdbcTemplate jdbc;
  @org.springframework.boot.test.web.server.LocalServerPort int port;
  UUID admin, user, other;
  UUID organization;
  UUID branch;

  @BeforeEach
  void setup() {
    String salt = Util.generateSalt();
    var a =
        UserDao.builder()
            .firstName("Super")
            .lastName("Admin")
            .email(UUID.randomUUID() + "@test.invalid")
            .password(Util.hashPassword("test-password", salt))
            .salt(salt)
            .isActive(true)
            .build();
    admin = users.saveAndFlush(a).getId();
    var r =
        RoleDao.builder()
            .name("SYS_SUPER_ADMIN")
            .scope(RoleScope.PLATFORM)
            .isActive(true)
            .isSystem(true)
            .build();
    roles.saveAndFlush(r);
    assignments.saveAndFlush(UserPlatformRoleDao.builder().user(a).role(r).build());
    user = createUser();
    other = createUser();
    organization =
        saas.createOrganization(
                new SaasCommands.Organization("Test " + UUID.randomUUID(), user), admin)
            .id();
    var p =
        saas.createPlan(
            new SaasCommands.Plan(
                "PLAN_" + UUID.randomUUID(),
                "Basic",
                "TURF",
                "test",
                "BDT",
                new BigDecimal("100"),
                Set.of("COURT_MANAGEMENT", "BOOKING_MANAGEMENT"),
                Map.of("MAX_BRANCHES", 2, "MAX_COURTS", 1),
                true,
                true),
            admin);
    saas.subscribe(
        organization,
        new SaasCommands.Subscription(
            p.id(),
            "ACTIVE",
            "MONTHLY",
            Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(3600),
            new BigDecimal("90")),
        admin);
    branch =
        saas.createBranch(
                organization,
                new SaasCommands.Branch("Branch " + UUID.randomUUID(), "Address", "123"))
            .id();
  }

  UUID createUser() {
    String salt = Util.generateSalt();
    return users
        .saveAndFlush(
            UserDao.builder()
                .firstName("Org")
                .lastName("Admin")
                .email(UUID.randomUUID() + "@test.invalid")
                .password(Util.hashPassword("test-password", salt))
                .salt(salt)
                .isActive(true)
                .build())
        .getId();
  }

  @Test
  void tenantPermissionsAndSnapshots() {
    assertTrue(
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_CREATE"))
            .allowed());
    assertFalse(
        saas.check(other, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_READ"))
            .allowed());
    assertEquals(
        "FEATURE_DISABLED",
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_PAYMENT_READ"))
            .reasonCode());
    saas.patchPermissions(organization, Set.of("TURF_COURT_READ"));
    assertEquals(
        "PERMISSION_DENIED",
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_CREATE"))
            .reasonCode());
    assertEquals(
        new BigDecimal("90.00"),
        jdbc.queryForObject(
            "select agreed_price from subscriptions where organization_id=? and status='ACTIVE'",
            BigDecimal.class,
            organization));
  }

  @Test
  void adminConstraintAndBranchProvisioning() {
    assertThrows(
        Exception.class,
        () -> jdbc.update("update organizations set admin_user_id=null where id=?", organization));
    assertEquals(
        0,
        jdbc.queryForObject(
            "select count(*) from user_branch_roles where branch_id=?", Integer.class, branch));
    saas.patchOrganization(
        organization, new SaasCommands.OrganizationPatch(null, null, other), admin);
    assertFalse(
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_READ"))
            .allowed());
    assertTrue(
        saas.check(other, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_READ"))
            .allowed());
    assertThrows(
        Exception.class,
        () ->
            saas.patchOrganization(
                organization, new SaasCommands.OrganizationPatch(null, false, null), user));
  }

  @Test
  void serviceAndUserCredentialsAreIndependent() throws Exception {
    String url = "http://localhost:" + port + "/internal/v1/access/check";
    String body =
        "{\"organizationId\":\""
            + organization
            + "\",\"branchId\":\""
            + branch
            + "\",\"product\":\"TURF\",\"permissionCode\":\"TURF_COURT_READ\"}";
    assertEquals(401, post(url, body, null, jwt.generateUserToken(user, "org@test.invalid")));
    assertEquals(
        401, post(url, body, "test-only-service-key-at-least-thirty-two-characters", "bad"));
    assertEquals(
        401,
        post(
            url,
            body,
            "test-only-service-key-at-least-thirty-two-characters",
            jwt.generateUserRefreshToken(user, "org@test.invalid")));
    assertEquals(
        200,
        post(
            url,
            body,
            "test-only-service-key-at-least-thirty-two-characters",
            jwt.generateUserToken(user, "org@test.invalid")));
  }

  int post(String url, String body, String key, String token) throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.ofString(body));
    if (key != null) b.header("X-Service-Key", key);
    return HttpClient.newHttpClient()
        .send(b.build(), HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  @Test
  void unverifiedDomainCannotResolve() {
    var d =
        saas.addDomain(
            organization, new SaasCommands.Domain(UUID.randomUUID() + ".example.invalid", "TURF"));
    assertThrows(Exception.class, () -> saas.resolve(d.hostname()));
    jdbc.update("update tenant_domains set verified_at=now() where id=?", d.id());
    assertEquals(organization, saas.resolve(d.hostname()).organizationId());
  }

  @Test
  void packageVersionsPreserveExistingContracts() {
    var original = saas.entitlements(organization, "TURF");
    saas.createPlan(
        new SaasCommands.Plan(
            original.packageCode(),
            "Changed",
            "TURF",
            "changed",
            "BDT",
            new BigDecimal("200"),
            Set.of("BASIC_REPORTS"),
            Map.of("MAX_COURTS", 0, "MAX_BRANCHES", 0),
            true,
            true),
        admin);
    var effective = saas.entitlements(organization, "TURF");
    assertEquals(original.packageId(), effective.packageId());
    assertTrue(effective.features().contains("COURT_MANAGEMENT"));
    saas.packageAvailability(original.packageId(), false, false);
    assertTrue(saas.entitlements(organization, "TURF").active());
  }

  @Test
  void subscriptionSuspensionDeniesAccess() {
    var e = saas.entitlements(organization, "TURF");
    saas.subscribe(
        organization,
        new SaasCommands.Subscription(
            e.packageId(),
            "SUSPENDED",
            "MONTHLY",
            Instant.now().minusSeconds(1),
            Instant.now().plusSeconds(1000),
            BigDecimal.TEN),
        admin);
    assertEquals("SUSPENDED", saas.entitlements(organization, "TURF").subscriptionStatus());
    assertFalse(
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_READ"))
            .allowed());
  }

  @Test
  void branchOwnershipAndInactiveUsersDenied() {
    assertEquals(
        "BRANCH_ACCESS_DENIED",
        saas.check(
                user,
                new SaasCommands.Access(organization, UUID.randomUUID(), "TURF", "TURF_COURT_READ"))
            .reasonCode());
    jdbc.update("update users set is_active=false where id=?", user);
    assertEquals(
        "USER_INACTIVE",
        saas.check(user, new SaasCommands.Access(organization, branch, "TURF", "TURF_COURT_READ"))
            .reasonCode());
  }

  @Test
  void concurrentBranchLimitEnforced() throws Exception {
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start = new java.util.concurrent.CountDownLatch(1);
      var jobs = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
      for (int i = 0; i < 2; i++)
        jobs.add(
            executor.submit(
                () -> {
                  start.await();
                  try {
                    saas.createBranch(
                        organization,
                        new SaasCommands.Branch(
                            "Concurrent " + UUID.randomUUID(), "address", "123"));
                    return true;
                  } catch (Exception ex) {
                    return false;
                  }
                }));
      start.countDown();
      int successes = 0;
      for (var job : jobs) if (job.get(20, java.util.concurrent.TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    }
  }
}
