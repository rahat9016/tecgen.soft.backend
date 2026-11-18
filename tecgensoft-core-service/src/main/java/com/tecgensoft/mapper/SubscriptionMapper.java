package com.tecgensoft.mapper;

import com.tecgensoft.jpa.daos.SubscriptionDao;
import com.tecgensoft.service.entities.Subscription;

public final class SubscriptionMapper {
    private SubscriptionMapper() { }

    public static Subscription toEntity(SubscriptionDao dao) {
        return new Subscription(
                dao.getId(), dao.getOrganization().getId(), dao.getPackageDao().getId(),
                dao.getPackageDao().getCode(), dao.getPackageDao().getVersion(), dao.getProduct(),
                dao.getStatus(), dao.getBillingPeriod(), dao.getStartsAt(), dao.getEndsAt(),
                dao.getAgreedPrice(), dao.getCreatedAt(), dao.isTrial(), dao.getTrialStartDate(),
                dao.getTrialEndDate(), dao.getActivatedAt(), dao.getCancelledAt(), dao.getUpdatedAt()
        );
    }
}
