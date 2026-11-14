package com.familiemunshi.http.controllers;

import com.familiemunshi.http.dtos.requests.SaasRequests.*;
import com.familiemunshi.http.dtos.responses.SuccessResponse;
import com.familiemunshi.jpa.repositories.*;
import com.familiemunshi.service.SaasService;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/saas")
@RequiredArgsConstructor
@PreAuthorize("@saasService.isSuperAdmin(authentication)")
public class SaasController {
  private final SaasService service;

  private UUID actor(Authentication a) {
    return UUID.fromString(a.getName());
  }

  private <T> SuccessResponse<T> ok(T value) {
    return SuccessResponse.of("Success", value);
  }

  @GetMapping("/organizations")
  public Object organizations(@RequestParam(defaultValue = "0") int page) {
    return ok(service.listOrganizations(page));
  }

  @PostMapping("/organizations")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object create(@Valid @RequestBody Organization r, Authentication a) {
    return ok(service.createOrganization(r.toEntity(), actor(a)));
  }

  @GetMapping("/organizations/{id}")
  public Object organization(@PathVariable UUID id) {
    return ok(service.organizationView(id));
  }

  @PatchMapping("/organizations/{id}")
  public Object update(
      @PathVariable UUID id, @Valid @RequestBody OrganizationPatch r, Authentication a) {
    return ok(service.patchOrganization(id, r.toEntity(), actor(a)));
  }

  @PostMapping("/packages")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object plan(@Valid @RequestBody Plan r, Authentication a) {
    return ok(service.createPlan(r.toEntity(), actor(a)));
  }

  @PatchMapping("/packages/{id}/availability")
  public Object availability(@PathVariable UUID id, @RequestBody Availability r) {
    return ok(service.packageAvailability(id, r.active(), r.availableForSale()));
  }

  @GetMapping("/packages")
  public Object packages(@RequestParam(defaultValue = "0") int page) {
    return ok(service.listPackages(page));
  }

  @PostMapping("/organizations/{id}/subscriptions")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object subscribe(
      @PathVariable UUID id, @Valid @RequestBody Subscription r, Authentication a) {
    return ok(service.subscribe(id, r.toEntity(), actor(a)));
  }

  @PatchMapping("/organizations/{id}/subscription")
  public Object updateSubscription(
      @PathVariable UUID id, @Valid @RequestBody Subscription r, Authentication a) {
    return ok(service.subscribe(id, r.toEntity(), actor(a)));
  }

  @GetMapping("/organizations/{id}/entitlements")
  public Object entitlements(@PathVariable UUID id) {
    return ok(service.entitlements(id, "TURF"));
  }

  @PostMapping("/organizations/{id}/domains")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object domain(@PathVariable UUID id, @Valid @RequestBody Domain r) {
    return ok(service.addDomain(id, r.toEntity()));
  }

  @PostMapping("/organizations/{id}/domains/{domainId}/verify")
  public Object verify(@PathVariable UUID id, @PathVariable UUID domainId) {
    return ok(service.verifyDomain(id, domainId));
  }

  @PostMapping("/organizations/{id}/branches")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object branch(@PathVariable UUID id, @Valid @RequestBody Branch r) {
    return ok(service.createBranch(id, r.toEntity()));
  }

  @PatchMapping("/organizations/{id}/role-permissions")
  public Object permissions(@PathVariable UUID id, @Valid @RequestBody RolePermissions r) {
    return ok(service.patchPermissions(id, r.permissions()));
  }
}
