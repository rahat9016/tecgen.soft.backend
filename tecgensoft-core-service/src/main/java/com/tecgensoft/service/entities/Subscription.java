package com.tecgensoft.service.entities;

import com.tecgensoft.service.enums.BillingPeriod;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.SubscriptionStatus;

import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record Subscription(
        UUID id,
        UUID organizationId,
        UUID packageId,
        String packageCode,
        int packageVersion,
        ProductCode product,
        SubscriptionStatus status,
        BillingPeriod billingPeriod,
        Instant startsAt,
        Instant endsAt,
        BigDecimal agreedPrice,
        Instant createdAt,
        boolean isTrial,
        Instant trialStartDate,
        Instant trialEndDate,
        Instant activatedAt,
        Instant cancelledAt,
        Instant updatedAt,
        int graceDays
) { }
