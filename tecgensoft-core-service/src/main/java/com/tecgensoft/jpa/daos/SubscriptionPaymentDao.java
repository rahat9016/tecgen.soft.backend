package com.tecgensoft.jpa.daos;

import com.tecgensoft.common.utils.UuidV7Generator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="subscription_payments") @Getter @Setter @NoArgsConstructor
public class SubscriptionPaymentDao {
  @Id private UUID id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="subscription_id",nullable=false) private SubscriptionDao subscription;
  @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
  @Column(nullable=false,length=3) private String currency;
  @Column(name="payment_method",length=32) private String paymentMethod;
  @Column(name="payment_status",nullable=false,length=16) private String paymentStatus;
  @Column(name="transaction_id",length=160) private String transactionId;
  @Column(name="payment_date") private Instant paymentDate;
  @Column(name="period_start") private Instant periodStart;
  @Column(name="period_end") private Instant periodEnd;
  @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @PrePersist void create(){if(id==null)id=UuidV7Generator.generate();createdAt=Instant.now();updatedAt=createdAt;}
  @PreUpdate void update(){updatedAt=Instant.now();}
}
