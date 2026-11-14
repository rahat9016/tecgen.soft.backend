package com.familiemunshi.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_assignment_audit")
public class AdminAssignmentAuditDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;
  private UUID userId;
  private UUID actorId;

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
