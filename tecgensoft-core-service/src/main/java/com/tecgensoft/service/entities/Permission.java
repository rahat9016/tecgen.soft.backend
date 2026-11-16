package com.tecgensoft.service.entities;

import com.tecgensoft.service.enums.PermissionCode;
import lombok.Builder;
import lombok.With;

import java.time.Instant;
import java.util.UUID;

@Builder
@With
public record Permission(
        UUID id,
        PermissionCode code,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
