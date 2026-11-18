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

    public static OrganizationDao toDao(Organization organization) {
        OrganizationDao dao = new OrganizationDao(
                organization.name(),
                organization.code(),
                organization.address(),
                organization.phone(),
                organization.email()
        );
        dao.setId(organization.id());
        dao.setLogoUrl(organization.logoUrl());
        dao.setActive(organization.isActive());
        if (organization.createdAt() != null) dao.setCreatedAt(organization.createdAt());
        if (organization.updatedAt() != null) dao.setUpdatedAt(organization.updatedAt());
        return dao;
    }
}
