package com.tecgensoft.service.entities;

public record CreatedOrganizationResult(
        Organization organization,
        OrganizationAdminCredentials admin
) {}
