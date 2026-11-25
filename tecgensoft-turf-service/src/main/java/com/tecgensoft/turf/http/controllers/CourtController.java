package com.tecgensoft.turf.http.controllers;

import com.tecgensoft.turf.common.SuccessResponse;
import com.tecgensoft.turf.http.dtos.TurfRequests.Court;
import com.tecgensoft.turf.http.dtos.responses.CourtResponse;
import com.tecgensoft.turf.service.CoreClient;
import com.tecgensoft.turf.service.CourtService;
import com.tecgensoft.turf.service.TurfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/turf/organizations/{organizationId}/courts")
public class CourtController {
  private final CoreClient core;
  private final CourtService courts;

  /** A valid token, membership of the path organization, the permission, then active entitlements. */
  private TurfService.Context authorize(
      HttpServletRequest request, UUID organizationId, String permission) {
    core.check(organizationId, request.getHeader("Authorization"), permission);
    core.entitlements(organizationId);
    return new TurfService.Context(organizationId);
  }

  @GetMapping
  public ResponseEntity<SuccessResponse<List<CourtResponse>>> list(
      HttpServletRequest request, @PathVariable UUID organizationId) {
    var context = authorize(request, organizationId, "TURF_COURT_VIEW");
    return ResponseEntity.ok(SuccessResponse.of("Success", courts.list(context, false)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<CourtResponse>> get(
      HttpServletRequest request, @PathVariable UUID organizationId, @PathVariable UUID id) {
    var context = authorize(request, organizationId, "TURF_COURT_VIEW");
    return ResponseEntity.ok(SuccessResponse.of("Success", courts.get(context, id)));
  }

  @PostMapping
  public ResponseEntity<SuccessResponse<CourtResponse>> create(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @Valid @RequestBody Court body) {
    var context = authorize(request, organizationId, "TURF_COURT_CREATE");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of("Success", courts.create(context, body.toEntity())));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<SuccessResponse<CourtResponse>> update(
      HttpServletRequest request,
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody Court body) {
    var context = authorize(request, organizationId, "TURF_COURT_UPDATE");
    return ResponseEntity.ok(SuccessResponse.of("Success", courts.update(context, id, body.toEntity())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<SuccessResponse<String>> delete(
      HttpServletRequest request, @PathVariable UUID organizationId, @PathVariable UUID id) {
    var context = authorize(request, organizationId, "TURF_COURT_DELETE");
    courts.delete(context, id);
    return ResponseEntity.ok(SuccessResponse.of("Success", "Court deactivated"));
  }
}
