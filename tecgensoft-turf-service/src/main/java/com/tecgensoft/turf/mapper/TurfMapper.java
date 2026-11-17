package com.tecgensoft.turf.mapper;

import com.tecgensoft.turf.jpa.daos.*;
import com.tecgensoft.turf.service.domain.TurfModels;

public final class TurfMapper {
  private TurfMapper() {}

  public static TurfModels.Venue toEntity(VenueDao d) {
    return new TurfModels.Venue(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getName(),
        d.getPhone(),
        d.getAddress(),
        d.getTimezone(),
        d.getOpeningHour(),
        d.getClosingHour(),
        d.getPeakStartHour(),
        d.getAdvancePercentage(),
        d.getHoldMinutes(),
        d.getMaxDurationHours(),
        d.getBookingWindowDays(),
        d.getCancellationCutoffHours());
  }

  public static TurfModels.Reservation toEntity(ReservationDao d) {
    return new TurfModels.Reservation(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getCourtId(),
        d.getStartAt(),
        d.getEndAt(),
        d.getKind(),
        d.getStatus(),
        d.getExpiresAt(),
        d.getReason());
  }

  public static TurfModels.Booking toEntity(BookingDao d) {
    return new TurfModels.Booking(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getCourtId(),
        d.getReservationId(),
        d.getLocalDate(),
        d.getStartHour(),
        d.getDurationHours(),
        d.getCustomerName(),
        d.getCustomerPhone(),
        d.getCustomerEmail(),
        d.getTotal(),
        d.getRequiredAdvance(),
        d.getStatus(),
        d.getSource(),
        d.getNotes(),
        d.getCancellationReason(),
        d.getCancelledAt(),
        d.getCancelledBy(),
        d.getSubmittedReference());
  }

  public static TurfModels.Payment toEntity(PaymentDao d) {
    return new TurfModels.Payment(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getBookingId(),
        d.getAmount(),
        d.getMethod(),
        d.getKind(),
        d.getReference(),
        d.getIdempotencyKey(),
        d.getRecordedBy());
  }

  public static TurfModels.Content toEntity(ContentDao d) {
    return new TurfModels.Content(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getKind(),
        d.getTitle(),
        d.getBody(),
        d.getImageUrl(),
        d.getSortOrder(),
        d.isActive());
  }
}
