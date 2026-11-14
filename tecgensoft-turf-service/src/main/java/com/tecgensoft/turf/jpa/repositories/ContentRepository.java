package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.ContentDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<ContentDao, UUID> {
  Optional<ContentDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<ContentDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
