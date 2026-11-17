package com.tecgensoft.service;


import com.tecgensoft.common.policy.PermissionPolicy;
import com.tecgensoft.jpa.repositories.UserOrganizationRepository;
import com.tecgensoft.jpa.repositories.UserOrganizationRoleRepository;
import com.tecgensoft.jpa.repositories.UserPlatformRoleRepository;
import com.tecgensoft.service.enums.PermissionCode;
import com.tecgensoft.service.enums.RoleScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service("permissionAuthorizationService")
@RequiredArgsConstructor
public class PermissionAuthorizationService {
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserOrganizationRoleRepository userOrganizationRoleRepository;

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        UUID userId = getAuthenticatedUserId(authentication);

        if (userId == null) {
            return false;
        }

        if (permissionCode == null || !PermissionCode.getSupportedPermissions().contains(permissionCode)) {
            log.warn("Permission denied: unsupported code={}", permissionCode);
            return false;
        }

        return userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, permissionCode);
    }

    public boolean canCreateUser(Authentication authentication, UUID organizationId) {
        UUID userId = getAuthenticatedUserId(authentication);
        return userId != null && (
                userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, "USER_CREATE")
                        || (organizationId != null && hasOrganizationPermission(userId, organizationId, "USER_CREATE"))
        );
    }

    public boolean canReadPermissions(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return userId != null && (
                userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, "PERMISSION_READ")
                        || userOrganizationRoleRepository.hasAnyOrganizationPermission(
                                userId, RoleScope.ORGANIZATION, "PERMISSION_READ")
        );
    }

    public boolean canReadPlatformPermissions(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return userId != null
                && userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, "PERMISSION_READ");
    }


    public boolean canReadOrganization(Authentication authentication, UUID organizationId) {
        return hasOrganizationPermission(authentication, organizationId, "ORGANIZATION_READ");
    }

    public boolean hasOrganizationPermission(Authentication authentication, UUID organizationId, String permissionCode) {
        UUID userId = getAuthenticatedUserId(authentication);

        return hasOrganizationPermission(userId, organizationId, permissionCode);
    }

    public void requireOrganizationPermission(UUID userId, UUID organizationId, String permissionCode) {
        if (!hasOrganizationPermission(userId, organizationId, permissionCode)) {
            throw new AccessDeniedException("Permission denied for this organization");
        }
    }

    private boolean hasOrganizationPermission(UUID userId, UUID organizationId, String permissionCode) {
        if (userId == null || organizationId == null || permissionCode == null) {
            return false;
        }

        if (PermissionPolicy.isPlatformPermission(permissionCode) && userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, permissionCode)) {
            return true;
        }

        return PermissionPolicy.isOrganizationPermission(permissionCode) && userOrganizationRepository.existsByUser_IdAndOrganization_Id(userId, organizationId) && userOrganizationRoleRepository.hasOrganizationPermission(userId, organizationId, RoleScope.ORGANIZATION, permissionCode);
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principal = authentication.getName();

        if (principal == null) {
            return null;
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException exception) {
            log.debug("Permission denied: principal is not a valid UUID");
            return null;
        }
    }


    public void requirePlatformPermission(UUID userId, String permissionCode) {
        if (userId == null || permissionCode == null || !PermissionPolicy.isPlatformPermission(permissionCode)) {
            throw new AccessDeniedException("Platform permission required");
        }
        boolean allowed = userPlatformRoleRepository.hasPermission(userId, RoleScope.PLATFORM, permissionCode);

        if (!allowed) {
            throw new AccessDeniedException("You do not have the required platform permission");
        }
    }
}
