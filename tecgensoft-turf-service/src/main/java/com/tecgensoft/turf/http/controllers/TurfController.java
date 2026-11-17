package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.http.dtos.TurfRequests.*;
import com.tecgensoft.turf.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/turf/organizations/{organizationId}")
public class TurfController {
  private final CoreClient core;
  private final TurfService service;

  private record Authorized(TurfService.Context context, UUID actor) {}

  private Authorized authorize(HttpServletRequest request, UUID organizationId, String permission) {
    var tenant = core.resolve(request.getHeader("X-Frontend-Host"));
    if (!tenant.organizationId().equals(organizationId))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Organization access denied");
    UUID actor = core.check(tenant, request.getHeader("Authorization"), permission);
    return new Authorized(new TurfService.Context(tenant.organizationId()), actor);
  }

  private Object ok(Object value) {
    return Map.of("success", true, "message", "Success", "data", value);
  }

  @GetMapping("/settings")
  public Object settings(HttpServletRequest r, @PathVariable UUID organizationId) {
    return ok(
        com.tecgensoft.turf.mapper.TurfMapper.toEntity(
            service.venue(authorize(r, organizationId, "TURF_SETTINGS_READ").context())));
  }

  @PatchMapping("/settings")
  public Object settings(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody Venue body) {
    return ok(
        service.settings(authorize(r, organizationId, "TURF_SETTINGS_UPDATE").context(), body.toEntity()));
  }

  @PostMapping("/settings")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object createSettings(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody Venue body) {
    return ok(
        service.createSettings(
            authorize(r, organizationId, "TURF_SETTINGS_UPDATE").context(), body.toEntity()));
  }

  @GetMapping("/courts")
  public Object courts(HttpServletRequest r, @PathVariable UUID organizationId) {
    return ok(service.courts(authorize(r, organizationId, "TURF_COURT_READ").context(), false));
  }

  @GetMapping("/courts/{id}")
  public Object court(HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    return ok(
        com.tecgensoft.turf.mapper.CourtMapper.toEntity(
            service.court(authorize(r, organizationId, "TURF_COURT_READ").context(), id)));
  }

  @PostMapping("/courts")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object court(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody Court body) {
    return ok(
        service.createCourt(authorize(r, organizationId, "TURF_COURT_CREATE").context(), body.toEntity()));
  }

  @PatchMapping("/courts/{id}")
  public Object court(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Court body) {
    return ok(
        service.updateCourt(
            authorize(r, organizationId, "TURF_COURT_UPDATE").context(), id, body.toEntity()));
  }

  @DeleteMapping("/courts/{id}")
  public Object deleteCourt(
      HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    service.deleteCourt(authorize(r, organizationId, "TURF_COURT_DELETE").context(), id);
    return ok("Court deactivated");
  }

  @GetMapping("/availability")
  public Object availability(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @RequestParam UUID courtId,
      @RequestParam LocalDate date,
      @RequestParam(defaultValue = "1") @Min(1) @Max(24) int durationHours) {
    return ok(
        service.availability(
            authorize(r, organizationId, "TURF_BOOKING_READ").context(), courtId, date, durationHours));
  }

  @PostMapping("/bookings")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object booking(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody AdminBooking body) {
    return ok(
        service.adminBooking(
            authorize(r, organizationId, "TURF_BOOKING_CREATE").context(), body.toEntity()));
  }

  @GetMapping("/bookings")
  public Object bookings(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to,
      @RequestParam(defaultValue = "0") @Min(0) int page) {
    return ok(
        service.bookings(authorize(r, organizationId, "TURF_BOOKING_READ").context(), from, to, page));
  }

  @GetMapping("/bookings/{id}")
  public Object booking(HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    return ok(service.detail(authorize(r, organizationId, "TURF_BOOKING_READ").context(), id));
  }

  @PatchMapping("/bookings/{id}/cancellation")
  public Object cancel(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Cancel body) {
    var a = authorize(r, organizationId, "TURF_BOOKING_CANCEL");
    return ok(service.cancel(a.context(), id, body.reason(), a.actor()));
  }

  @PostMapping("/bookings/{id}/payments")
  public Object payment(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Payment body) {
    var a =
        authorize(
            r,
            organizationId,
            "REFUND".equals(body.kind()) ? "TURF_PAYMENT_REFUND" : "TURF_PAYMENT_RECORD");
    return ok(service.payment(a.context(), id, body.toEntity(), a.actor()));
  }

  @GetMapping("/bookings/{id}/payments")
  public Object payments(HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    return ok(service.ledger(authorize(r, organizationId, "TURF_PAYMENT_READ").context(), id));
  }

  @PostMapping("/blocks")
  public Object block(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody Block body) {
    return ok(service.block(authorize(r, organizationId, "TURF_BLOCK_MANAGE").context(), body.toEntity()));
  }

  @GetMapping("/blocks")
  public Object blocks(HttpServletRequest r, @PathVariable UUID organizationId) {
    return ok(service.blocks(authorize(r, organizationId, "TURF_BLOCK_MANAGE").context()));
  }

  @PatchMapping("/blocks/{id}/release")
  public Object releaseBlock(
      HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    service.releaseBlock(authorize(r, organizationId, "TURF_BLOCK_MANAGE").context(), id);
    return ok("Block released");
  }

  @GetMapping("/content")
  public Object contents(HttpServletRequest r, @PathVariable UUID organizationId) {
    return ok(service.contents(authorize(r, organizationId, "TURF_CONTENT_READ").context(), false));
  }

  @PostMapping("/content")
  public Object content(
      HttpServletRequest r, @PathVariable UUID organizationId, @Valid @RequestBody Content body) {
    return ok(
        service.content(
            authorize(
                    r,
                    organizationId,
                    "GALLERY".equals(body.kind()) ? "TURF_GALLERY_MANAGE" : "TURF_CONTENT_UPDATE")
                .context(),
            null,
            body.toEntity()));
  }

  @PatchMapping("/content/{id}")
  public Object content(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Content body) {
    var a = authorize(r, organizationId, "TURF_CONTENT_UPDATE");
    if ("GALLERY".equals(body.kind()) || "GALLERY".equals(service.contentKind(a.context(), id)))
      authorize(r, organizationId, "TURF_GALLERY_MANAGE");
    return ok(service.content(a.context(), id, body.toEntity()));
  }

  @DeleteMapping("/content/{id}")
  public Object deleteContent(
      HttpServletRequest r, @PathVariable UUID organizationId, @PathVariable UUID id) {
    var a = authorize(r, organizationId, "TURF_CONTENT_UPDATE");
    if ("GALLERY".equals(service.contentKind(a.context(), id)))
      authorize(r, organizationId, "TURF_GALLERY_MANAGE");
    service.deleteContent(a.context(), id);
    return ok("Content deleted");
  }

  @GetMapping("/reports")
  public Object report(
      HttpServletRequest r,
      @PathVariable UUID organizationId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to) {
    return ok(service.report(authorize(r, organizationId, "TURF_REPORT_READ").context(), from, to));
  }
}
