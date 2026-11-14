package com.familiemunshi.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_packages")
public class ProductPackageDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private String code;
  private String displayName;
  private String product;
  private String description;
  private String currency;
  private java.math.BigDecimal price;
  private int version;
  private boolean active;
  private boolean availableForSale;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private java.util.Set<String> features;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private java.util.Map<String, Integer> limits;

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
