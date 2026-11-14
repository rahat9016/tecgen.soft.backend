package com.familiemunshi.http.dtos.requests;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class SaasRequests {
  public record Availability(Boolean active, Boolean availableForSale) {}

  public record Organization(@NotBlank String name, @NotNull UUID adminUserId) {
    public com.familiemunshi.service.domain.SaasCommands.Organization toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Organization(name, adminUserId);
    }
  }

  public record OrganizationPatch(String name, Boolean active, UUID adminUserId) {
    public com.familiemunshi.service.domain.SaasCommands.OrganizationPatch toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.OrganizationPatch(
          name, active, adminUserId);
    }
  }

  public record Plan(
      @NotBlank String code,
      @NotBlank String displayName,
      @NotBlank @Pattern(regexp = "TURF") String product,
      String description,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal price,
      @NotNull Set<String> features,
      @NotNull Map<String, Integer> limits,
      boolean active,
      boolean availableForSale) {
    public com.familiemunshi.service.domain.SaasCommands.Plan toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Plan(
          code,
          displayName,
          product,
          description,
          currency,
          price,
          features,
          limits,
          active,
          availableForSale);
    }
  }

  public record Subscription(
      @NotNull UUID packageId,
      @NotBlank @Pattern(regexp = "ACTIVE|SUSPENDED|CANCELLED") String status,
      @NotBlank @Pattern(regexp = "MONTHLY|YEARLY") String billingPeriod,
      @NotNull Instant startsAt,
      @NotNull Instant endsAt,
      @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal agreedPrice) {
    public com.familiemunshi.service.domain.SaasCommands.Subscription toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Subscription(
          packageId, status, billingPeriod, startsAt, endsAt, agreedPrice);
    }
  }

  public record Domain(
      @NotBlank String hostname, @NotBlank @Pattern(regexp = "TURF") String product) {
    public com.familiemunshi.service.domain.SaasCommands.Domain toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Domain(hostname, product);
    }
  }

  public record Branch(@NotBlank String name, String address, String phone) {
    public com.familiemunshi.service.domain.SaasCommands.Branch toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Branch(name, address, phone);
    }
  }

  public record Resolve(@NotBlank String hostname) {
    public com.familiemunshi.service.domain.SaasCommands.Resolve toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Resolve(hostname);
    }
  }

  public record Access(
      @NotNull UUID organizationId,
      UUID branchId,
      @NotBlank @Pattern(regexp = "TURF") String product,
      @NotBlank String permissionCode) {
    public com.familiemunshi.service.domain.SaasCommands.Access toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.Access(
          organizationId, branchId, product, permissionCode);
    }
  }

  public record RolePermissions(@NotNull Set<String> permissions) {
    public com.familiemunshi.service.domain.SaasCommands.RolePermissions toEntity() {
      return new com.familiemunshi.service.domain.SaasCommands.RolePermissions(permissions);
    }
  }
}
