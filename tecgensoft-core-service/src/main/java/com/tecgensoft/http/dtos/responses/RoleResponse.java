package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.entities.Branch;
import com.tecgensoft.service.entities.Role;
import com.tecgensoft.service.enums.RoleScope;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public class RoleResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isSystem;
    private RoleScope scope;
    private Boolean isActive;
    private Branch branch;
    private Instant createdAt;
    private Instant updatedAt;

    public RoleResponse(Role role) {
        this.id = role.id();
        this.name = role.name();
        this.description = role.description();
        this.isSystem = role.isSystem();
        this.scope = role.scope();
        this.isActive = role.isActive();
        this.branch = role.branch();
        this.createdAt = role.createdAt();
        this.updatedAt = role.updatedAt();
    }
}
