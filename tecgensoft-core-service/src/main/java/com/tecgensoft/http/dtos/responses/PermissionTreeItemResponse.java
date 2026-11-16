package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.Permission;

import java.util.UUID;

public record PermissionTreeItemResponse(
        UUID id,
        String name,
        String description
) {
    public PermissionTreeItemResponse(Permission permission) {
        this(
                permission.id(),
                permission.code().getCode(),
                permission.code().getDescription()
        );
    }
}
