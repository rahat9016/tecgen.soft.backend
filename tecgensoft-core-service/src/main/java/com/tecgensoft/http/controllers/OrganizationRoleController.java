package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.AssignRolePermissionsRequest;
import com.tecgensoft.http.dtos.requests.CreateRoleRequest;
import com.tecgensoft.http.dtos.responses.*;
import com.tecgensoft.service.PermissionService;
import com.tecgensoft.service.RoleService;
import com.tecgensoft.service.entities.Role;
import com.tecgensoft.service.entities.RolePermissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.tecgensoft.common.utils.Constants.API_BASE;

@RestController
@RequestMapping(API_BASE + "/organizations/{organizationId}/roles")
@RequiredArgsConstructor
public class OrganizationRoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_CREATE')")
    public ResponseEntity<SuccessResponse<RoleResponse>> createRole(
            Authentication authentication,
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateRoleRequest request) {
        Role created = roleService.createOrganizationRole(
                UUID.fromString(authentication.getName()), organizationId, request.toEntity());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.of("Organization role created successfully", new RoleResponse(created)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_READ')")
    public ResponseEntity<SuccessResponse<List<RoleResponse>>> getRoles(
            Authentication authentication,
            @PathVariable UUID organizationId) {
        List<RoleResponse> roles = roleService.getOrganizationRoles(
                        UUID.fromString(authentication.getName()), organizationId)
                .stream().map(RoleResponse::new).toList();
        return ResponseEntity.ok(SuccessResponse.of("Organization roles retrieved successfully", roles));
    }

    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_PERMISSION_MANAGE')")
    public ResponseEntity<SuccessResponse<RolePermissionsResponse>> assignPermissions(
            Authentication authentication,
            @PathVariable UUID organizationId,
            @PathVariable UUID roleId,
            @Valid @RequestBody AssignRolePermissionsRequest request) {
        RolePermissions saved = roleService.assignOrganizationRolePermissions(
                UUID.fromString(authentication.getName()), organizationId, request.toEntity(roleId));
        List<ModulePermissionTreeResponse> tree = permissionService.toModulePermissionTree(
                saved.permissionCodes().stream().toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(SuccessResponse.of(
                "Organization role permissions assigned successfully",
                new RolePermissionsResponse(saved.roleId(), tree)));
    }
}
