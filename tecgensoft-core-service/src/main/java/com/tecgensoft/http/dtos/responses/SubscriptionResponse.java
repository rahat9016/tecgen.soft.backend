package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.Subscription;
import com.tecgensoft.service.enums.BillingPeriod;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private UUID id;
    private UUID organizationId;
    private UUID packageId;
    private String packageCode;
    private int packageVersion;
    private ProductCode product;
    private SubscriptionStatus status;
    private BillingPeriod billingPeriod;
    private Instant startsAt;
    private Instant endsAt;
    private BigDecimal agreedPrice;
    private Instant createdAt;
    private boolean isTrial;
    private Instant trialStartDate;
    private Instant trialEndDate;
    private Instant activatedAt;
    private Instant cancelledAt;
    private Instant updatedAt;
    private int graceDays;

    public SubscriptionResponse(Subscription value) {
        this.id = value.id();
        this.organizationId = value.organizationId();
        this.packageId = value.packageId();
        this.packageCode = value.packageCode();
        this.packageVersion = value.packageVersion();
        this.product = value.product();
        this.status = value.status();
        this.billingPeriod = value.billingPeriod();
        this.startsAt = value.startsAt();
        this.endsAt = value.endsAt();
        this.agreedPrice = value.agreedPrice();
        this.createdAt = value.createdAt();
        this.isTrial = value.isTrial();
        this.trialStartDate = value.trialStartDate();
        this.trialEndDate = value.trialEndDate();
        this.activatedAt = value.activatedAt();
        this.cancelledAt = value.cancelledAt();
        this.updatedAt = value.updatedAt();
        this.graceDays = value.graceDays();
    }
}
