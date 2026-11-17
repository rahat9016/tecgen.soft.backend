package com.tecgensoft.turf;

import static org.junit.jupiter.api.Assertions.*;

import com.tecgensoft.turf.jpa.repositories.*;
import com.tecgensoft.turf.service.*;
import com.tecgensoft.turf.service.domain.TurfCommands.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_TURF_DATABASE_URL}",
      "spring.datasource.username=${TEST_DATABASE_USERNAME}",
      "spring.datasource.password=${TEST_DATABASE_PASSWORD:}",
      "core.service-key=test-service-key-at-least-thirty-two-characters",
      "core.url=http://localhost:1"
    })
@EnabledIfEnvironmentVariable(named = "TEST_TURF_DATABASE_URL", matches = ".+")
class TurfIntegrationTest {
  @TestConfiguration
  static class Config {
    @Bean
    @Primary
    CoreClient testCore() {
      return new CoreClient("http://localhost:1", "test-only") {
        @Override
        public Entitlements entitlements(UUID id) {
          return new Entitlements(
              id,
              UUID.randomUUID(),
              "TEST",
              1,
              "ACTIVE",
              true,
              Set.of("ONLINE_BOOKING", "COURT_MANAGEMENT"),
              Map.of("MAX_COURTS", 1));
        }
      };
    }
  }

  @Autowired TurfService service;
  @Autowired CourtRepository courts;
  @Autowired BookingRepository bookings;
  @Autowired JdbcTemplate jdbc;
  TurfService.Context context;
  UUID court;
  UUID actor = UUID.randomUUID();

  @BeforeEach
  void setup() {
    context = new TurfService.Context(UUID.randomUUID());
    service.createSettings(
        context, new Venue("Venue", "0123", "Address", "Asia/Dhaka", 0, 24, 18, 50, 10, 4, 30, 0));
    service.createCourt(context, courtRequest("First", true));
    court =
        courts
            .findByOrganizationId(context.organizationId())
            .getFirst()
            .getId();
  }

  Court courtRequest(String name, boolean active) {
    return new Court(
        name,
        "FOOTBALL",
        "GRASS",
        false,
        "100x50",
        null,
        new BigDecimal("100"),
        new BigDecimal("200"),
        10,
        Set.of("LIGHTS"),
        "Test",
        active);
  }

  Slot slot(int hour) {
    return new Slot(court, LocalDate.now(ZoneId.of("Asia/Dhaka")).plusDays(1), hour, 1);
  }

  AdminBooking admin(int hour) {
    return new AdminBooking(slot(hour), "Customer", "0123", null, "PHONE", null);
  }

  @Test
  void holdSecretConsumptionExpiryAndPricing() {
    var held = service.hold(context, slot(18));
    assertEquals(new BigDecimal("200.00"), held.quote().total());
    assertThrows(Exception.class, () -> service.release(context, held.holdId(), "wrong"));
    assertThrows(Exception.class, () -> service.adminBooking(context, admin(18)));
    var receipt =
        service.checkout(
            context, new Checkout(held.holdId(), held.secret(), "Customer", "0123", null, null));
    assertEquals("PENDING_PAYMENT", receipt.status());
    assertThrows(
        Exception.class,
        () ->
            service.checkout(
                context,
                new Checkout(held.holdId(), held.secret(), "Customer", "0123", null, null)));
    jdbc.update(
        "update reservations set expires_at=now()-interval '1 second' where id=?", held.holdId());
    service.availability(context, court, slot(18).date(), 1);
    assertEquals("EXPIRED", bookings.findById(receipt.bookingId()).orElseThrow().getStatus());
    assertDoesNotThrow(() -> service.adminBooking(context, admin(18)));
  }

  @Test
  void expiredHoldStopsBlocking() {
    var held = service.hold(context, slot(10));
    jdbc.update(
        "update reservations set expires_at=now()-interval '1 second' where id=?", held.holdId());
    assertDoesNotThrow(() -> service.adminBooking(context, admin(10)));
    assertThrows(
        Exception.class,
        () ->
            service.checkout(
                context, new Checkout(held.holdId(), held.secret(), "A", "0123", null, null)));
  }

  @Test
  void ledgerIdempotencyAndBounds() {
    var b = service.adminBooking(context, admin(11));
    var p =
        new Payment(
            new BigDecimal("80"),
            "BKASH",
            "ADVANCE",
            "TX-" + UUID.randomUUID(),
            "KEY-" + UUID.randomUUID(),
            true);
    var saved = service.payment(context, b.id(), p, actor);
    assertEquals(saved.id(), service.payment(context, b.id(), p, actor).id());
    assertThrows(
        Exception.class,
        () ->
            service.payment(
                context,
                b.id(),
                new Payment(
                    new BigDecimal("81"),
                    p.method(),
                    p.kind(),
                    p.reference(),
                    p.idempotencyKey(),
                    true),
                actor));
    assertThrows(
        Exception.class,
        () ->
            service.payment(
                context,
                b.id(),
                new Payment(
                    new BigDecimal("21"),
                    "CASH",
                    "BALANCE",
                    null,
                    UUID.randomUUID().toString(),
                    true),
                actor));
    service.payment(
        context,
        b.id(),
        new Payment(
            new BigDecimal("50"), "CASH", "REFUND", null, UUID.randomUUID().toString(), true),
        actor);
    assertThrows(
        Exception.class,
        () ->
            service.payment(
                context,
                b.id(),
                new Payment(
                    new BigDecimal("31"),
                    "CASH",
                    "REFUND",
                    null,
                    UUID.randomUUID().toString(),
                    true),
                actor));
  }

  @Test
  void advanceConfirmsPendingBeforeExpiry() {
    var h = service.hold(context, slot(19));
    var b =
        service.checkout(context, new Checkout(h.holdId(), h.secret(), "A", "0123", null, null));
    service.payment(
        context,
        b.bookingId(),
        new Payment(
            new BigDecimal("100"), "CASH", "ADVANCE", null, UUID.randomUUID().toString(), true),
        actor);
    assertEquals("CONFIRMED", bookings.findById(b.bookingId()).orElseThrow().getStatus());
  }

  @Test
  void crossTenantReadAndMutationDenied() {
    var b = service.adminBooking(context, admin(12));
    var other = new TurfService.Context(UUID.randomUUID());
    assertThrows(Exception.class, () -> service.detail(other, b.id()));
    assertThrows(Exception.class, () -> service.cancel(other, b.id(), "bad", actor));
    assertThrows(
        Exception.class,
        () ->
            service.payment(
                other,
                b.id(),
                new Payment(BigDecimal.ONE, "CASH", "ADVANCE", null, "bad", true),
                actor));
  }

  @Test
  void concurrentBookingHoldAndMaintenanceHaveOneWinner() throws Exception {
    assertEquals(
        1,
        race(
            List.of(
                () -> service.adminBooking(context, admin(13)),
                () -> service.hold(context, slot(13)),
                () -> service.block(context, new Block(slot(13), "Maintenance")))));
  }

  @Test
  void concurrentCourtLimitHasOneWinner() throws Exception {
    service.deleteCourt(context, court);
    assertEquals(
        1,
        race(
            List.of(
                () -> service.createCourt(context, courtRequest("A", true)),
                () -> service.createCourt(context, courtRequest("B", true)))));
  }

  int race(List<Callable<Object>> operations) throws Exception {
    try (var executor = Executors.newFixedThreadPool(operations.size())) {
      var start = new CountDownLatch(1);
      List<Future<Boolean>> futures = new ArrayList<>();
      for (var operation : operations)
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  try {
                    operation.call();
                    return true;
                  } catch (Exception e) {
                    return false;
                  }
                }));
      start.countDown();
      int succeeded = 0;
      for (var future : futures) if (future.get(20, TimeUnit.SECONDS)) succeeded++;
      return succeeded;
    }
  }

  @Test
  void cancellationReleasesOccupancy() {
    var b = service.adminBooking(context, admin(14));
    assertEquals("CANCELLED", service.cancel(context, b.id(), "Customer request", actor).status());
    assertDoesNotThrow(() -> service.adminBooking(context, admin(14)));
  }

  @Test
  void transactionReferenceCannotFundTwoBookings() {
    var first = service.adminBooking(context, admin(15));
    var second = service.adminBooking(context, admin(16));
    String reference = "UNIQUE-" + UUID.randomUUID();
    service.payment(
        context,
        first.id(),
        new Payment(
            BigDecimal.TEN, "BKASH", "ADVANCE", reference, UUID.randomUUID().toString(), true),
        actor);
    assertThrows(
        Exception.class,
        () ->
            service.payment(
                context,
                second.id(),
                new Payment(
                    BigDecimal.TEN,
                    "BKASH",
                    "ADVANCE",
                    " " + reference + " ",
                    UUID.randomUUID().toString(),
                    true),
                actor));
  }

  @Test
  void timezoneGapRejected() {
    service.settings(
        context,
        new Venue("Venue", "0123", "Address", "Europe/Berlin", 0, 24, 18, 50, 10, 4, 365, 0));
    var today = LocalDate.now();
    int year = today.getYear();
    LocalDate gap =
        LocalDate.of(year, 3, 31)
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
    if (!gap.isAfter(today))
      gap =
          LocalDate.of(year + 1, 3, 31)
              .with(
                  java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
    final LocalDate date = gap;
    assertThrows(Exception.class, () -> service.hold(context, new Slot(court, date, 2, 1)));
  }
}
