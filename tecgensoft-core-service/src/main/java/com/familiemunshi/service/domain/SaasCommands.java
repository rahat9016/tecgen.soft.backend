package com.familiemunshi.service.domain;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class SaasCommands {
  public record Organization(String name, UUID adminUserId) {}

  public record OrganizationPatch(String name, Boolean active, UUID adminUserId) {}

  public record Plan(
      String code,
      String displayName,
      String product,
      String description,
      String currency,
      BigDecimal price,
      Set<String> features,
      Map<String, Integer> limits,
      boolean active,
      boolean availableForSale) {}

  public record Subscription(
      UUID packageId,
      String status,
      String billingPeriod,
      Instant startsAt,
      Instant endsAt,
      BigDecimal agreedPrice) {}

  public record Domain(String hostname, String product) {}

  public record Branch(String name, String address, String phone) {}

  public record Resolve(String hostname) {}

  public record Access(UUID organizationId, UUID branchId, String product, String permissionCode) {}

  public record RolePermissions(Set<String> permissions) {}
}
