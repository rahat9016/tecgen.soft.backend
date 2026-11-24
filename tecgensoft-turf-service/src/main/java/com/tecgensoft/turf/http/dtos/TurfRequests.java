package com.tecgensoft.turf.http.dtos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class TurfRequests {
  public record Venue(
      @NotBlank String name,
      @NotBlank String phone,
      @NotBlank String address,
      @NotBlank String timezone,
      @Min(0) @Max(23) int openingHour,
      @Min(1) @Max(24) int closingHour,
      @Min(0) @Max(24) int peakStartHour,
      @Min(0) @Max(100) int advancePercentage,
      @Min(1) @Max(60) int holdMinutes,
      @Min(1) @Max(24) int maxDurationHours,
      @Min(1) @Max(365) int bookingWindowDays,
      @Min(0) @Max(720) int cancellationCutoffHours) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Venue toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Venue(
          name,
          phone,
          address,
          timezone,
          openingHour,
          closingHour,
          peakStartHour,
          advancePercentage,
          holdMinutes,
          maxDurationHours,
          bookingWindowDays,
          cancellationCutoffHours);
    }
  }

  public record Court(
      @NotBlank String name,
      @NotBlank String sport,
      @NotBlank String surface,
      boolean indoor,
      String size,
      String imageUrl,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal regularPrice,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal peakPrice,
      @Min(1) int players,
      @NotNull Set<String> features,
      String description,
      boolean active) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Court toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Court(
          name,
          sport,
          surface,
          indoor,
          size,
          imageUrl,
          regularPrice,
          peakPrice,
          players,
          features,
          description,
          active);
    }
  }

  public record Slot(
      @NotNull UUID courtId,
      @NotNull LocalDate date,
      @Min(0) @Max(23) int startHour,
      @Min(1) @Max(24) int durationHours) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Slot toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Slot(
          courtId, date, startHour, durationHours);
    }
  }

  public record Hold(@NotNull @jakarta.validation.Valid Slot slot) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Hold toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Hold(slot.toEntity());
    }
  }

  public record Checkout(
      @NotNull UUID holdId,
      @NotBlank @Size(max = 100) String secret,
      @NotBlank @Size(max = 120) String customerName,
      @NotBlank @Size(max = 30) String customerPhone,
      @Email @Size(max = 254) String customerEmail,
      @Size(max = 1000) String notes) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Checkout toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Checkout(
          holdId, secret, customerName, customerPhone, customerEmail, notes);
    }
  }

  public record AdminBooking(
      @NotNull @jakarta.validation.Valid Slot slot,
      @NotBlank String customerName,
      @NotBlank String customerPhone,
      @Email String customerEmail,
      @Pattern(regexp = "PHONE|WALK_IN") String source,
      String notes) {
    public com.tecgensoft.turf.service.domain.TurfCommands.AdminBooking toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.AdminBooking(
          slot.toEntity(), customerName, customerPhone, customerEmail, source, notes);
    }
  }

  public record DirectBooking(
      @NotNull @jakarta.validation.Valid Slot slot,
      @NotBlank @Size(max = 30) String customerPhone,
      @Size(max = 120) String customerName,
      @Size(max = 1000) String notes) {
    public com.tecgensoft.turf.service.domain.TurfCommands.DirectBooking toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.DirectBooking(
          slot.toEntity(), customerPhone, customerName, notes);
    }
  }

  public record Secret(@NotBlank String secret) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Secret toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Secret(secret);
    }
  }

  public record Reference(@NotBlank @Size(max = 100) String secret, @NotBlank @Size(max = 255) String reference) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Reference toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Reference(secret, reference);
    }
  }

  public record Cancel(@NotBlank String reason) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Cancel toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Cancel(reason);
    }
  }

  public record Payment(
      @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal amount,
      @NotBlank @Pattern(regexp = "BKASH|NAGAD|CARD|CASH") String method,
      @NotBlank @Pattern(regexp = "ADVANCE|BALANCE|REFUND") String kind,
      String reference,
      @NotBlank @Size(max = 150) String idempotencyKey,
      boolean verified) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Payment toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Payment(
          amount, method, kind, reference, idempotencyKey, verified);
    }
  }

  public record Block(@NotNull @jakarta.validation.Valid Slot slot, @NotBlank String reason) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Block toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Block(slot.toEntity(), reason);
    }
  }

  public record Content(
      @NotBlank @Pattern(regexp = "HERO|ABOUT|GALLERY|SERVICE") String kind,
      @NotBlank String title,
      String body,
      String imageUrl,
      int sortOrder,
      boolean active) {
    public com.tecgensoft.turf.service.domain.TurfCommands.Content toEntity() {
      return new com.tecgensoft.turf.service.domain.TurfCommands.Content(
          kind, title, body, imageUrl, sortOrder, active);
    }
  }
}
