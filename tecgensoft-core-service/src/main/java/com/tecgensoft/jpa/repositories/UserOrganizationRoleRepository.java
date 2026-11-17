package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.OrganizationDao;
import com.tecgensoft.jpa.daos.UserOrganizationRoleDao;
import com.tecgensoft.service.enums.RoleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserOrganizationRoleRepository
        extends JpaRepository<UserOrganizationRoleDao, UUID> {

    @Query("""
                SELECT DISTINCT permission.name
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.role role
                JOIN role.permissions permission
                WHERE ubr.user.id = :userId
                  AND ubr.organization.id = :organizationId
                  AND ubr.user.isActive = true
                  AND ubr.organization.isActive = true
                  AND role.isActive = true
                  AND role.scope = :scope
                  AND role.organization.id = :organizationId
            """)
    List<String> findEffectivePermissionCodes(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("scope") RoleScope scope
    );

    @Query("""
                SELECT COUNT(ubr) > 0
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.role role
                JOIN role.permissions permission
                WHERE ubr.user.id = :userId
                  AND ubr.organization.id = :organizationId
                  AND ubr.user.isActive = true
                  AND ubr.organization.isActive = true
                  AND role.isActive = true
                  AND role.scope = :scope
                  AND role.organization.id = :organizationId
                  AND permission.name = :permissionCode
            """)
    boolean hasOrganizationPermission(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("scope") RoleScope scope,
            @Param("permissionCode") String permissionCode
    );

    @Query("""
                SELECT COUNT(ubr) > 0
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.role role
                JOIN role.permissions permission
                WHERE ubr.user.id = :userId
                  AND ubr.user.isActive = true
                  AND ubr.organization.isActive = true
                  AND role.isActive = true
                  AND role.scope = :scope
                  AND role.organization.id = ubr.organization.id
                  AND permission.name = :permissionCode
            """)
    boolean hasAnyOrganizationPermission(
            @Param("userId") UUID userId,
            @Param("scope") RoleScope scope,
            @Param("permissionCode") String permissionCode
    );

    @Query("""
                SELECT DISTINCT role.name
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.role role
                WHERE ubr.user.id = :userId
                  AND ubr.organization.id = :organizationId
                  AND ubr.user.isActive = true
                  AND ubr.organization.isActive = true
                  AND role.isActive = true
                  AND role.scope = :scope
                  AND role.organization.id = :organizationId
            """)
    List<String> findRoleNames(
            @Param("userId") UUID userId,
            @Param("organizationId") UUID organizationId,
            @Param("scope") RoleScope scope
    );


    @Query("""
                SELECT COUNT(ubr) > 0
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.role role
                WHERE ubr.user.id = :userId
                  AND ubr.user.isActive = false
                  AND ubr.organization.isActive = true
                  AND role.name = :roleName
                  AND role.scope = :scope
                  AND role.isActive = true
                  AND role.organization.id = ubr.organization.id
            """)
    boolean hasPendingOrganizationAdminAssignment(
            @Param("userId") UUID userId,
            @Param("roleName") String roleName,
            @Param("scope") RoleScope scope
    );


    // GET USER'S AUTHORIZED ACTIVE ORGANIZATIONS
    @Query("""
                SELECT DISTINCT organization
                FROM UserOrganizationRoleDao ubr
                JOIN ubr.organization organization
                JOIN ubr.role role
                WHERE ubr.user.id = :userId
                  AND ubr.user.isActive = true
                  AND organization.isActive = true
                  AND role.isActive = true
                  AND role.scope = :scope
                  AND role.organization.id = organization.id
                ORDER BY organization.name
            """)
    List<OrganizationDao> findAuthorizedOrganizations(@Param("userId") UUID userId, @Param("scope") RoleScope scope);
}
