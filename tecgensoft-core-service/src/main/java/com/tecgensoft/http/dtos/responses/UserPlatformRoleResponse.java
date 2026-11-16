package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.UserPlatformRole;

import java.util.UUID;

public record UserPlatformRoleResponse(
        UUID userId,
        UUID roleId
) {

    public UserPlatformRoleResponse(UserPlatformRole mapping) {
        this(
                mapping.userId(),
                mapping.roleId()
        );
    }
}