package com.tecgensoft.service;

import com.tecgensoft.common.exceptions.StorageException;
import com.tecgensoft.common.service.FileStorageService;
import com.tecgensoft.http.dtos.responses.PublicTenantResponse;
import com.tecgensoft.service.enums.ProductCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/** Builds the public view of a tenant: branding and enabled features, never billing data. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicTenantService {
    private final SubscriptionService subscriptionService;
    private final FileStorageService fileStorageService;

    /** Empty when the organization or its subscription is not active. */
    public Optional<PublicTenantResponse> describe(OrganizationDomainService.DomainTenant tenant) {
        if (!tenant.active()) {
            return Optional.empty();
        }
        return Arrays.stream(ProductCode.values())
                .filter(product -> product.name().equals(tenant.product()))
                .findFirst()
                .map(product -> subscriptionService.getEntitlements(tenant.organizationId(), product))
                .filter(SubscriptionService.EntitlementSnapshot::active)
                .map(entitlements -> new PublicTenantResponse(
                        tenant.organizationId(),
                        tenant.name(),
                        publicLogoUrl(tenant.logoUrl()),
                        tenant.product(),
                        entitlements.features().entrySet().stream()
                                .filter(feature -> Boolean.TRUE.equals(feature.getValue()))
                                .map(java.util.Map.Entry::getKey)
                                .sorted()
                                .toList()
                ));
    }

    private String publicLogoUrl(String logo) {
        if (logo == null || logo.isBlank() || logo.startsWith("https://")) {
            return logo;
        }
        try {
            return fileStorageService.getPublicUrl(logo);
        } catch (StorageException ex) {
            log.warn("Organization logo has no public URL: {}", ex.getMessage());
            return null;
        }
    }
}
