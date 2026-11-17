package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.UserOrganizationDao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserOrganizationRepository
        extends JpaRepository<UserOrganizationDao, UUID> {

    boolean existsByUser_IdAndOrganization_Id(
            UUID userId,
            UUID organizationId
    );

    @EntityGraph(attributePaths = {"organization"})
    List<UserOrganizationDao> findByUser_Id(UUID userId);
}
