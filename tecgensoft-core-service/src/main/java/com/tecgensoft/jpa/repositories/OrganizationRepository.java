package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.OrganizationDao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationDao, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    boolean existsByCode(String code);
    List<OrganizationDao> findAllByOrderByNameAsc();
}
