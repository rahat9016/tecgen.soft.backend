package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.CreatedOrganization;

public record CreatedOrganizationResponse(
        OrganizationResponse organization,
        AdminResponse admin
) {
    public CreatedOrganizationResponse(CreatedOrganization result) {
        this(new OrganizationResponse(result.organization()), new AdminResponse(result.admin()));
    }
}
