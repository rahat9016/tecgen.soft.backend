package com.tecgensoft.mapper;

import com.tecgensoft.jpa.daos.SubscriptionDao;
import com.tecgensoft.service.entities.Subscription;

public final class SubscriptionMapper {
    private SubscriptionMapper() { }

    public static Subscription toEntity(SubscriptionDao dao) {
        return Subscription.builder()
                .id(dao.getId())
                .organizationId(dao.getOrganization().getId())
                .packageId(dao.getPackageDao().getId())
                .packageCode(dao.getPackageDao().getCode())
                .packageVersion(dao.getPackageDao().getVersion())
                .product(dao.getProduct())
                .status(dao.getStatus())
                .billingPeriod(dao.getBillingPeriod())
                .startsAt(dao.getStartsAt())
                .endsAt(dao.getEndsAt())
                .agreedPrice(dao.getAgreedPrice())
                .createdAt(dao.getCreatedAt())
                .isTrial(dao.isTrial())
                .trialStartDate(dao.getTrialStartDate())
                .trialEndDate(dao.getTrialEndDate())
                .activatedAt(dao.getActivatedAt())
                .cancelledAt(dao.getCancelledAt())
                .updatedAt(dao.getUpdatedAt())
                .graceDays(dao.getGraceDays())
                .build();
    }
}
