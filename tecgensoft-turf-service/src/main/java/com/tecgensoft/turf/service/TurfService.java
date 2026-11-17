package com.tecgensoft.turf.service;

import com.tecgensoft.turf.jpa.daos.*;
import com.tecgensoft.turf.jpa.repositories.*;
import com.tecgensoft.turf.mapper.CourtMapper;
import com.tecgensoft.turf.mapper.TurfMapper;
import com.tecgensoft.turf.service.domain.TurfCommands.*;
import com.tecgensoft.turf.service.domain.TurfModels;
import jakarta.persistence.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class TurfService {
  private final EntityManager em;
  private final VenueRepository venues;
  private final CourtRepository courts;
  private final ReservationRepository reservations;
  private final BookingRepository bookings;
  private final PaymentRepository payments;
  private final ContentRepository contents;
  private final CoreClient core;

  public record Context(UUID organizationId) {}

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
  }

  private void organizationLock(UUID id) {
    em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:id,0))")
        .setParameter("id", id.toString())
        .getSingleResult();
  }

  public VenueDao venue(Context c) {
    return venues.findByOrganizationId(c.organizationId()).stream()
        .findFirst()
        .orElseThrow(this::missing);
  }

  public TurfModels.Venue settings(Context c, Venue r) {
    return saveSettings(c, r, false);
  }

  public TurfModels.Venue createSettings(Context c, Venue r) {
    return saveSettings(c, r, true);
  }

  private TurfModels.Venue saveSettings(Context c, Venue r, boolean create) {
    if (r.openingHour() >= r.closingHour()) throw bad("Overnight opening hours are unsupported");
    try {
      ZoneId.of(r.timezone());
    } catch (java.time.DateTimeException ex) {
      throw bad("Invalid venue timezone");
    }
    organizationLock(c.organizationId());
    var existing =
        venues.findByOrganizationId(c.organizationId()).stream()
            .findFirst();
    if (create && existing.isPresent()) throw conflict("Venue settings already exist");
    if (!create && existing.isEmpty()) throw missing();
    var d = existing.orElseGet(VenueDao::new);
    d.setOrganizationId(c.organizationId());

    d.setName(r.name());
    d.setPhone(r.phone());
    d.setAddress(r.address());
    d.setTimezone(r.timezone());
    d.setOpeningHour(r.openingHour());
    d.setClosingHour(r.closingHour());
    d.setPeakStartHour(r.peakStartHour());
    d.setAdvancePercentage(r.advancePercentage());
    d.setHoldMinutes(r.holdMinutes());
    d.setMaxDurationHours(r.maxDurationHours());
    d.setBookingWindowDays(r.bookingWindowDays());
    d.setCancellationCutoffHours(r.cancellationCutoffHours());
    return TurfMapper.toEntity(venues.saveAndFlush(d));
  }

  private void enforceCourtLimit(Context c) {
    var e = core.entitlements(c.organizationId());
    long n =
        em.createQuery(
                "select count(c) from CourtDao c where c.organizationId=:org and c.active=true",
                Long.class)
            .setParameter("org", c.organizationId())
            .getSingleResult();
    if (n >= e.limits().getOrDefault("MAX_COURTS", 0)) throw conflict("Active court limit reached");
  }

  public Object createCourt(Context c, Court r) {
    organizationLock(c.organizationId());
    venue(c);
    if (r.active()) enforceCourtLimit(c);
    return CourtMapper.toEntity(
        courts.saveAndFlush(CourtMapper.toDao(r, c.organizationId())));
  }

  public CourtDao court(Context c, UUID id) {
    return courts
        .findByIdAndOrganizationId(id, c.organizationId())
        .orElseThrow(this::missing);
  }

  private CourtDao lockedCourt(Context c, UUID id) {
    var court = court(c, id);
    em.lock(court, LockModeType.PESSIMISTIC_WRITE);
    expireCourt(id, Instant.now());
    return court;
  }

  public Object updateCourt(Context c, UUID id, Court r) {
    organizationLock(c.organizationId());
    var d = lockedCourt(c, id);
    if (!d.isActive() && r.active()) enforceCourtLimit(c);
    CourtMapper.apply(d, r);
    return CourtMapper.toEntity(courts.saveAndFlush(d));
  }

  public void deleteCourt(Context c, UUID id) {
    organizationLock(c.organizationId());
    var d = lockedCourt(c, id);
    d.setActive(false);
    courts.save(d);
  }

  public Object courts(Context c, boolean publicOnly) {
    return courts.findByOrganizationId(c.organizationId()).stream()
        .filter(d -> !publicOnly || d.isActive())
        .map(CourtMapper::toEntity)
        .toList();
  }

  public record Quote(
      Instant startAt, Instant endAt, BigDecimal total, BigDecimal requiredAdvance) {}

  public static BigDecimal price(
      BigDecimal regular, BigDecimal peak, int start, int duration, int peakStart) {
    BigDecimal total = BigDecimal.ZERO;
    for (int h = start; h < start + duration; h++)
      total = total.add(h >= peakStart ? peak : regular);
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private Quote quote(VenueDao v, CourtDao court, Slot slot) {
    return quote(v, court, slot, false);
  }

  private Quote quote(VenueDao v, CourtDao court, Slot slot, boolean maintenance) {
    if (!court.isActive()) throw conflict("Court inactive");
    var zone = ZoneId.of(v.getTimezone());
    var today = LocalDate.now(zone);
    if (slot.date().isBefore(today)
        || slot.date().isAfter(today.plusDays(v.getBookingWindowDays()))
        || (!maintenance && slot.durationHours() > v.getMaxDurationHours())
        || slot.startHour() < v.getOpeningHour()
        || slot.startHour() + slot.durationHours() > v.getClosingHour())
      throw bad("Slot outside booking hours or window");
    var local = slot.date().atStartOfDay().plusHours(slot.startHour());
    var endLocal = local.plusHours(slot.durationHours());
    for (int h = 0; h <= slot.durationHours(); h++)
      if (zone.getRules().getValidOffsets(local.plusHours(h)).size() != 1)
        throw bad("Ambiguous or nonexistent timezone-local slot");
    Instant start = local.atZone(zone).toInstant(), end = endLocal.atZone(zone).toInstant();
    if (!start.isAfter(Instant.now())) throw bad("Slot must be in the future");
    BigDecimal total =
        price(
            court.getRegularPrice(),
            court.getPeakPrice(),
            slot.startHour(),
            slot.durationHours(),
            v.getPeakStartHour());
    return new Quote(
        start,
        end,
        total,
        total
            .multiply(BigDecimal.valueOf(v.getAdvancePercentage()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING));
  }

  private boolean occupied(UUID court, Quote q) {
    return em.createQuery(
                "select count(r) from ReservationDao r where r.courtId=:court and r.status='ACTIVE'"
                    + " and (r.expiresAt is null or r.expiresAt>:now) and r.startAt<:end and"
                    + " r.endAt>:start",
                Long.class)
            .setParameter("court", court)
            .setParameter("now", Instant.now())
            .setParameter("start", q.startAt())
            .setParameter("end", q.endAt())
            .getSingleResult()
        > 0;
  }

  public Object availability(Context c, UUID courtId, LocalDate date, int duration) {
    var d = lockedCourt(c, courtId);
    var v = venue(c);
    List<Map<String, Object>> result = new ArrayList<>();
    for (int h = v.getOpeningHour(); h + duration <= v.getClosingHour(); h++) {
      try {
        var q = quote(v, d, new Slot(courtId, date, h, duration));
        result.add(
            Map.of(
                "startHour",
                h,
                "durationHours",
                duration,
                "available",
                !occupied(courtId, q),
                "quote",
                q));
      } catch (ResponseStatusException ignored) {
      }
    }
    return result;
  }

  private ReservationDao reserve(
      Context c, UUID court, Quote q, String kind, Instant expiry, String hash, String reason) {
    if (occupied(court, q)) throw conflict("Slot is unavailable");
    var d = new ReservationDao();
    d.setOrganizationId(c.organizationId());

    d.setCourtId(court);
    d.setStartAt(q.startAt());
    d.setEndAt(q.endAt());
    d.setKind(kind);
    d.setStatus("ACTIVE");
    d.setExpiresAt(expiry);
    d.setSecretHash(hash);
    d.setReason(reason);
    return reservations.saveAndFlush(d);
  }

  private static String hash(String secret) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void verifySecret(ReservationDao r, String secret) {
    if (secret == null
        || r.getSecretHash() == null
        || !MessageDigest.isEqual(
            hash(secret).getBytes(StandardCharsets.UTF_8),
            r.getSecretHash().getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid checkout secret");
  }

  public record Held(UUID holdId, String secret, Instant expiresAt, Quote quote) {}

  public Held hold(Context c, Slot slot) {
    if (!core.entitlements(c.organizationId()).features().contains("ONLINE_BOOKING"))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Online booking disabled");
    var court = lockedCourt(c, slot.courtId());
    var v = venue(c);
    var q = quote(v, court, slot);
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiry = Instant.now().plusSeconds(v.getHoldMinutes() * 60L);
    if (expiry.isAfter(q.startAt())) expiry = q.startAt();
    var r = reserve(c, court.getId(), q, "HOLD", expiry, hash(secret), null);
    return new Held(r.getId(), secret, expiry, q);
  }

  private ReservationDao reservation(Context c, UUID id) {
    return reservations
        .findByIdAndOrganizationId(id, c.organizationId())
        .orElseThrow(this::missing);
  }

  public void release(Context c, UUID id, String secret) {
    var r = reservation(c, id);
    lockedCourt(c, r.getCourtId());
    em.refresh(r);
    verifySecret(r, secret);
    if (!"HOLD".equals(r.getKind())) throw conflict("Hold already consumed");
    r.setStatus("RELEASED");
    reservations.save(r);
  }

  public record Receipt(
      UUID bookingId,
      String status,
      BigDecimal total,
      BigDecimal requiredAdvance,
      Instant paymentExpiresAt) {}

  public Receipt checkout(Context c, Checkout request) {
    var r = reservation(c, request.holdId());
    lockedCourt(c, r.getCourtId());
    em.refresh(r);
    verifySecret(r, request.secret());
    if (!"HOLD".equals(r.getKind())
        || !"ACTIVE".equals(r.getStatus())
        || !r.getExpiresAt().isAfter(Instant.now())) throw conflict("Hold expired or consumed");
    var v = venue(c);
    var local = r.getStartAt().atZone(ZoneId.of(v.getTimezone()));
    var slot =
        new Slot(
            r.getCourtId(),
            local.toLocalDate(),
            local.getHour(),
            (int) Duration.between(r.getStartAt(), r.getEndAt()).toHours());
    var q = quote(v, court(c, r.getCourtId()), slot);
    if (!core.entitlements(c.organizationId()).features().contains("ONLINE_BOOKING"))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Online booking disabled");
    var b =
        booking(
            c,
            r,
            slot,
            q,
            request.customerName(),
            request.customerPhone(),
            request.customerEmail(),
            "ONLINE",
            request.notes());
    b.setStatus(q.requiredAdvance().signum() == 0 ? "CONFIRMED" : "PENDING_PAYMENT");
    r.setKind("BOOKING");
    if ("CONFIRMED".equals(b.getStatus())) r.setExpiresAt(null);
    reservations.save(r);
    bookings.saveAndFlush(b);
    return new Receipt(
        b.getId(), b.getStatus(), b.getTotal(), b.getRequiredAdvance(), r.getExpiresAt());
  }

  private BookingDao booking(
      Context c,
      ReservationDao r,
      Slot slot,
      Quote q,
      String name,
      String phone,
      String email,
      String source,
      String notes) {
    var b = new BookingDao();
    b.setOrganizationId(c.organizationId());
    b.setCourtId(r.getCourtId());
    b.setReservationId(r.getId());
    b.setLocalDate(slot.date());
    b.setStartHour(slot.startHour());
    b.setDurationHours(slot.durationHours());
    b.setCustomerName(name);
    b.setCustomerPhone(phone);
    b.setCustomerEmail(email);
    b.setTotal(q.total());
    b.setRequiredAdvance(q.requiredAdvance());
    b.setSource(source);
    b.setNotes(notes);
    return b;
  }

  public TurfModels.Booking adminBooking(Context c, AdminBooking r) {
    if (r.source() == null) throw bad("Booking source required");
    var court = lockedCourt(c, r.slot().courtId());
    var q = quote(venue(c), court, r.slot());
    var reservation = reserve(c, court.getId(), q, "BOOKING", null, null, null);
    var b =
        booking(
            c,
            reservation,
            r.slot(),
            q,
            r.customerName(),
            r.customerPhone(),
            r.customerEmail(),
            r.source(),
            r.notes());
    b.setStatus("CONFIRMED");
    return TurfMapper.toEntity(bookings.saveAndFlush(b));
  }

  public BookingDao booking(Context c, UUID id) {
    return bookings
        .findByIdAndOrganizationId(id, c.organizationId())
        .orElseThrow(this::missing);
  }

  private BookingDao lockedBooking(Context c, UUID id) {
    var b = booking(c, id);
    lockedCourt(c, b.getCourtId());
    em.refresh(b);
    em.lock(b, LockModeType.PESSIMISTIC_WRITE);
    return b;
  }

  public Object bookings(Context c, LocalDate from, LocalDate to, int page) {
    expireOrganization(c);
    return em
        .createQuery(
            "select b from BookingDao b where b.organizationId=:org and"
                + " b.localDate>=:from and b.localDate<=:to order by b.localDate desc,b.id",
            BookingDao.class)
        .setParameter("org", c.organizationId())
        .setParameter("from", from)
        .setParameter("to", to)
        .setFirstResult(Math.max(0, page) * 50)
        .setMaxResults(50)
        .getResultList()
        .stream()
        .map(TurfMapper::toEntity)
        .toList();
  }

  public Object detail(Context c, UUID id) {
    var b = lockedBooking(c, id);
    return Map.of("booking", TurfMapper.toEntity(b));
  }

  public Object ledger(Context c, UUID id) {
    booking(c, id);
    return payments.findByOrganizationId(c.organizationId()).stream()
        .filter(p -> p.getBookingId().equals(id))
        .map(TurfMapper::toEntity)
        .toList();
  }

  public void submitReference(Context c, UUID id, Reference r) {
    var b = lockedBooking(c, id);
    verifySecret(reservation(c, b.getReservationId()), r.secret());
    if (!"PENDING_PAYMENT".equals(b.getStatus())) throw conflict("Booking not awaiting payment");
    b.setSubmittedReference(r.reference());
    bookings.save(b);
  }

  public TurfModels.Booking cancel(Context c, UUID id, String reason, UUID actor) {
    var b = lockedBooking(c, id);
    var r = reservation(c, b.getReservationId());
    if ("CANCELLED".equals(b.getStatus())) return TurfMapper.toEntity(b);
    if (!Set.of("CONFIRMED", "PENDING_PAYMENT").contains(b.getStatus()))
      throw conflict("Booking cannot be cancelled");
    if (!Instant.now()
        .isBefore(r.getStartAt().minusSeconds(venue(c).getCancellationCutoffHours() * 3600L)))
      throw conflict("Cancellation cutoff passed");
    b.setStatus("CANCELLED");
    b.setCancellationReason(reason);
    b.setCancelledAt(Instant.now());
    b.setCancelledBy(actor);
    r.setStatus("RELEASED");
    reservations.save(r);
    return TurfMapper.toEntity(bookings.saveAndFlush(b));
  }

  public TurfModels.Payment payment(Context c, UUID id, Payment request, UUID actor) {
    var b = lockedBooking(c, id);
    String reference = request.reference() == null ? null : request.reference().strip();
    if (reference != null && reference.isEmpty()) reference = null;
    var previous =
        em.createQuery(
                "select p from PaymentDao p where p.organizationId=:org and p.idempotencyKey=:key",
                PaymentDao.class)
            .setParameter("org", c.organizationId())
            .setParameter("key", request.idempotencyKey())
            .getResultList();
    if (!previous.isEmpty()) {
      var p = previous.getFirst();
      if (p.getBookingId().equals(id)
          && p.getAmount().compareTo(request.amount()) == 0
          && p.getMethod().equals(request.method())
          && p.getKind().equals(request.kind())
          && Objects.equals(p.getReference(), reference)
          && request.verified()) return TurfMapper.toEntity(p);
      throw conflict("Idempotency key reused with different payment");
    }
    if (!request.verified())
      throw bad("Payment must be independently verified by the recording admin");
    if (!"CASH".equals(request.method())
        && (request.reference() == null || request.reference().isBlank()))
      throw bad("Verified transaction reference required");
    if (request.amount().scale() > 2) throw bad("Amount supports two decimal places");
    if (!"REFUND".equals(request.kind())
        && !Set.of("CONFIRMED", "PENDING_PAYMENT").contains(b.getStatus()))
      throw conflict("Booking does not accept payments");
    List<PaymentDao> ledger =
        em.createQuery("select p from PaymentDao p where p.bookingId=:id", PaymentDao.class)
            .setParameter("id", id)
            .getResultList();
    BigDecimal paid =
        ledger.stream()
            .filter(p -> !"REFUND".equals(p.getKind()))
            .map(PaymentDao::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunded =
        ledger.stream()
            .filter(p -> "REFUND".equals(p.getKind()))
            .map(PaymentDao::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if ("REFUND".equals(request.kind())) {
      if (request.amount().compareTo(paid.subtract(refunded)) > 0)
        throw conflict("Refund exceeds unrefunded payments");
    } else if (paid.add(request.amount()).compareTo(b.getTotal()) > 0)
      throw conflict("Payment exceeds booking total");
    if ("PENDING_PAYMENT".equals(b.getStatus()) && !"ADVANCE".equals(request.kind()))
      throw bad("Pending booking requires advance payment");
    var p = new PaymentDao();
    p.setOrganizationId(c.organizationId());
    p.setBookingId(id);
    p.setAmount(request.amount());
    p.setMethod(request.method());
    p.setKind(request.kind());
    p.setReference(reference);
    p.setIdempotencyKey(request.idempotencyKey());
    p.setRecordedBy(actor);
    payments.saveAndFlush(p);
    if ("PENDING_PAYMENT".equals(b.getStatus())
        && paid.add(request.amount()).compareTo(b.getRequiredAdvance()) >= 0) {
      var r = reservation(c, b.getReservationId());
      if (r.getExpiresAt() == null || !r.getExpiresAt().isAfter(Instant.now()))
        throw conflict("Payment deadline passed");
      b.setStatus("CONFIRMED");
      r.setExpiresAt(null);
      reservations.save(r);
      bookings.save(b);
    }
    return TurfMapper.toEntity(p);
  }

  public TurfModels.Reservation block(Context c, Block request) {
    var court = lockedCourt(c, request.slot().courtId());
    var q = quote(venue(c), court, request.slot(), true);
    return TurfMapper.toEntity(reserve(c, court.getId(), q, "BLOCK", null, null, request.reason()));
  }

  public void releaseBlock(Context c, UUID id) {
    var r = reservation(c, id);
    lockedCourt(c, r.getCourtId());
    em.refresh(r);
    if (!"BLOCK".equals(r.getKind())) throw missing();
    r.setStatus("RELEASED");
    reservations.save(r);
  }

  public Object blocks(Context c) {
    return reservations.findByOrganizationId(c.organizationId()).stream()
        .filter(r -> "BLOCK".equals(r.getKind()))
        .map(
            r ->
                Map.of(
                    "id",
                    r.getId(),
                    "courtId",
                    r.getCourtId(),
                    "startAt",
                    r.getStartAt(),
                    "endAt",
                    r.getEndAt(),
                    "status",
                    r.getStatus(),
                    "reason",
                    r.getReason()))
        .toList();
  }

  public TurfModels.Content content(Context c, UUID id, Content r) {
    venue(c);
    var d =
        id == null
            ? new ContentDao()
            : contents
                .findByIdAndOrganizationId(id, c.organizationId())
                .orElseThrow(this::missing);
    d.setOrganizationId(c.organizationId());

    d.setKind(r.kind());
    d.setTitle(r.title());
    d.setBody(r.body());
    d.setImageUrl(r.imageUrl());
    d.setSortOrder(r.sortOrder());
    d.setActive(r.active());
    return TurfMapper.toEntity(contents.saveAndFlush(d));
  }

  public Object contents(Context c, boolean publicOnly) {
    return contents.findByOrganizationId(c.organizationId()).stream()
        .filter(x -> !publicOnly || x.isActive())
        .sorted(Comparator.comparingInt(ContentDao::getSortOrder))
        .map(TurfMapper::toEntity)
        .toList();
  }

  public String contentKind(Context c, UUID id) {
    return contents
        .findByIdAndOrganizationId(id, c.organizationId())
        .orElseThrow(this::missing)
        .getKind();
  }

  public void deleteContent(Context c, UUID id) {
    var d =
        contents
            .findByIdAndOrganizationId(id, c.organizationId())
            .orElseThrow(this::missing);
    contents.delete(d);
  }

  private void expireCourt(UUID court, Instant now) {
    em.createQuery(
            "update ReservationDao r set r.status='EXPIRED',r.updatedAt=:now where r.courtId=:court"
                + " and r.status='ACTIVE' and r.expiresAt<=:now")
        .setParameter("court", court)
        .setParameter("now", now)
        .executeUpdate();
    em.createQuery(
            "update BookingDao b set b.status='EXPIRED',b.updatedAt=:now where b.courtId=:court and"
                + " b.status='PENDING_PAYMENT' and b.reservationId in (select r.id from"
                + " ReservationDao r where r.status='EXPIRED')")
        .setParameter("court", court)
        .setParameter("now", now)
        .executeUpdate();
  }

  private void expireOrganization(Context c) {
    courts.findByOrganizationId(c.organizationId()).stream()
        .sorted(Comparator.comparing(CourtDao::getId))
        .forEach(d -> lockedCourt(c, d.getId()));
  }

  @Scheduled(fixedDelayString = "${turf.expiry-interval-ms:30000}")
  public void expire() {
    var ids =
        em.createQuery(
                "select distinct r.courtId from ReservationDao r where r.status='ACTIVE' and"
                    + " r.expiresAt<=:now",
                UUID.class)
            .setParameter("now", Instant.now())
            .getResultList();
    ids.stream()
        .sorted()
        .forEach(
            id -> {
              var court = em.find(CourtDao.class, id, LockModeType.PESSIMISTIC_WRITE);
              if (court != null) expireCourt(id, Instant.now());
            });
  }

  public Object report(Context c, LocalDate from, LocalDate to) {
    if (to.isBefore(from)) throw bad("Report end date must not precede start date");
    expireOrganization(c);
    var bs =
        em.createQuery(
                "select b from BookingDao b where b.organizationId=:org and"
                    + " b.localDate>=:from and b.localDate<=:to",
                BookingDao.class)
            .setParameter("org", c.organizationId())
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
    Set<UUID> ids = new HashSet<>(bs.stream().map(BookingDao::getId).toList());
    var ledger =
        payments.findByOrganizationId(c.organizationId()).stream()
            .filter(p -> ids.contains(p.getBookingId()))
            .toList();
    BigDecimal collected =
        ledger.stream()
            .filter(p -> !"REFUND".equals(p.getKind()))
            .map(PaymentDao::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunds =
        ledger.stream()
            .filter(p -> "REFUND".equals(p.getKind()))
            .map(PaymentDao::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal total =
        bs.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .map(BookingDao::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var v = venue(c);
    long capacity = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
    capacity *= v.getClosingHour() - v.getOpeningHour();
    final long availableHours = capacity;
    var utilization =
        courts.findByOrganizationId(c.organizationId()).stream()
            .map(
                court -> {
                  long hours =
                      bs.stream()
                          .filter(
                              b ->
                                  court.getId().equals(b.getCourtId())
                                      && "CONFIRMED".equals(b.getStatus()))
                          .mapToLong(BookingDao::getDurationHours)
                          .sum();
                  return Map.of(
                      "courtId",
                      court.getId(),
                      "courtName",
                      court.getName(),
                      "bookedHours",
                      hours,
                      "availableHours",
                      availableHours,
                      "utilizationPercentage",
                      BigDecimal.valueOf(hours)
                          .multiply(BigDecimal.valueOf(100))
                          .divide(BigDecimal.valueOf(availableHours), 2, RoundingMode.HALF_UP));
                })
            .toList();
    return Map.of(
        "courtUtilization",
        utilization,
        "bookingCount",
        bs.size(),
        "confirmedHours",
        bs.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .mapToInt(BookingDao::getDurationHours)
            .sum(),
        "collections",
        collected,
        "refunds",
        refunds,
        "confirmedBookingValue",
        total,
        "outstanding",
        bs.stream()
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .map(
                b ->
                    b.getTotal()
                        .subtract(
                            ledger.stream()
                                .filter(
                                    p ->
                                        p.getBookingId().equals(b.getId())
                                            && !"REFUND".equals(p.getKind()))
                                .map(PaymentDao::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }
}
