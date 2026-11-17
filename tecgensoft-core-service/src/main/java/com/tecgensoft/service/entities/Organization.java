package com.tecgensoft.service.entities;

import java.time.Instant;
import java.util.UUID;

public record Organization(
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
) { }
