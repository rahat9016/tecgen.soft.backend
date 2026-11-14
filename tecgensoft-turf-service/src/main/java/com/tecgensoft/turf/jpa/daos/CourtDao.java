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
@Table(name = "courts")
public class CourtDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;
  private UUID branchId;
  private String name;
  private String sport;
  private String surface;
  private boolean indoor;
  private String size;
  private String imageUrl;
  private BigDecimal regularPrice;
  private BigDecimal peakPrice;
  private int players;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private java.util.Set<String> features;

  private String description;
  private boolean active;

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
