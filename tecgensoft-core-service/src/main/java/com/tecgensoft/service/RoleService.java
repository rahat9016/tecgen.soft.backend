package com.tecgensoft.service;

import com.tecgensoft.common.policy.PermissionPolicy;
import com.tecgensoft.common.utils.RoleNameUtil;
import com.tecgensoft.common.utils.Util;
import com.tecgensoft.jpa.daos.PermissionDao;
import com.tecgensoft.jpa.daos.RoleDao;
import com.tecgensoft.jpa.daos.UserPlatformRoleDao;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.repositories.*;
import com.tecgensoft.mapper.RoleMapper;
import com.tecgensoft.service.entities.Role;
import com.tecgensoft.service.entities.RolePermissions;
import com.tecgensoft.service.entities.UserPlatformRole;
import com.tecgensoft.service.exceptions.OrganizationNotFoundException;
import com.tecgensoft.service.enums.PermissionCode;
import com.tecgensoft.service.enums.RoleScope;
import com.tecgensoft.service.exceptions.*;
import com.tecgensoft.service.filters.FindAllRolesFilter;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final PermissionAuthorizationService permissionAuthorizationService;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRoleRepository userOrganizationRoleRepository;

    public Role createSystemRole(Role role) {
        Role newRole = role
                .withScope(RoleScope.PLATFORM)
                .withName(RoleNameUtil.platformRole(role.name()))
                .withIsSystem(false)
                .withIsActive(true);
        if(roleRepository.existsByNameIgnoreCaseAndScope(newRole.name(), newRole.scope())){
            throw new RoleAlreadyExistsException(newRole.name());
        }
        // MAP THE ROLE ENTITY TO DAO
        RoleDao roleMapper = RoleMapper.toDao(newRole);

        // SAVE THE ROLE DAO TO THE DATABASE
        RoleDao savedRole = roleRepository.save(roleMapper);

        return RoleMapper.toEntity(savedRole);
    }

    @Transactional
    public Role createOrganizationRole(UUID callerId, UUID organizationId, Role role) {
        permissionAuthorizationService.requireOrganizationPermission(callerId, organizationId, "ROLE_CREATE");
        OrganizationDao organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        String name = role.name().trim();
        if (name.isBlank() || roleRepository.existsByNameIgnoreCaseAndScopeAndOrganization_Id(
                name, RoleScope.ORGANIZATION, organizationId)) {
            throw new RoleAlreadyExistsException(name);
        }
        Role organizationRole = role.withName(name).withScope(RoleScope.ORGANIZATION)
                .withOrganization(com.tecgensoft.mapper.OrganizationMapper.toEntity(organization))
                .withIsSystem(false).withIsActive(true);
        RoleDao dao = RoleMapper.toDao(organizationRole);
        dao.setOrganization(organization);
        return RoleMapper.toEntity(roleRepository.saveAndFlush(dao));
    }

    @Transactional
    public java.util.List<Role> getOrganizationRoles(UUID callerId, UUID organizationId) {
        permissionAuthorizationService.requireOrganizationPermission(callerId, organizationId, "ROLE_READ");
        return roleRepository.findByOrganization_IdAndScopeOrderByNameAsc(organizationId, RoleScope.ORGANIZATION)
                .stream().map(RoleMapper::toEntity).toList();
    }

    @Transactional
    public RolePermissions assignOrganizationRolePermissions(UUID callerId, UUID organizationId, RolePermissions assignment) {
        permissionAuthorizationService.requireOrganizationPermission(callerId, organizationId, "ROLE_PERMISSION_MANAGE");
        RoleDao role = roleRepository.findByIdAndOrganization_IdAndScope(
                        assignment.roleId(), organizationId, RoleScope.ORGANIZATION)
                .orElseThrow(() -> new RoleNotFoundException(assignment.roleId()));
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new InvalidRolePermissionAssignmentException("Cannot modify a built-in organization role");
        }
        Set<String> requested = assignment.permissionCodes();
        for (String code : requested) {
            if (!PermissionCode.isSuppoerted(code) || !PermissionPolicy.isOrganizationPermission(code)) {
                throw new InvalidRolePermissionAssignmentException(code + " is not an organization permission");
            }
        }
        Set<String> callerPermissions = new HashSet<>(userOrganizationRoleRepository
                .findEffectivePermissionCodes(callerId, organizationId, RoleScope.ORGANIZATION));
        if (!callerPermissions.containsAll(requested)) {
            throw new PermissionDeniedException("You cannot assign permissions beyond your organization role");
        }
        var permissions = permissionRepository.findAllByNameIn(requested);
        if (permissions.size() != requested.size()) {
            throw new PermissionNotFoundException(String.join(", ", requested));
        }
        role.getPermissions().addAll(permissions);
        RoleDao saved = roleRepository.saveAndFlush(role);
        return new RolePermissions(saved.getId(), saved.getPermissions().stream()
                .map(PermissionDao::getName).collect(Collectors.toSet()));
    }

    public Role getRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .map(RoleMapper::toEntity)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
    }

    public Page<Role> getAllRolesByFilter(
            FindAllRolesFilter filter,
            Pageable pageable
    ) {

        String searchPattern = Util.toSearchPattern(filter.getSearch());

        boolean filterActive = filter.getIsActive() != null;
        boolean activeValue = Boolean.TRUE.equals(filter.getIsActive());

        boolean filterSystem = filter.getIsSystem() != null;
        boolean systemValue = Boolean.TRUE.equals(filter.getIsSystem());

        Page<RoleDao> roles = roleRepository.findAllByFilters(
                RoleScope.PLATFORM,
                searchPattern,
                filterActive,
                activeValue,
                filterSystem,
                systemValue,
                pageable
        );

        return roles.map(RoleMapper::toEntity);
    }

    @Transactional
    public UserPlatformRole assignUserPlatformRole(
            UUID callerId,
            UserPlatformRole mapping
    ) {
        permissionAuthorizationService.requirePlatformPermission(
                callerId,
                "USER_ROLE_ASSIGN"
        );

        RoleDao roleDao = roleRepository.findPlatformRoleByIdForUpdate(
                        mapping.roleId(),
                        RoleScope.PLATFORM
                )
                .orElseThrow(() ->
                        new RoleNotFoundException(mapping.roleId())
                );

        if (!Boolean.TRUE.equals(roleDao.getIsActive())) {
            throw new PermissionDeniedException(
                    "Cannot assign an inactive platform role"
            );
        }

        requireRoleWithinCallerAccess(callerId, roleDao);

        var userDao = userRepository.findById(mapping.userId())
                .orElseThrow(() ->
                        new UserNotFoundException(mapping.userId())
                );

        if (!userDao.isActive()) {
            throw new PermissionDeniedException(
                    "Cannot assign a platform role to an inactive user"
            );
        }

        if (userPlatformRoleRepository.existsByUser_IdAndRole_Id(
                mapping.userId(),
                mapping.roleId()
        )) {
            return mapping;
        }

        UserPlatformRoleDao assignment = UserPlatformRoleDao.builder()
                .user(userDao)
                .role(roleDao)
                .build();

        userPlatformRoleRepository.saveAndFlush(assignment);

        return mapping;
    }

    @Transactional
    public UserPlatformRole removeUserPlatformRole(
            UUID callerId,
            UserPlatformRole mapping
    ) {
        permissionAuthorizationService.requirePlatformPermission(
                callerId,
                "USER_ROLE_REMOVE"
        );

        RoleDao roleDao = roleRepository.findPlatformRoleByIdForUpdate(
                        mapping.roleId(),
                        RoleScope.PLATFORM
                )
                .orElseThrow(() ->
                        new RoleNotFoundException(mapping.roleId())
                );

        /*
         * Protect seeded system-role assignments.
         * Super Admin lifecycle needs a separate controlled process.
         */
        if (Boolean.TRUE.equals(roleDao.getIsSystem())) {
            throw new PermissionDeniedException(
                    "Built-in system role assignments cannot be removed through this API"
            );
        }

        requireRoleWithinCallerAccess(callerId, roleDao);

        userRepository.findById(mapping.userId())
                .orElseThrow(() ->
                        new UserNotFoundException(mapping.userId())
                );

        userPlatformRoleRepository.findByUser_IdAndRole_Id(
                mapping.userId(),
                mapping.roleId()
        ).ifPresent(assignment -> {
            userPlatformRoleRepository.delete(assignment);
            userPlatformRoleRepository.flush();
        });

        return mapping;
    }

    private void requireRoleWithinCallerAccess(
            UUID callerId,
            RoleDao roleDao
    ) {
        Set<String> callerPermissions = new HashSet<>(
                userPlatformRoleRepository.findEffectivePermissionCodes(
                        callerId,
                        RoleScope.PLATFORM
                )
        );

        Set<String> rolePermissions = roleDao.getPermissions()
                .stream()
                .map(PermissionDao::getName)
                .collect(Collectors.toSet());

        if (!callerPermissions.containsAll(rolePermissions)) {
            throw new PermissionDeniedException(
                    "You cannot manage assignments for a role beyond your own access"
            );
        }
    }

    @Transactional
    public RolePermissions assignPlatformRolePermissions(
            UUID userId,
            RolePermissions rolePermissions
    ) {
        // CHECK CALLER'S PLATFORM PERMISSION
        permissionAuthorizationService.requirePlatformPermission(
                userId,
                "ROLE_PERMISSION_MANAGE"
        );

        // FIND AND LOCK THE TARGET PLATFORM ROLE
        RoleDao roleDao =
                roleRepository.findPlatformRoleByIdForUpdate(
                                rolePermissions.roleId(),
                                RoleScope.PLATFORM
                        )
                        .orElseThrow(() ->
                                new RoleNotFoundException(
                                        rolePermissions.roleId()
                                )
                        );

        // PROTECT BUILT-IN SYSTEM ROLES
        if (Boolean.TRUE.equals(roleDao.getIsSystem())) {
            throw new InvalidRolePermissionAssignmentException(
                    "Cannot assign permissions to a built-in system role"
            );
        }

        if (!Boolean.TRUE.equals(roleDao.getIsActive())) {
            throw new InvalidRolePermissionAssignmentException(
                    "Target platform role is inactive"
            );
        }

        Set<String> requestedCodes = rolePermissions.permissionCodes();

        // VALIDATE REQUESTED PERMISSIONS
        for (String permissionCode : requestedCodes) {
            if (!PermissionCode.isSuppoerted(permissionCode)) {
                throw new InvalidPermissionCodeException(
                        permissionCode
                );
            }

            if (!PermissionPolicy.isPlatformPermission(permissionCode)) {
                throw new InvalidRolePermissionAssignmentException(
                        permissionCode + " is not a platform permission"
                );
            }
        }

        // LOAD CALLER'S EFFECTIVE PLATFORM PERMISSIONS
        Set<String> callerPermissions = new HashSet<>(
                userPlatformRoleRepository.findEffectivePermissionCodes(
                        userId,
                        RoleScope.PLATFORM
                )
        );

        if (!callerPermissions.containsAll(requestedCodes)) {
            throw new PermissionDeniedException(
                    "You cannot assign permissions beyond your own access"
            );
        }

        // LOAD SEEDED PERMISSION RECORDS
        var permissions =
                permissionRepository.findAllByNameIn(requestedCodes);

        if (permissions.size() != requestedCodes.size()) {
            throw new PermissionNotFoundException(
                    String.join(", ", requestedCodes)
            );
        }

        // ADD TO EXISTING PERMISSIONS
        roleDao.getPermissions().addAll(permissions);

        RoleDao savedRole = roleRepository.saveAndFlush(roleDao);

        Set<String> assignedCodes =
                savedRole.getPermissions()
                        .stream()
                        .map(PermissionDao::getName)
                        .collect(Collectors.toSet());

        return new RolePermissions(
                savedRole.getId(),
                assignedCodes
        );
    }

}
