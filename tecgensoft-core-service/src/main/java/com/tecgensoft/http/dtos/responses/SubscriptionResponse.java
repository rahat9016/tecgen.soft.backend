package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.Subscription;
import com.tecgensoft.service.enums.BillingPeriod;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
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
        Instant updatedAt
) {
    public SubscriptionResponse(Subscription value) {
        this(value.id(), value.organizationId(), value.packageId(), value.packageCode(),
                value.packageVersion(), value.product(), value.status(), value.billingPeriod(),
                value.startsAt(), value.endsAt(), value.agreedPrice(), value.createdAt(), value.isTrial(),
                value.trialStartDate(), value.trialEndDate(), value.activatedAt(), value.cancelledAt(), value.updatedAt());
    }
}
