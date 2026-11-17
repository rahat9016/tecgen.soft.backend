package com.tecgensoft.turf.service.domain;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class TurfModels {
  public record Venue(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,

      String name,
      String phone,
      String address,
      String timezone,
      int openingHour,
      int closingHour,
      int peakStartHour,
      int advancePercentage,
      int holdMinutes,
      int maxDurationHours,
      int bookingWindowDays,
      int cancellationCutoffHours) {}

  public record Reservation(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,

      UUID courtId,
      Instant startAt,
      Instant endAt,
      String kind,
      String status,
      Instant expiresAt,
      String reason) {}

  public record Booking(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,

      UUID courtId,
      UUID reservationId,
      java.time.LocalDate localDate,
      int startHour,
      int durationHours,
      String customerName,
      String customerPhone,
      String customerEmail,
      BigDecimal total,
      BigDecimal requiredAdvance,
      String status,
      String source,
      String notes,
      String cancellationReason,
      Instant cancelledAt,
      UUID cancelledBy,
      String submittedReference) {}

  public record Payment(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,

      UUID bookingId,
      BigDecimal amount,
      String method,
      String kind,
      String reference,
      String idempotencyKey,
      UUID recordedBy) {}

  public record Content(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,

      String kind,
      String title,
      String body,
      String imageUrl,
      int sortOrder,
      boolean active) {}
}
