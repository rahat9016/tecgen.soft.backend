package com.tecgensoft.turf.jpa.daos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class PaymentDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;

  private UUID bookingId;
  private BigDecimal amount;
  private String method;
  private String kind;
  private String reference;
  private String idempotencyKey;
  private UUID recordedBy;

  @PrePersist
  void create() {
    if (id == null) id = com.tecgensoft.turf.common.Ids.next();
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}
