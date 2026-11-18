package com.tecgensoft.jpa.daos;

import com.tecgensoft.common.utils.UuidV7Generator;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.BillingPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "packages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_package_code_version",
                columnNames = {"code", "version"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PackageDao {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String code;

    @Column(nullable = false)
    private int version;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCode product;

    @Enumerated(EnumType.STRING)
    @Column(name="billing_period",nullable=false,length=20)
    private BillingPeriod billingPeriod=BillingPeriod.MONTHLY;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Set<String> features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> limits;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "available_for_sale", nullable = false)
    private boolean availableForSale = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UuidV7Generator.generate();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
