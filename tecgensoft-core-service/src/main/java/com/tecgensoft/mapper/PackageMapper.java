package com.tecgensoft.mapper;

import com.tecgensoft.jpa.daos.PackageDao;
import com.tecgensoft.service.entities.SoftwarePackage;

public final class PackageMapper {
    private PackageMapper() { }

    public static SoftwarePackage toEntity(PackageDao dao) {
        return new SoftwarePackage(
                dao.getId(), dao.getCode(), dao.getVersion(), dao.getDisplayName(), dao.getProduct(), dao.getBillingPeriod(),
                dao.getDescription(), dao.getCurrency(), dao.getPrice(), dao.getFeatures(), dao.getLimits(),
                dao.isActive(), dao.isAvailableForSale(), dao.getCreatedAt(), dao.getUpdatedAt()
        );
    }
}
