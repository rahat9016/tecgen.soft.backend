package com.tecgensoft.service.entities;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.With;

@Builder
@With
public record Organization(
        UUID id,
        String name,
        String code,
        String logoUrl,
        String address,
        String phone,
        String email,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) { }
