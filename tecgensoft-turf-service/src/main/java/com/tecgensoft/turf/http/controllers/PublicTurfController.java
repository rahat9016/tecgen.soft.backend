package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.http.dtos.TurfRequests.*;
import com.tecgensoft.turf.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/public/turf/organizations/{organizationId}")
public class PublicTurfController {
  private final CoreClient core;
  private final TurfService service;

  private TurfService.Context context(String host, UUID organizationId, String feature) {
    var t = core.resolve(host);
    if (!t.organizationId().equals(organizationId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
    if (!Boolean.TRUE.equals(core.entitlements(t.organizationId()).features().get(feature)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feature disabled");
    return new TurfService.Context(t.organizationId());
  }

  private Object ok(Object value) {
    return Map.of("success", true, "message", "Success", "data", value);
  }

  @GetMapping("/venue")
  public Object venue(HttpServletRequest request, @PathVariable UUID organizationId) {
    return ok(
        com.tecgensoft.turf.mapper.TurfMapper.toEntity(
            service.venue(context(request.getServerName(), organizationId, "VENUE_SETTINGS"))));
  }

  @GetMapping("/content")
  public Object content(HttpServletRequest request, @PathVariable UUID organizationId) {
    return ok(service.contents(context(request.getServerName(), organizationId, "WEBSITE_CONTENT"), true));
  }

  @GetMapping("/courts")
  public Object courts(HttpServletRequest request, @PathVariable UUID organizationId) {
    return ok(service.courts(context(request.getServerName(), organizationId, "COURT_MANAGEMENT"), true));
  }

  @GetMapping("/availability")
  public Object availability(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @RequestParam UUID courtId,
      @RequestParam LocalDate date,
      @RequestParam(defaultValue = "1") @Min(1) @Max(24) int durationHours) {
    return ok(
        service.availability(
            context(request.getServerName(), organizationId, "ONLINE_BOOKING"), courtId, date, durationHours));
  }

  @PostMapping("/holds")
  @ResponseStatus(HttpStatus.CREATED)
  public Object hold(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @Valid @RequestBody Hold r) {
    return ok(service.hold(context(request.getServerName(), organizationId, "ONLINE_BOOKING"), r.slot().toEntity()));
  }

  @PatchMapping("/holds/{id}/release")
  public Object release(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Secret r) {
    service.release(context(request.getServerName(), organizationId, "ONLINE_BOOKING"), id, r.secret());
    return ok("Hold released");
  }

  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public Object checkout(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @Valid @RequestBody Checkout r) {
    return ok(service.checkout(context(request.getServerName(), organizationId, "ONLINE_BOOKING"), r.toEntity()));
  }

  @PostMapping("/direct-bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public Object directBooking(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @Valid @RequestBody DirectBooking body) {
    var ctx = context(request.getServerName(), organizationId, "ONLINE_BOOKING");
    return ok(service.createDirectBooking(ctx, body.slot().toEntity(), body.customerPhone(),
        body.customerName(), body.notes(), "PUBLIC", null));
  }

  @PatchMapping("/bookings/{id}/payment-reference")
  public Object reference(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Reference r) {
    service.submitReference(context(request.getServerName(), organizationId, "ONLINE_BOOKING"), id, r.toEntity());
    return ok("Reference submitted for admin verification");
  }
}
