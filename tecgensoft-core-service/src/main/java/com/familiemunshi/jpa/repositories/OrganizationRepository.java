package com.familiemunshi.jpa.repositories;

import com.familiemunshi.jpa.daos.OrganizationDao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationDao, UUID> {}
