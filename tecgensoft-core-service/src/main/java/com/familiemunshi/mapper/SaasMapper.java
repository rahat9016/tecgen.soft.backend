package com.familiemunshi.mapper;

import com.familiemunshi.jpa.daos.*;
import com.familiemunshi.service.domain.SaasModels;

public final class SaasMapper {
  private SaasMapper() {}

  public static SaasModels.Organization toEntity(OrganizationDao d) {
    return new SaasModels.Organization(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getName(),
        d.isActive(),
        d.getAdminUserId(),
        d.getAdminRoleId());
  }

  public static SaasModels.ProductPackage toEntity(ProductPackageDao d) {
    return new SaasModels.ProductPackage(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getCode(),
        d.getDisplayName(),
        d.getProduct(),
        d.getDescription(),
        d.getCurrency(),
        d.getPrice(),
        d.getVersion(),
        d.isActive(),
        d.isAvailableForSale(),
        d.getFeatures(),
        d.getLimits());
  }

  public static SaasModels.Subscription toEntity(SubscriptionDao d) {
    return new SaasModels.Subscription(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getPackageId(),
        d.getProduct(),
        d.getStatus(),
        d.getBillingPeriod(),
        d.getStartsAt(),
        d.getEndsAt(),
        d.getAgreedPrice(),
        d.getCurrency());
  }

  public static SaasModels.TenantDomain toEntity(TenantDomainDao d) {
    return new SaasModels.TenantDomain(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getProduct(),
        d.getHostname(),
        d.getChallenge(),
        d.getVerifiedAt());
  }

  public static SaasModels.AdminAssignmentAudit toEntity(AdminAssignmentAuditDao d) {
    return new SaasModels.AdminAssignmentAudit(
        d.getId(),
        d.getCreatedAt(),
        d.getUpdatedAt(),
        d.getOrganizationId(),
        d.getUserId(),
        d.getActorId());
  }
}
