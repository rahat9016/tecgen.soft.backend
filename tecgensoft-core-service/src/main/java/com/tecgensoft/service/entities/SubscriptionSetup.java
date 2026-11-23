package com.tecgensoft.service.entities;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record SubscriptionSetup(
        UUID packageId,
        boolean isFreeTrial,
        Instant startsAt,
        Instant endsAt,
        BigDecimal amountPaid,
        int graceDays
) { }
