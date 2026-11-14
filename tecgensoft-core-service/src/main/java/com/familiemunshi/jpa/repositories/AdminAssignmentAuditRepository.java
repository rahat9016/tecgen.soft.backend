package com.familiemunshi.jpa.repositories;

import com.familiemunshi.jpa.daos.AdminAssignmentAuditDao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAssignmentAuditRepository
    extends JpaRepository<AdminAssignmentAuditDao, UUID> {}
