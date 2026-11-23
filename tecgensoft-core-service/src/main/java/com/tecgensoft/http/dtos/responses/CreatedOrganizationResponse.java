package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.CreatedOrganization;

public record CreatedOrganizationResponse(
        OrganizationResponse organization,
        AdminResponse admin,
        SubscriptionResponse subscription
) {
    public CreatedOrganizationResponse(CreatedOrganization result) {
        this(
                new OrganizationResponse(result.organization()),
                new AdminResponse(result.admin()),
                new SubscriptionResponse(result.subscription())
        );
    }
}
