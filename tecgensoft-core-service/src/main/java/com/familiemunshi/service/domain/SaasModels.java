package com.familiemunshi.service.domain;

import java.time.*;
import java.util.*;

public final class SaasModels {
  public record Organization(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      String name,
      boolean active,
      UUID adminUserId,
      UUID adminRoleId) {}

  public record ProductPackage(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      String code,
      String displayName,
      String product,
      String description,
      String currency,
      java.math.BigDecimal price,
      int version,
      boolean active,
      boolean availableForSale,
      java.util.Set<String> features,
      java.util.Map<String, Integer> limits) {}

  public record Subscription(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,
      UUID packageId,
      String product,
      String status,
      String billingPeriod,
      Instant startsAt,
      Instant endsAt,
      java.math.BigDecimal agreedPrice,
      String currency) {}

  public record TenantDomain(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,
      String product,
      String hostname,
      String challenge,
      Instant verifiedAt) {}

  public record AdminAssignmentAudit(
      UUID id,
      Instant createdAt,
      Instant updatedAt,
      UUID organizationId,
      UUID userId,
      UUID actorId) {}
}
