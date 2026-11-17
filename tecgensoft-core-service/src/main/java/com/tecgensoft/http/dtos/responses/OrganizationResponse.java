package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.Organization;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String code,
        String logoUrl,
        String address,
        String phone,
        String email,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public OrganizationResponse(Organization organization) {
        this(
                organization.id(),
                organization.name(),
                organization.code(),
                organization.logoUrl(),
                organization.address(),
                organization.phone(),
                organization.email(),
                organization.isActive(),
                organization.createdAt(),
                organization.updatedAt()
        );
    }
}
