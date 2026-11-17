package com.tecgensoft.turf.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reservations")
public class ReservationDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;

  private UUID courtId;
  private Instant startAt;
  private Instant endAt;
  private String kind;
  private String status;
  private Instant expiresAt;
  private String secretHash;
  private String reason;

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
