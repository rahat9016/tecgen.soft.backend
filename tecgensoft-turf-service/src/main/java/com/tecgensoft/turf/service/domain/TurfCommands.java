package com.tecgensoft.turf.service.domain;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class TurfCommands {
  public record Venue(
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

  public record Court(
      String name,
      String sport,
      String surface,
      boolean indoor,
      String size,
      String imageUrl,
      BigDecimal regularPrice,
      BigDecimal peakPrice,
      int players,
      Set<String> features,
      String description,
      boolean active) {}

  public record Slot(UUID courtId, LocalDate date, int startHour, int durationHours) {}

  public record Hold(Slot slot) {}

  public record Checkout(
      UUID holdId,
      String secret,
      String customerName,
      String customerPhone,
      String customerEmail,
      String notes) {}

  public record AdminBooking(
      Slot slot,
      String customerName,
      String customerPhone,
      String customerEmail,
      String source,
      String notes) {}

  public record Secret(String secret) {}

  public record Reference(String secret, String reference) {}

  public record Cancel(String reason) {}

  public record Payment(
      BigDecimal amount,
      String method,
      String kind,
      String reference,
      String idempotencyKey,
      boolean verified) {}

  public record Block(Slot slot, String reason) {}

  public record Content(
      String kind, String title, String body, String imageUrl, int sortOrder, boolean active) {}
}
