package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.http.dtos.TurfRequests.*;
import com.tecgensoft.turf.service.*;
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
    if (!core.entitlements(t.organizationId()).features().contains(feature))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feature disabled");
    return new TurfService.Context(t.organizationId());
  }

  private Object ok(Object value) {
    return Map.of("success", true, "message", "Success", "data", value);
  }

  @GetMapping("/venue")
  public Object venue(@RequestHeader("X-Frontend-Host") String host, @PathVariable UUID organizationId) {
    return ok(
        com.tecgensoft.turf.mapper.TurfMapper.toEntity(
            service.venue(context(host, organizationId, "VENUE_SETTINGS"))));
  }

  @GetMapping("/content")
  public Object content(@RequestHeader("X-Frontend-Host") String host, @PathVariable UUID organizationId) {
    return ok(service.contents(context(host, organizationId, "WEBSITE_CONTENT"), true));
  }

  @GetMapping("/courts")
  public Object courts(@RequestHeader("X-Frontend-Host") String host, @PathVariable UUID organizationId) {
    return ok(service.courts(context(host, organizationId, "COURT_MANAGEMENT"), true));
  }

  @GetMapping("/availability")
  public Object availability(
      @RequestHeader("X-Frontend-Host") String host,
      @PathVariable UUID organizationId,
      @RequestParam UUID courtId,
      @RequestParam LocalDate date,
      @RequestParam(defaultValue = "1") @Min(1) @Max(24) int durationHours) {
    return ok(
        service.availability(
            context(host, organizationId, "ONLINE_BOOKING"), courtId, date, durationHours));
  }

  @PostMapping("/holds")
  @ResponseStatus(HttpStatus.CREATED)
  public Object hold(
      @RequestHeader("X-Frontend-Host") String host,
      @PathVariable UUID organizationId,
      @Valid @RequestBody Hold r) {
    return ok(service.hold(context(host, organizationId, "ONLINE_BOOKING"), r.slot().toEntity()));
  }

  @PatchMapping("/holds/{id}/release")
  public Object release(
      @RequestHeader("X-Frontend-Host") String host,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Secret r) {
    service.release(context(host, organizationId, "ONLINE_BOOKING"), id, r.secret());
    return ok("Hold released");
  }

  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public Object checkout(
      @RequestHeader("X-Frontend-Host") String host,
      @PathVariable UUID organizationId,
      @Valid @RequestBody Checkout r) {
    return ok(service.checkout(context(host, organizationId, "ONLINE_BOOKING"), r.toEntity()));
  }

  @PatchMapping("/bookings/{id}/payment-reference")
  public Object reference(
      @RequestHeader("X-Frontend-Host") String host,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Reference r) {
    service.submitReference(context(host, organizationId, "ONLINE_BOOKING"), id, r.toEntity());
    return ok("Reference submitted for admin verification");
  }
}
