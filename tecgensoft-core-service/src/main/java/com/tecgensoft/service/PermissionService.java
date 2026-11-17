package com.tecgensoft.service;

import com.tecgensoft.common.policy.PermissionPolicy;
import com.tecgensoft.common.utils.Util;
import com.tecgensoft.http.dtos.responses.OrganizationPermissionResponse;
import com.tecgensoft.http.dtos.responses.ModulePermissionTreeResponse;
import com.tecgensoft.http.dtos.responses.MyPermissionsResponse;
import com.tecgensoft.http.dtos.responses.PermissionTreeItemResponse;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.daos.PermissionDao;
import com.tecgensoft.jpa.repositories.PermissionRepository;
import com.tecgensoft.jpa.repositories.UserOrganizationRoleRepository;
import com.tecgensoft.jpa.repositories.UserPlatformRoleRepository;
import com.tecgensoft.jpa.repositories.UserRepository;
import com.tecgensoft.mapper.PermissionMapper;
import com.tecgensoft.service.entities.Permission;
import com.tecgensoft.service.entities.PermissionGroup;
import com.tecgensoft.service.enums.PermissionCode;
import com.tecgensoft.service.enums.RoleScope;
import com.tecgensoft.service.exceptions.PermissionNotFoundException;
import com.tecgensoft.service.exceptions.UserNotFoundException;
import com.tecgensoft.service.filters.FindAllPermissionsFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final UserOrganizationRoleRepository userOrganizationRoleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public Page<Permission> getAllPermissionsByFilter(
            FindAllPermissionsFilter filter,
            Pageable pageable,
            boolean organizationOnly
    ) {
        String searchPattern = Util.toSearchPattern(filter.getSearch());

        String module = filter.getModule() == null
                ? null
                : filter.getModule().trim().toUpperCase(Locale.ROOT);

        List<String> permissionCodes = Arrays.stream(PermissionCode.values())
                .filter(permission -> module == null
                        || module.isBlank()
                        || permission.getModule().equals(module))
                .filter(permission -> !organizationOnly
                        || PermissionPolicy.isOrganizationPermission(permission.getCode()))
                .map(PermissionCode::getCode)
                .toList();

        if (permissionCodes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid permission module: " + module
            );
        }

        return permissionRepository.findAllByFilters(
                        permissionCodes,
                        searchPattern,
                        pageable
                )
                .map(PermissionMapper::toEntity);
    }

    @Transactional(readOnly = true)
    public List<PermissionGroup> getPermissionTree(boolean organizationOnly) {
        List<Permission> permissions = permissionRepository.findAll()
                .stream()
                .map(PermissionMapper::toEntity)
                .filter(permission -> !organizationOnly
                        || PermissionPolicy.isOrganizationPermission(permission.code().getCode()))
                .toList();

        return groupPermissions(permissions);
    }

    @Transactional(readOnly = true)
    public Permission getPermissionById(UUID id, boolean organizationOnly) {
        return permissionRepository.findById(id)
                .map(PermissionMapper::toEntity)
                .filter(permission -> !organizationOnly
                        || PermissionPolicy.isOrganizationPermission(permission.code().getCode()))
                .orElseThrow(() -> new PermissionNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public MyPermissionsResponse getMyPermissions(UUID userId) {
        if (!userRepository.existsByIdAndIsActiveTrue(userId)) {
            throw new UserNotFoundException(userId.toString());
        }

        List<String> platformRoles =
                userPlatformRoleRepository.findEffectiveRoleNames(
                        userId,
                        RoleScope.PLATFORM
                );

        if (!platformRoles.isEmpty()) {
            List<String> permissionCodes = userPlatformRoleRepository
                    .findEffectivePermissionCodes(userId, RoleScope.PLATFORM)
                    .stream()
                    .filter(PermissionPolicy::isPlatformPermission)
                    .distinct()
                    .toList();

            return MyPermissionsResponse.platform(
                    userId,
                    platformRoles,
                    toModulePermissionTree(permissionCodes)
            );
        }

        List<OrganizationDao> organizations =
                userOrganizationRoleRepository.findAuthorizedOrganizations(
                        userId,
                        RoleScope.ORGANIZATION
                );

        if (organizations.isEmpty()) {
            throw new AccessDeniedException("No active role assignment found");
        }

        List<OrganizationPermissionResponse> organizationPermissions = organizations.stream()
                .map(organization -> getOrganizationPermissions(userId, organization))
                .toList();

        if (organizationPermissions.size() == 1) {
            return MyPermissionsResponse.organization(
                    userId,
                    organizationPermissions.getFirst()
            );
        }

        return MyPermissionsResponse.multipleOrganizations(
                userId,
                organizationPermissions
        );
    }

    private OrganizationPermissionResponse getOrganizationPermissions(
            UUID userId,
            OrganizationDao organization
    ) {
        UUID organizationId = organization.getId();

        List<String> roles = userOrganizationRoleRepository
                .findRoleNames(userId, organizationId, RoleScope.ORGANIZATION)
                .stream()
                .distinct()
                .sorted()
                .toList();

        List<String> permissionCodes = userOrganizationRoleRepository
                .findEffectivePermissionCodes(userId, organizationId, RoleScope.ORGANIZATION)
                .stream()
                .filter(PermissionPolicy::isOrganizationPermission)
                .distinct()
                .toList();

        return new OrganizationPermissionResponse(
                organizationId,
                organization.getCode(),
                organization.getName(),
                roles,
                toModulePermissionTree(permissionCodes)
        );
    }

    public List<ModulePermissionTreeResponse> toModulePermissionTree(
            List<String> permissionCodes
    ) {
        if (permissionCodes.isEmpty()) {
            return List.of();
        }

        List<Permission> permissions =
                permissionRepository.findAllByNameIn(permissionCodes)
                        .stream()
                        .map(PermissionMapper::toEntity)
                        .toList();

        return groupPermissions(permissions).stream()
                .map(group -> new ModulePermissionTreeResponse(
                        group.module(),
                        group.permissions().stream()
                                .map(PermissionTreeItemResponse::new)
                                .toList()
                ))
                .toList();
    }

    private List<PermissionGroup> groupPermissions(List<Permission> permissions) {
        var permissionsByModule = permissions.stream()
                .collect(Collectors.groupingBy(
                        permission -> permission.code().getModule(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        return permissionsByModule.entrySet().stream()
                .map(entry -> new PermissionGroup(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(
                                        permission -> permission.code().getCode()
                                ))
                                .toList()
                ))
                .toList();
    }
}
