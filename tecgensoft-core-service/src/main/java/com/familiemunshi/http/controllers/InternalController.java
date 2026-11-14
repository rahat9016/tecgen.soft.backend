package com.familiemunshi.http.controllers;

import com.familiemunshi.common.utils.JwtUtil;
import com.familiemunshi.http.dtos.requests.SaasRequests.*;
import com.familiemunshi.service.SaasService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalController {
  private final SaasService service;
  private final JwtUtil jwt;

  @PostMapping("/tenants/resolve")
  public Object resolve(@Valid @RequestBody Resolve r) {
    return service.resolve(r.hostname());
  }

  @PostMapping("/access/check")
  public Object check(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody Access r) {
    if (authorization == null
        || !authorization.startsWith("Bearer ")
        || !jwt.validateUserToken(authorization.substring(7)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user access token");
    return service.check(jwt.extractUserId(authorization.substring(7)), r.toEntity());
  }

  @GetMapping("/organizations/{id}/entitlements")
  public Object entitlements(@PathVariable UUID id, @RequestParam String product) {
    if (!"TURF".equals(product))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported product");
    return service.entitlements(id, product);
  }
}
