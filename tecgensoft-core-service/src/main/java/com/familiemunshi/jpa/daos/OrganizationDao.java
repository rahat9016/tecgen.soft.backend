package com.familiemunshi.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class OrganizationDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private String name;
  private boolean active;
  private UUID adminUserId;
  private UUID adminRoleId;

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
