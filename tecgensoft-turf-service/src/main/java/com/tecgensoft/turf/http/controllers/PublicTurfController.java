package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.common.TenantHostResolver;
import com.tecgensoft.turf.http.dtos.TurfRequests.*;
import com.tecgensoft.turf.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Public site API. The organization always comes from the site hostname. The organization path is
 * the older form of the same routes and must match the hostname's organization.
 */
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping({
  "/api/v1/public/turf",
  "/api/v1/public/turf/organizations/{organizationId}"
})
@Tag(name = "Public Turf", description = "Account-free site content and hold-then-checkout booking")
public class PublicTurfController {
  private final CoreClient core;
  private final TurfService service;
  private final TenantHostResolver tenantHost;

  private TurfService.Context context(
      HttpServletRequest request, UUID organizationId, String feature) {
    var t = core.resolve(tenantHost.resolve(request));
    if (organizationId != null && !t.organizationId().equals(organizationId))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found");
    if (!Boolean.TRUE.equals(core.entitlements(t.organizationId()).features().get(feature)))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feature disabled");
    return new TurfService.Context(t.organizationId());
  }

  private Object ok(Object value) {
    return Map.of("success", true, "message", "Success", "data", value);
  }

  @GetMapping("/venue")
  public Object venue(
      HttpServletRequest request, @PathVariable(required = false) UUID organizationId) {
    return ok(
        com.tecgensoft.turf.mapper.TurfMapper.toEntity(
            service.venue(context(request, organizationId, "VENUE_SETTINGS"))));
  }

  @GetMapping("/content")
  public Object content(
      HttpServletRequest request, @PathVariable(required = false) UUID organizationId) {
    return ok(service.contents(context(request, organizationId, "WEBSITE_CONTENT"), true));
  }

  @GetMapping("/courts")
  public Object courts(
      HttpServletRequest request, @PathVariable(required = false) UUID organizationId) {
    return ok(service.courts(context(request, organizationId, "COURT_MANAGEMENT"), true));
  }

  @GetMapping("/availability")
  public Object availability(
      HttpServletRequest request,
      @PathVariable(required = false) UUID organizationId,
      @RequestParam UUID courtId,
      @RequestParam LocalDate date,
      @RequestParam(defaultValue = "1") @Min(1) @Max(24) int durationHours) {
    return ok(
        service.availability(
            context(request, organizationId, "ONLINE_BOOKING"), courtId, date, durationHours));
  }

  @PostMapping("/holds")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Hold a slot", description = "Returns the hold ID and the secret needed at checkout.")
  public Object hold(
      HttpServletRequest request,
      @PathVariable(required = false) UUID organizationId,
      @Valid @RequestBody Hold r) {
    return ok(service.hold(context(request, organizationId, "ONLINE_BOOKING"), r.slot().toEntity()));
  }

  @PatchMapping("/holds/{id}/release")
  public Object release(
      HttpServletRequest request,
      @PathVariable(required = false) UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Secret r) {
    service.release(context(request, organizationId, "ONLINE_BOOKING"), id, r.secret());
    return ok("Hold released");
  }

  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Check out a hold",
      description = "PENDING_PAYMENT when the venue requires an advance, otherwise CONFIRMED.")
  public Object checkout(
      HttpServletRequest request,
      @PathVariable(required = false) UUID organizationId,
      @Valid @RequestBody Checkout r) {
    return ok(service.checkout(context(request, organizationId, "ONLINE_BOOKING"), r.toEntity()));
  }

  @PatchMapping("/bookings/{id}/payment-reference")
  public Object reference(
      HttpServletRequest request,
      @PathVariable(required = false) UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Reference r) {
    service.submitReference(context(request, organizationId, "ONLINE_BOOKING"), id, r.toEntity());
    return ok("Reference submitted for admin verification");
  }
}
