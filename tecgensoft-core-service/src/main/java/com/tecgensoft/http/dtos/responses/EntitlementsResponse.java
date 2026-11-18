package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.enums.ProductCode;

import java.util.Map;
import java.util.UUID;

public record EntitlementsResponse(
        UUID organizationId,
        UUID packageId,
        String packageCode,
        int version,
        ProductCode product,
        String productCode,
        String subscriptionStatus,
        boolean active,
        boolean isActive,
        boolean isTrial,
        long trialDaysRemaining,
        Map<String, Object> features,
        Map<String, Integer> limits
) { }
