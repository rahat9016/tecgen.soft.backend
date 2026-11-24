package com.tecgensoft.mapper;

import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.service.entities.SubscriptionPlan;

import java.math.RoundingMode;

public final class PackageMapper {
    private PackageMapper() { }

    public static PackageDao toDao(SubscriptionPlan plan, int version) {
        PackageDao dao = new PackageDao();
        dao.setCode(plan.code());
        dao.setVersion(version);
        dao.setDisplayName(plan.displayName());
        dao.setProduct(plan.product());
        dao.setBillingPeriod(plan.billingPeriod());
        dao.setDescription(plan.description());
        dao.setCurrency(plan.currency());
        dao.setPrice(plan.price().setScale(2, RoundingMode.UNNECESSARY));
        dao.setFeatures(plan.features());
        dao.setLimits(plan.limits());
        dao.setActive(plan.active());
        dao.setAvailableForSale(plan.availableForSale());
        return dao;
    }

    public static SubscriptionPlan toEntity(PackageDao dao) {
        return SubscriptionPlan.builder()
                .id(dao.getId())
                .code(dao.getCode())
                .version(dao.getVersion())
                .displayName(dao.getDisplayName())
                .product(dao.getProduct())
                .billingPeriod(dao.getBillingPeriod())
                .description(dao.getDescription())
                .currency(dao.getCurrency())
                .price(dao.getPrice())
                .features(dao.getFeatures())
                .limits(dao.getLimits())
                .active(dao.isActive())
                .availableForSale(dao.isAvailableForSale())
                .createdAt(dao.getCreatedAt())
                .updatedAt(dao.getUpdatedAt())
                .build();
    }
}
