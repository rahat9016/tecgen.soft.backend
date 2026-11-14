package com.familiemunshi.http.controllers;

import com.familiemunshi.http.dtos.responses.SuccessResponse;
import com.familiemunshi.service.SaasService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrganizationController {
  private final SaasService service;

  @GetMapping("/api/v1/organizations/me")
  public Object mine(Authentication a) {
    return SuccessResponse.of(
        "Organizations retrieved", service.myOrganizations(UUID.fromString(a.getName())));
  }
}
