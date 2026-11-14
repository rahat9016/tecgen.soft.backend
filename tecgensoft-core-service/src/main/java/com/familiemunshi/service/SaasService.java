package com.familiemunshi.service;

import com.familiemunshi.common.policy.TurfFeatures;
import com.familiemunshi.jpa.daos.*;
import com.familiemunshi.jpa.repositories.*;
import com.familiemunshi.mapper.SaasMapper;
import com.familiemunshi.service.domain.SaasCommands.*;
import com.familiemunshi.service.domain.SaasModels;
import com.familiemunshi.service.enums.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.net.IDN;
import java.time.*;
import java.util.*;
import javax.naming.directory.InitialDirContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class SaasService {
  private final EntityManager em;
  private final OrganizationRepository organizations;
  private final ProductPackageRepository packages;
  private final SubscriptionRepository subscriptions;
  private final TenantDomainRepository domains;
  private final UserRepository users;
  private final RoleRepository roles;
  private final PermissionRepository permissions;
  private final BranchRepository branches;
  private final AdminAssignmentAuditRepository audit;
  private final UserPlatformRoleRepository platformRoles;

  public boolean isSuperAdmin(Authentication a) {
    if (a == null || !a.isAuthenticated()) return false;
    try {
      return platformRoles
          .findEffectiveRoleNames(UUID.fromString(a.getName()), RoleScope.PLATFORM)
          .contains("SYS_SUPER_ADMIN");
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public void requireSuperAdmin(UUID id) {
    if (!platformRoles.findEffectiveRoleNames(id, RoleScope.PLATFORM).contains("SYS_SUPER_ADMIN"))
      throw new AccessDeniedException("Super Admin required");
  }

  public OrganizationDao organization(UUID id) {
    return organizations
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
  }

  public SaasModels.Organization createOrganization(Organization r, UUID actor) {
    requireSuperAdmin(actor);
    activeUser(r.adminUserId());
    RoleDao role =
        RoleDao.builder()
            .name("ORGANIZATION_ADMIN")
            .scope(RoleScope.ORGANIZATION)
            .isSystem(true)
            .isActive(true)
            .permissions(
                new HashSet<>(
                    permissions.findAllByNameIn(
                        Arrays.stream(PermissionCode.values())
                            .map(PermissionCode::getCode)
                            .filter(x -> x.startsWith("TURF_"))
                            .toList())))
            .build();
    roles.saveAndFlush(role);
    OrganizationDao o = new OrganizationDao();
    o.setName(r.name());
    o.setActive(true);
    o.setAdminUserId(r.adminUserId());
    o.setAdminRoleId(role.getId());
    organizations.saveAndFlush(o);
    audit(o, actor);
    return SaasMapper.toEntity(o);
  }

  private void activeUser(UUID id) {
    if (!users.existsByIdAndIsActiveTrue(id))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user must be active");
  }

  private void audit(OrganizationDao o, UUID actor) {
    var a = new AdminAssignmentAuditDao();
    a.setOrganizationId(o.getId());
    a.setUserId(o.getAdminUserId());
    a.setActorId(actor);
    audit.save(a);
  }

  public SaasModels.Organization patchOrganization(UUID id, OrganizationPatch r, UUID actor) {
    requireSuperAdmin(actor);
    var o = organization(id);
    em.lock(o, LockModeType.PESSIMISTIC_WRITE);
    if (r.name() != null) {
      if (r.name().isBlank())
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name required");
      o.setName(r.name());
    }
    if (r.active() != null) o.setActive(r.active());
    if (r.adminUserId() != null) {
      activeUser(r.adminUserId());
      o.setAdminUserId(r.adminUserId());
      audit(o, actor);
    }
    return SaasMapper.toEntity(organizations.saveAndFlush(o));
  }

  public SaasModels.ProductPackage createPlan(Plan r, UUID actor) {
    requireSuperAdmin(actor);
    if (!"TURF".equals(r.product())
        || r.currency() == null
        || !TurfFeatures.ALL.containsAll(r.features())
        || !Set.of("MAX_COURTS", "MAX_BRANCHES").containsAll(r.limits().keySet())
        || r.limits().values().stream().anyMatch(x -> x == null || x < 0))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid product, features or limits");
    // Serialize version allocation per package code, including the first version.
    em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:code,0))")
        .setParameter("code", r.code())
        .getSingleResult();
    int v =
        em.createQuery(
                    "select coalesce(max(p.version),0) from ProductPackageDao p where p.code=:code",
                    Integer.class)
                .setParameter("code", r.code())
                .getSingleResult()
            + 1;
    var p = new ProductPackageDao();
    p.setCode(r.code());
    p.setDisplayName(r.displayName());
    p.setProduct(r.product());
    p.setDescription(r.description());
    p.setCurrency(r.currency());
    p.setPrice(r.price());
    p.setVersion(v);
    p.setFeatures(Set.copyOf(r.features()));
    p.setLimits(Map.copyOf(r.limits()));
    p.setActive(r.active());
    p.setAvailableForSale(r.availableForSale());
    return SaasMapper.toEntity(packages.saveAndFlush(p));
  }

  public SaasModels.Subscription subscribe(UUID id, Subscription r, UUID actor) {
    requireSuperAdmin(actor);
    var o = organization(id);
    em.lock(o, LockModeType.PESSIMISTIC_WRITE);
    var p =
        packages
            .findById(r.packageId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
    if (!p.isActive()
        || !p.isAvailableForSale()
        || !r.endsAt().isAfter(r.startsAt())
        || r.status() == null
        || r.billingPeriod() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid subscription");
    em.createQuery(
            "update SubscriptionDao s set s.status='REPLACED' where s.organizationId=:id and"
                + " s.product=:product and s.status='ACTIVE'")
        .setParameter("id", id)
        .setParameter("product", p.getProduct())
        .executeUpdate();
    var s = new SubscriptionDao();
    s.setOrganizationId(id);
    s.setPackageId(p.getId());
    s.setProduct(p.getProduct());
    s.setStatus(r.status());
    s.setBillingPeriod(r.billingPeriod());
    s.setStartsAt(r.startsAt());
    s.setEndsAt(r.endsAt());
    s.setAgreedPrice(r.agreedPrice());
    s.setCurrency(p.getCurrency());
    return SaasMapper.toEntity(subscriptions.saveAndFlush(s));
  }

  public record Entitlements(
      UUID organizationId,
      UUID packageId,
      String packageCode,
      int version,
      String subscriptionStatus,
      boolean active,
      Set<String> features,
      Map<String, Integer> limits) {}

  public Entitlements entitlements(UUID id, String product) {
    var o = organization(id);
    var found =
        em.createQuery(
                "select s from SubscriptionDao s where s.organizationId=:id and s.product=:product"
                    + " order by s.createdAt desc,s.id desc",
                SubscriptionDao.class)
            .setParameter("id", id)
            .setParameter("product", product)
            .setMaxResults(1)
            .getResultList();
    if (found.isEmpty())
      return new Entitlements(id, null, null, 0, "INACTIVE", false, Set.of(), Map.of());
    var s = found.getFirst();
    var p = packages.findById(s.getPackageId()).orElseThrow();
    var now = Instant.now();
    boolean active =
        o.isActive()
            && "ACTIVE".equals(s.getStatus())
            && !now.isBefore(s.getStartsAt())
            && now.isBefore(s.getEndsAt());
    String status = s.getStatus();
    if ("ACTIVE".equals(status)) {
      if (now.isBefore(s.getStartsAt())) status = "NOT_STARTED";
      else if (!now.isBefore(s.getEndsAt())) status = "EXPIRED";
    }
    return new Entitlements(
        id, p.getId(), p.getCode(), p.getVersion(), status, active, p.getFeatures(), p.getLimits());
  }

  public record Decision(boolean allowed, String reasonCode, UUID userId) {
    public Decision(boolean allowed, String reasonCode) {
      this(allowed, reasonCode, null);
    }
  }

  public Decision check(UUID user, Access r) {
    if (!users.existsByIdAndIsActiveTrue(user)) return new Decision(false, "USER_INACTIVE");
    var o = organization(r.organizationId());
    if (!o.isActive()) return new Decision(false, "ORGANIZATION_INACTIVE");
    if (!o.getAdminUserId().equals(user)) return new Decision(false, "ORGANIZATION_ACCESS_DENIED");
    if (r.branchId() != null
        && branches
            .findById(r.branchId())
            .filter(b -> r.organizationId().equals(b.getOrganizationId()) && b.isActive())
            .isEmpty()) return new Decision(false, "BRANCH_ACCESS_DENIED");
    var e = entitlements(o.getId(), r.product());
    if (!e.active()) return new Decision(false, "SUBSCRIPTION_INACTIVE");
    String f = TurfFeatures.forPermission(r.permissionCode());
    if (f == null || !e.features().contains(f)) return new Decision(false, "FEATURE_DISABLED");
    var role = roles.findById(o.getAdminRoleId()).orElseThrow();
    if (!Boolean.TRUE.equals(role.getIsActive())
        || role.getScope() != RoleScope.ORGANIZATION
        || role.getPermissions().stream().noneMatch(p -> p.getName().equals(r.permissionCode())))
      return new Decision(false, "PERMISSION_DENIED");
    return new Decision(true, "ALLOWED", user);
  }

  public static String normalizeHost(String host) {
    if (host == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hostname required");
    String h = host.strip().toLowerCase(Locale.ROOT);
    if (h.endsWith(".")) h = h.substring(0, h.length() - 1);
    try {
      h = IDN.toASCII(h);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hostname");
    }
    if (h.length() > 253
        || !h.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?")
        || Arrays.stream(h.split("\\.", -1))
            .anyMatch(x -> !x.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hostname");
    return h;
  }

  public SaasModels.TenantDomain addDomain(UUID id, Domain r) {
    organization(id);
    var d = new TenantDomainDao();
    d.setOrganizationId(id);
    d.setHostname(normalizeHost(r.hostname()));
    d.setProduct(r.product());
    d.setChallenge(UUID.randomUUID().toString() + UUID.randomUUID());
    return SaasMapper.toEntity(domains.saveAndFlush(d));
  }

  public SaasModels.TenantDomain verifyDomain(UUID id, UUID domainId) {
    var d =
        domains
            .findById(domainId)
            .filter(x -> x.getOrganizationId().equals(id))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Domain not found"));
    try {
      var env = new Hashtable<String, String>();
      env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
      env.put("com.sun.jndi.dns.timeout.initial", "2000");
      env.put("com.sun.jndi.dns.timeout.retries", "1");
      var context = new InitialDirContext(env);
      try {
        var txt =
            context
                .getAttributes("_tecgen-verification." + d.getHostname(), new String[] {"TXT"})
                .get("TXT");
        boolean ok = false;
        if (txt != null) {
          var values = txt.getAll();
          while (values.hasMore())
            if (values.next().toString().replace("\"", "").equals(d.getChallenge())) ok = true;
        }
        if (!ok) throw new IllegalStateException();
      } finally {
        context.close();
      }
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DNS challenge not verified");
    }
    d.setVerifiedAt(Instant.now());
    return SaasMapper.toEntity(domains.saveAndFlush(d));
  }

  public record Resolved(UUID organizationId, String product, boolean active) {}

  public Resolved resolve(String host) {
    var ds =
        em.createQuery(
                "select d from TenantDomainDao d where d.hostname=:host and d.verifiedAt is not"
                    + " null",
                TenantDomainDao.class)
            .setParameter("host", normalizeHost(host))
            .getResultList();
    if (ds.isEmpty())
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Verified tenant domain not found");
    var d = ds.getFirst();
    return new Resolved(
        d.getOrganizationId(),
        d.getProduct(),
        entitlements(d.getOrganizationId(), d.getProduct()).active());
  }

  public com.familiemunshi.service.entities.Branch createBranch(UUID id, Branch r) {
    var o = organization(id);
    em.lock(o, LockModeType.PESSIMISTIC_WRITE);
    var e = entitlements(id, "TURF");
    long count =
        em.createQuery("select count(b) from BranchDao b where b.organizationId=:id", Long.class)
            .setParameter("id", id)
            .getSingleResult();
    if (!e.active() || count >= e.limits().getOrDefault("MAX_BRANCHES", 0))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Branch limit reached or subscription inactive");
    var b =
        BranchDao.builder()
            .id(com.familiemunshi.common.utils.UuidV7Generator.generate())
            .organizationId(id)
            .name(r.name())
            .code("BR-" + UUID.randomUUID())
            .address(r.address())
            .phone(r.phone())
            .isActive(true)
            .build();
    return com.familiemunshi.mapper.BranchMapper.toEntity(branches.saveAndFlush(b));
  }

  public Set<String> patchPermissions(UUID id, Set<String> codes) {
    var o = organization(id);
    if (codes.stream().anyMatch(c -> TurfFeatures.forPermission(c) == null))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Turf permissions allowed");
    var role = roles.findById(o.getAdminRoleId()).orElseThrow();
    role.setPermissions(new HashSet<>(permissions.findAllByNameIn(codes)));
    roles.save(role);
    return codes;
  }

  public Object packageAvailability(UUID id, Boolean active, Boolean sale) {
    var p =
        packages
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
    if (active == null && sale == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "At least one availability flag required");
    if (active != null) p.setActive(active);
    if (sale != null) p.setAvailableForSale(sale);
    return com.familiemunshi.mapper.SaasMapper.toEntity(packages.saveAndFlush(p));
  }

  public Object myOrganizations(UUID user) {
    return em
        .createQuery(
            "select o from OrganizationDao o where o.adminUserId=:user and o.active=true",
            OrganizationDao.class)
        .setParameter("user", user)
        .getResultList()
        .stream()
        .map(
            o -> {
              var e = entitlements(o.getId(), "TURF");
              var role = roles.findById(o.getAdminRoleId()).orElseThrow();
              var codes =
                  Boolean.TRUE.equals(role.getIsActive()) && e.active()
                      ? role.getPermissions().stream()
                          .map(PermissionDao::getName)
                          .filter(p -> e.features().contains(TurfFeatures.forPermission(p)))
                          .sorted()
                          .toList()
                      : List.<String>of();
              var bs =
                  em
                      .createQuery(
                          "select b from BranchDao b where b.organizationId=:id and"
                              + " b.isActive=true",
                          BranchDao.class)
                      .setParameter("id", o.getId())
                      .getResultList()
                      .stream()
                      .map(b -> Map.of("id", b.getId(), "name", b.getName()))
                      .toList();
              return Map.of(
                  "organizationId",
                  o.getId(),
                  "name",
                  o.getName(),
                  "role",
                  "ORGANIZATION_ADMIN",
                  "permissions",
                  codes,
                  "entitlements",
                  e,
                  "branches",
                  bs);
            })
        .toList();
  }

  public SaasModels.Organization organizationView(UUID id) {
    return SaasMapper.toEntity(organization(id));
  }

  public Object listOrganizations(int page) {
    return organizations
        .findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50))
        .map(SaasMapper::toEntity);
  }

  public Object listPackages(int page) {
    return packages
        .findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50))
        .map(SaasMapper::toEntity);
  }
}
