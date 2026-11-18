package com.tecgensoft.jpa.daos;

import com.tecgensoft.common.utils.UuidV7Generator;
import com.tecgensoft.service.enums.BillingPeriod;
import com.tecgensoft.service.enums.ProductCode;
import com.tecgensoft.service.enums.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionDao {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationDao organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private PackageDao packageDao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCode product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "agreed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal agreedPrice;

    @Column(name = "is_trial", nullable = false)
    private boolean isTrial;
    @Column(name = "trial_start_date") private Instant trialStartDate;
    @Column(name = "trial_end_date") private Instant trialEndDate;
    @Column(name = "activated_at") private Instant activatedAt;
    @Column(name = "cancelled_at") private Instant cancelledAt;

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

    @PreUpdate protected void onUpdate() { updatedAt=Instant.now(); }
}
