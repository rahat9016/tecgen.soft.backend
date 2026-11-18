package com.tecgensoft.http.dtos.requests;

import com.tecgensoft.service.enums.BillingPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignSubscriptionRequest(
        @NotNull UUID packageId,
        @NotNull BillingPeriod billingPeriod,
        @NotNull Instant startsAt,
        Instant endsAt,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal agreedPrice
) { }
