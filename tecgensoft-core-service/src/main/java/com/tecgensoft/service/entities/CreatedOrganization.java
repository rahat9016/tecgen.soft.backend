package com.tecgensoft.service.entities;

public record CreatedOrganization(
        Organization organization,
        OrganizationAdminCredentials admin
) {}
