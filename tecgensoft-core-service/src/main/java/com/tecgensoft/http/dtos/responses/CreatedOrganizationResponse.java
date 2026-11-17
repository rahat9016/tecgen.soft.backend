package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.CreatedOrganizationResult;

public record CreatedOrganizationResponse(
        OrganizationResponse organization,
        AdminResponse admin
) {
    public CreatedOrganizationResponse(CreatedOrganizationResult result) {
        this(new OrganizationResponse(result.organization()), new AdminResponse(result.admin()));
    }
}
