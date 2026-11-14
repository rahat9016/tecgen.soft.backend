package com.familiemunshi.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class SubscriptionDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;
  private UUID packageId;
  private String product;
  private String status;
  private String billingPeriod;
  private Instant startsAt;
  private Instant endsAt;
  private java.math.BigDecimal agreedPrice;
  private String currency;

  @PrePersist
  void create() {
    if (id == null) id = com.familiemunshi.common.utils.UuidV7Generator.generate();
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}
