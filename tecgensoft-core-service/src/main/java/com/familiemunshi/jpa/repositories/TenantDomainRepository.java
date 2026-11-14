package com.familiemunshi.jpa.repositories;

import com.familiemunshi.jpa.daos.TenantDomainDao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDomainRepository extends JpaRepository<TenantDomainDao, UUID> {}
