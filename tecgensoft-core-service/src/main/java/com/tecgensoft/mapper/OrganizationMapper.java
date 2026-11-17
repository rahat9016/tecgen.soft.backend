package com.tecgensoft.mapper;

import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.service.entities.Organization;

public final class OrganizationMapper {
    private OrganizationMapper() { }

    public static Organization toEntity(OrganizationDao dao) {
        if (dao == null) return null;
        return new Organization(
                dao.getId(),
                dao.getName(),
                dao.getCode(),
                dao.getLogoUrl(),
                dao.getAddress(),
                dao.getPhone(),
                dao.getEmail(),
                dao.isActive(),
                dao.getCreatedAt(),
                dao.getUpdatedAt()
        );
    }
}
