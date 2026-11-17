package com.tecgensoft.service;


import com.tecgensoft.common.service.FileStorageService;
import com.tecgensoft.common.utils.Util;
import com.tecgensoft.http.validations.ValidationUtil;
import com.tecgensoft.jpa.daos.*;
import com.tecgensoft.jpa.repositories.*;
import com.tecgensoft.mapper.UserMapper;
import com.tecgensoft.service.entities.User;
import com.tecgensoft.service.enums.RoleScope;
import com.tecgensoft.service.exceptions.*;
import com.tecgensoft.service.filters.FindAllUsersFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionAuthorizationService permissionAuthorizationService;
    private final FileStorageService fileStorageService;
    private final UserPlatformRoleRepository userPlatformRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserOrganizationRoleRepository userOrganizationRoleRepository;
    private final RoleService roleService;

    @Transactional
    public User createUserWithRole(
            UUID callerId,
            User user,
            UUID roleId,
            UUID organizationId,
            MultipartFile profilePicture
    ) {

        // 1. NORMALIZE EMAIL
        String email = user.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 3. CHECK DUPLICATE EMAIL
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserEmailAlreadyExistsException(email);
        }

        // 4. FIND ROLE
        RoleDao role = roleRepository.findById(roleId)
                .orElseThrow(() ->
                        new RoleNotFoundException(roleId)
                );

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new PermissionDeniedException("Inactive role cannot be assigned");
        }

        if (role.getIsSystem() && "SYS_SUPER_ADMIN".equals(role.getName())) {
            throw new InvalidUserCreationException(
                    "SYS_SUPER_ADMIN role cannot be assigned during user creation"
            );
        }

        // 5. VALIDATE ROLE SCOPE
        if (role.getScope() == RoleScope.PLATFORM) {

            permissionAuthorizationService.requirePlatformPermission(callerId, "USER_CREATE");

            if (organizationId != null || role.getOrganization() != null) {
                throw new IllegalArgumentException(
                        "Platform role cannot have a organization"
                );
            }

        } else if (role.getScope() == RoleScope.ORGANIZATION) {

            permissionAuthorizationService.requireOrganizationPermission(callerId, organizationId, "USER_CREATE");

            if (organizationId == null) {
                throw new IllegalArgumentException(
                        "Organization ID is required for organization role"
                );
            }

            if (role.getOrganization() == null
                    || !role.getOrganization().getId().equals(organizationId)) {
                throw new IllegalArgumentException(
                        "Role does not belong to the selected organization"
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "Unsupported role scope"
            );
        }

        // 6. CHECK CALLER'S ROLE ASSIGNMENT AUTHORITY
        if (role.getScope() == RoleScope.PLATFORM) {

            permissionAuthorizationService.requirePlatformPermission(
                    callerId,
                    "USER_ROLE_ASSIGN"
            );

            requireRoleWithinCallerAccess(callerId, role);

        } else {

            permissionAuthorizationService.requireOrganizationPermission(
                    callerId,
                    organizationId,
                    "USER_ROLE_ASSIGN"
            );

            requireOrganizationRoleWithinCallerAccess(callerId, organizationId, role);
        }

        // 7. GENERATE PASSWORD HASH
        String salt = Util.generateSalt();

        String hashedPassword = Util.hashPassword(
                user.password(),
                salt
        );

        User preparedUser = user
                .withEmail(email)
                .withPassword(hashedPassword);

        // 8. OPTIONAL PROFILE PICTURE
        String profilePictureKey = null;

        if (profilePicture != null && !profilePicture.isEmpty()) {

            ValidationUtil.validateMediaType(profilePicture);

            String filename = fileStorageService.uploadPublicFile(
                    profilePicture,
                    "users/profile-pictures"
            );

            registerUploadedFileCleanup(filename);

            profilePictureKey = filename;

            preparedUser = preparedUser.withProfilePicture(filename);
        }

        // 9. MAP USER ENTITY TO DAO
        UserDao userDao = UserMapper.toDao(
                preparedUser,
                salt
        );

        // 10. SAVE USER
        UserDao savedUser = userRepository.saveAndFlush(userDao);

        // 11. ASSIGN ROLE
        if (role.getScope() == RoleScope.PLATFORM) {

            UserPlatformRoleDao assignment =
                    UserPlatformRoleDao.builder()
                            .user(savedUser)
                            .role(role)
                            .build();

            userPlatformRoleRepository.saveAndFlush(assignment);

        } else {

            OrganizationDao organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Organization not found: " + organizationId
                            )
                    );

            if (!organization.isActive()) {
                throw new PermissionDeniedException(
                        "Cannot assign a role to an inactive organization"
                );
            }

            if (!userOrganizationRepository.existsByUser_IdAndOrganization_Id(savedUser.getId(), organizationId)) {
                userOrganizationRepository.saveAndFlush(UserOrganizationDao.builder()
                        .user(savedUser)
                        .organization(organization)
                        .build());
            }

            UserOrganizationRoleDao assignment =
                    UserOrganizationRoleDao.builder()
                            .user(savedUser)
                            .organization(organization)
                            .role(role)
                            .build();

            userOrganizationRoleRepository.saveAndFlush(assignment);
        }

        // 12. RETURN RESPONSE
        return UserMapper.toEntity(savedUser);
    }

    // This method is used to get a user by ID, but it only returns the user if they are active.
    @Cacheable(value = "users", key = "'active:' + #id")
    @Transactional(readOnly = true)
    public User getUser(UUID id){
        UserDao user = userRepository
                .findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
        return UserMapper.toEntity(user);
    }

    // This method is used to get a user by ID without checking if the user is active or not.
    // It is useful for admin purposes where you might want to retrieve users regardless of their active status.
    @Cacheable(value = "users", key = "'all:' + #id")
    @Transactional(readOnly = true)
    public User getUserById(UUID id){
        UserDao user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
        return UserMapper.toEntity(user);
    }



    @Transactional(readOnly = true)
    public Page<User> getAllUsersByFilter(
            FindAllUsersFilter filter,
            Pageable pageable
    ) {

        String searchPattern = Util.toSearchPattern(
                filter.getSearch()
        );

        boolean filterActive = filter.getIsActive() != null;

        boolean activeValue = Boolean.TRUE.equals(
                filter.getIsActive()
        );

        Page<UserDao> users = userRepository.findAllByFilters(
                searchPattern,
                filterActive,
                activeValue,
                pageable
        );

        return users.map(UserMapper::toEntity);
    }


    private void requireRoleWithinCallerAccess(
            UUID callerId,
            RoleDao role
    ) {

        Set<String> callerPermissions = new HashSet<>(
                userPlatformRoleRepository.findEffectivePermissionCodes(
                        callerId,
                        RoleScope.PLATFORM
                )
        );

        Set<String> requestedPermissions = role.getPermissions()
                .stream()
                .map(PermissionDao::getName)
                .collect(Collectors.toSet());

        if (!callerPermissions.containsAll(requestedPermissions)) {
            throw new PermissionDeniedException(
                    "You cannot assign a role beyond your own permissions"
            );
        }
    }

    private void requireOrganizationRoleWithinCallerAccess(UUID callerId, UUID organizationId, RoleDao role) {
        Set<String> callerPermissions = new HashSet<>(userOrganizationRoleRepository.findEffectivePermissionCodes(
                callerId, organizationId, RoleScope.ORGANIZATION));
        Set<String> requestedPermissions = role.getPermissions().stream()
                .map(PermissionDao::getName)
                .collect(Collectors.toSet());
        if (!callerPermissions.containsAll(requestedPermissions)) {
            throw new PermissionDeniedException("You cannot assign an organization role beyond your own permissions");
        }
    }

    private void registerUploadedFileCleanup(String filename) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status == STATUS_COMMITTED) {
                            return;
                        }

                        try {
                            fileStorageService.deleteFile(filename);
                        } catch (Exception exception) {
                            log.error(
                                    "Failed to cleanup profile picture: {}",
                                    filename,
                                    exception
                            );
                        }
                    }
                }
        );
    }
}
