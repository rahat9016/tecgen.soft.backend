package com.tecgensoft.http.controllers;

import com.tecgensoft.http.dtos.requests.AssignRolePermissionsRequest;
import com.tecgensoft.http.dtos.requests.CreateRoleRequest;
import com.tecgensoft.http.dtos.responses.*;
import com.tecgensoft.service.PermissionService;
import com.tecgensoft.service.RoleService;
import com.tecgensoft.service.entities.Role;
import com.tecgensoft.service.entities.RolePermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.tecgensoft.common.utils.Constants.*;

@Tag(name = "Organization Roles", description = "APIs for managing organization-scoped roles")
@RestController
@RequestMapping(API_BASE + ORGANIZATIONS + ORGANIZATION_ID + ROLES)
@RequiredArgsConstructor
public class OrganizationRoleController {
    private final RoleService roleService;
    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_CREATE')")
    @Operation(
            summary = "Create an organization role.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Role created."),
                    @ApiResponse(responseCode = "400", description = "Invalid input."),
                    @ApiResponse(responseCode = "403", description = "ROLE_CREATE permission required.")
            }
    )
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
    @Operation(
            summary = "List organization roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Roles retrieved."),
                    @ApiResponse(responseCode = "403", description = "ROLE_READ permission required.")
            }
    )
    public ResponseEntity<SuccessResponse<List<RoleResponse>>> getRoles(
            Authentication authentication,
            @PathVariable UUID organizationId) {
        List<RoleResponse> roles = roleService.getOrganizationRoles(
                        UUID.fromString(authentication.getName()), organizationId)
                .stream().map(RoleResponse::new).toList();
        return ResponseEntity.ok(SuccessResponse.of("Organization roles retrieved successfully", roles));
    }

    @GetMapping(ROLE_ID)
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_READ')")
    @Operation(
            summary = "Get an organization role by id.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role retrieved."),
                    @ApiResponse(responseCode = "403", description = "ROLE_READ permission required."),
                    @ApiResponse(responseCode = "404", description = "Role not found.")
            }
    )
    public ResponseEntity<SuccessResponse<RoleResponse>> getRole(
            Authentication authentication,
            @PathVariable UUID organizationId,
            @PathVariable UUID roleId) {
        Role role = roleService.getOrganizationRoleById(
                UUID.fromString(authentication.getName()), organizationId, roleId);
        return ResponseEntity.ok(SuccessResponse.of("Organization role retrieved successfully", new RoleResponse(role)));
    }

    @PostMapping(ROLE_ID + PERMISSIONS)
    @PreAuthorize("@permissionAuthorizationService.hasOrganizationPermission(authentication, #organizationId, 'ROLE_PERMISSION_MANAGE')")
    @Operation(
            summary = "Assign permissions to an organization role.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Permissions assigned."),
                    @ApiResponse(responseCode = "400", description = "Invalid permission assignment."),
                    @ApiResponse(responseCode = "403", description = "Permission denied."),
                    @ApiResponse(responseCode = "404", description = "Role or permission not found.")
            }
    )
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
