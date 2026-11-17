
package com.tecgensoft.service;


import com.tecgensoft.common.policy.PermissionPolicy;
import com.tecgensoft.common.utils.OrganizationAdminGenerator;
import com.tecgensoft.common.utils.Util;

import com.tecgensoft.http.validations.ValidationUtil;
import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.daos.PermissionDao;
import com.tecgensoft.jpa.daos.RoleDao;
import com.tecgensoft.jpa.daos.UserDao;
import com.tecgensoft.jpa.daos.UserOrganizationDao;
import com.tecgensoft.jpa.daos.UserOrganizationRoleDao;

import com.tecgensoft.jpa.repositories.PermissionRepository;
import com.tecgensoft.jpa.repositories.RoleRepository;
import com.tecgensoft.jpa.repositories.UserRepository;
import com.tecgensoft.jpa.repositories.UserOrganizationRepository;
import com.tecgensoft.jpa.repositories.UserOrganizationRoleRepository;

import com.tecgensoft.mapper.RoleMapper;
import com.tecgensoft.mapper.UserMapper;
import com.tecgensoft.service.entities.OrganizationAdminCredentials;
import com.tecgensoft.service.entities.Role;
import com.tecgensoft.service.entities.User;
import com.tecgensoft.service.enums.RoleScope;
import com.tecgensoft.service.exceptions.UserEmailAlreadyExistsException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationAdminProvisioner {

    private static final String ORGANIZATION_ADMIN_ROLE =
            "ORGANIZATION_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserOrganizationRoleRepository userOrganizationRoleRepository;

    @Value("${organization-admin.email-domain}")
    private String emailDomain;

    @Value("${organization-admin.common-password}")
    private String commonPassword;

    @Transactional(propagation = Propagation.MANDATORY)
    public OrganizationAdminCredentials provision(OrganizationDao organization) {

        // ======================================
        // 1. LOAD DEFAULT ORGANIZATION PERMISSIONS
        // ======================================

        List<String> permissionCodes =
                PermissionPolicy.getOrganizationPermissions();

        List<PermissionDao> permissions =
                permissionRepository.findAllByNameIn(
                        permissionCodes
                );

        Set<String> foundCodes = permissions.stream()
                .map(PermissionDao::getName)
                .collect(Collectors.toSet());

        if (!foundCodes.containsAll(permissionCodes)) {
            throw new IllegalStateException(
                    "Default Organization permissions are not fully seeded"
            );
        }

        // ======================================
        // 2. GENERATE ADMIN EMAIL
        // ======================================

        String email = OrganizationAdminGenerator.generateEmail(
                organization.getName(),
                organization.getCode(),
                emailDomain
        );

        if (userRepository.existsByEmail(email)) {
            throw new UserEmailAlreadyExistsException(email);
        }

        // ======================================
        // 3. VALIDATE COMMON PASSWORD
        // ======================================

        ValidationUtil.validateOrganizationAdminPassword(
                commonPassword
        );

        // ======================================
        // 4. HASH COMMON PASSWORD
        // ======================================

        String salt = Util.generateSalt();

        String hashedPassword = Util.hashPassword(
                commonPassword,
                salt
        );

        // ======================================
        // 5. CREATE FIRST ADMIN USER
        // ======================================

        String firstName = organization.getName().trim();

        if (firstName.length() > 120) {
            firstName = firstName.substring(0, 120);
        }

        User adminUser = User.builder()
                .firstName(firstName)
                .lastName("Admin")
                .email(email)
                .password(hashedPassword)
                .isActive(false)
                .build();

        UserDao adminDao = UserMapper.toDao(
                adminUser,
                salt
        );

        UserDao savedAdmin = userRepository.saveAndFlush(adminDao);

        // ======================================
        // 6. CREATE ORGANIZATION_ADMIN ROLE
        // ======================================

        Role adminRole = Role.builder()
                .name(ORGANIZATION_ADMIN_ROLE)
                .description(
                        "Administrator of Organization "
                                + organization.getCode()
                )
                .scope(RoleScope.ORGANIZATION)
                .isSystem(true)
                .isActive(true)
                .build();

        RoleDao roleDao = RoleMapper.toDao(
                adminRole,
                organization,
                new HashSet<>(permissions)
        );

        RoleDao savedRole =
                roleRepository.saveAndFlush(roleDao);

        // ======================================
        // 7. CREATE USER-ORGANIZATION MEMBERSHIP
        // ======================================

        UserOrganizationDao membership =
                UserOrganizationDao.builder()
                        .user(savedAdmin)
                        .organization(organization)
                        .build();

        userOrganizationRepository.saveAndFlush(membership);

        // ======================================
        // 8. ASSIGN ADMIN ROLE TO USER
        // ======================================

        UserOrganizationRoleDao assignment =
                UserOrganizationRoleDao.builder()
                        .user(savedAdmin)
                        .organization(organization)
                        .role(savedRole)
                        .build();

        userOrganizationRoleRepository.saveAndFlush(assignment);

        log.info(
                "Organization Admin provisioned. organizationId={}, userId={}",
                organization.getId(),
                savedAdmin.getId()
        );

        // ======================================
        // 9. RETURN NON-SENSITIVE INFORMATION
        // ======================================

        return new OrganizationAdminCredentials(
                savedAdmin.getId(),
                savedRole.getId(),
                savedAdmin.getEmail(),
                savedRole.getName(),
                savedAdmin.isActive()
        );
    }
}
