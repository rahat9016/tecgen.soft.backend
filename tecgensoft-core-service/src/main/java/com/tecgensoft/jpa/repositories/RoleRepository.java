package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.RoleDao;
import com.tecgensoft.service.enums.RoleScope;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleDao, UUID>, JpaSpecificationExecutor<RoleDao> {
    Optional<RoleDao> findByName(String name);

    boolean existsByNameIgnoreCaseAndScope(String name, RoleScope scope);

    boolean existsByNameIgnoreCaseAndScopeAndOrganization_Id(String name, RoleScope scope, UUID organizationId);

    java.util.List<RoleDao> findByOrganization_IdAndScopeOrderByNameAsc(UUID organizationId, RoleScope scope);

    Optional<RoleDao> findByIdAndOrganization_IdAndScope(UUID id, UUID organizationId, RoleScope scope);

    Optional<RoleDao> findByNameIgnoreCaseAndScope(String name, RoleScope scope);

    @Query("""
            SELECT r
            FROM RoleDao r
            WHERE r.scope = :scope
              AND (
                  LOWER(r.name) LIKE :searchPattern ESCAPE '!'
              )
              AND (
                  :filterActive = false
                  OR r.isActive = :activeValue
              )
              AND (
                  :filterSystem = false
                  OR r.isSystem = :systemValue
              )
            """)
    Page<RoleDao> findAllByFilters(@Param("scope") RoleScope scope, @Param("searchPattern") String searchPattern, @Param("filterActive") boolean filterActive, @Param("activeValue") boolean activeValue, @Param("filterSystem") boolean filterSystem, @Param("systemValue") boolean systemValue, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT role
            FROM RoleDao role
            WHERE role.id = :roleId
              AND role.scope = :scope
              AND role.organization IS NULL
            """)
    Optional<RoleDao> findPlatformRoleByIdForUpdate(@Param("roleId") UUID roleId, @Param("scope") RoleScope scope);
}
