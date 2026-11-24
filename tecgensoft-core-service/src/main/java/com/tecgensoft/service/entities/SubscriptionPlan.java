package com.tecgensoft.service.entities;

import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.BillingPeriod;

import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
@With
public record SubscriptionPlan(
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
) { }
