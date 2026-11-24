package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.SubscriptionPlan;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.BillingPeriod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record PackageResponse(
        UUID id,
        String code,
        int version,
        String displayName,
        ProductCode product,
        BillingPeriod billingPeriod,
        String description,
        String currency,
        BigDecimal price,
        Set<String> features,
        Map<String, Integer> limits,
        boolean active,
        boolean availableForSale,
        Instant createdAt,
        Instant updatedAt
) {
    public PackageResponse(SubscriptionPlan value) {
        this(value.id(), value.code(), value.version(), value.displayName(), value.product(), value.billingPeriod(),
                value.description(), value.currency(), value.price(), value.features(), value.limits(),
                value.active(), value.availableForSale(), value.createdAt(), value.updatedAt());
    }
}
