package com.tecgensoft.jpa.daos;

import com.tecgensoft.common.utils.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_organizations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_organization",
                columnNames = {"user_id", "organization_id"}
        ),
        indexes = @Index(
                name = "idx_user_organizations_organization",
                columnList = "organization_id"
        )
)
public class UserOrganizationDao {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDao user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationDao organization;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UuidV7Generator.generate();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
