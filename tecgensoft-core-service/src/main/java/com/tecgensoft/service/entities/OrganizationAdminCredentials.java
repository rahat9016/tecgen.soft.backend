package com.tecgensoft.service.entities;

import java.util.UUID;

public record OrganizationAdminCredentials(
        UUID userId,
        UUID roleId,
        String email,
        String role,
        boolean active
) {}
