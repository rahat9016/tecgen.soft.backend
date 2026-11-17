package com.tecgensoft.http.dtos.responses;

import com.tecgensoft.service.enums.RoleScope;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyPermissionsResponse(
        UUID userId,
        RoleScope scope,
        UUID organizationId,
        String organizationCode,
        String organizationName,
        List<String> roles,
        List<ModulePermissionTreeResponse> permissions,
        List<OrganizationPermissionResponse> organizations
) {

    public static MyPermissionsResponse platform(
            UUID userId,
            List<String> roles,
            List<ModulePermissionTreeResponse> permissions
    ) {
        return new MyPermissionsResponse(
                userId,
                RoleScope.PLATFORM,
                null,
                null,
                null,
                roles,
                permissions,
                null
        );
    }

    public static MyPermissionsResponse organization(
            UUID userId,
            OrganizationPermissionResponse organization
    ) {
        return new MyPermissionsResponse(
                userId,
                RoleScope.ORGANIZATION,
                organization.organizationId(),
                organization.organizationCode(),
                organization.organizationName(),
                organization.roles(),
                organization.permissions(),
                null
        );
    }

    public static MyPermissionsResponse multipleOrganizations(
            UUID userId,
            List<OrganizationPermissionResponse> organizations
    ) {
        return new MyPermissionsResponse(
                userId,
                RoleScope.ORGANIZATION,
                null,
                null,
                null,
                null,
                null,
                organizations
        );
    }
}
