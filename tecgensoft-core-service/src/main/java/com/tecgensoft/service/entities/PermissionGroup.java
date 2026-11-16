package com.tecgensoft.service.entities;

import java.util.List;

public record PermissionGroup(
        String module,
        List<Permission> permissions
) {}
