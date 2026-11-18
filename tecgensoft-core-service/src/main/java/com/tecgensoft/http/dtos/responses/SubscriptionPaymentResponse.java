package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.jpa.daos.SubscriptionPaymentDao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionPaymentResponse(UUID id,UUID subscriptionId,BigDecimal amount,String currency,
    String paymentMethod,String paymentStatus,String transactionId,Instant paymentDate,
    Instant periodStart,Instant periodEnd,Instant createdAt) {
  public SubscriptionPaymentResponse(SubscriptionPaymentDao d){this(d.getId(),d.getSubscription().getId(),d.getAmount(),d.getCurrency(),d.getPaymentMethod(),d.getPaymentStatus(),d.getTransactionId(),d.getPaymentDate(),d.getPeriodStart(),d.getPeriodEnd(),d.getCreatedAt());}
}
