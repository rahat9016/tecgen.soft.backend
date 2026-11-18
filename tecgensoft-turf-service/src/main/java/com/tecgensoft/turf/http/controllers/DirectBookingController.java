package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.http.dtos.TurfRequests.BookingRequest;
import com.tecgensoft.turf.service.CoreClient;
import com.tecgensoft.turf.service.TurfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name="Direct Turf Booking",description="Account-free public bookings and JWT-authorized organization admin bookings")
public class DirectBookingController {
  private final CoreClient core;
  private final TurfService service;

  private Object response(Object data) {
    return Map.of("success", true, "message", "Success", "data", data);
  }

  @PostMapping("/api/v1/public/turf/bookings")
  @Operation(summary="Create public customer booking",description="Requires a verified tenant hostname, active TURF subscription, and ONLINE_BOOKING. No customer account is required.")
  @ResponseStatus(HttpStatus.CREATED)
  public Object publicBooking(HttpServletRequest request, @Valid @RequestBody BookingRequest body) {
    var tenant = core.resolve(request.getServerName());
    var booking = body.toEntity();
    return response(service.createDirectBooking(new TurfService.Context(tenant.organizationId()),
        booking.slot(), booking.customerPhone(), booking.customerName(), booking.notes(), "PUBLIC", null));
  }

  @PostMapping("/api/v1/admin/turf/bookings")
  @Operation(summary="Create booking as organization admin",description="Requires a user JWT, organization membership, TURF_BOOKING_CREATE permission, active subscription, and ADMIN_BOOKING feature.")
  @ResponseStatus(HttpStatus.CREATED)
  public Object adminBooking(HttpServletRequest request, @Valid @RequestBody BookingRequest body) {
    var tenant = core.resolve(request.getServerName());
    UUID actor = core.check(tenant, request.getHeader("Authorization"), "TURF_BOOKING_CREATE");
    var booking = body.toEntity();
    return response(service.createDirectBooking(new TurfService.Context(tenant.organizationId()),
        booking.slot(), booking.customerPhone(), booking.customerName(), booking.notes(), "ADMIN", actor));
  }
}
