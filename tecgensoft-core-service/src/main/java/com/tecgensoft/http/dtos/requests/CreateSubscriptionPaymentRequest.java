package com.tecgensoft.http.dtos.requests;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateSubscriptionPaymentRequest(
    @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal amount,
    @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
    @NotBlank String paymentMethod,
    @NotBlank @Pattern(regexp="PENDING|SUCCESS|FAILED") String paymentStatus,
    @Size(max=160) String transactionId,
    Instant paymentDate, Instant periodStart, Instant periodEnd) {}
