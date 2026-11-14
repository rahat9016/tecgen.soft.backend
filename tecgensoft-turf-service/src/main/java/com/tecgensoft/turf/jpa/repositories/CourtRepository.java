package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.CourtDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtRepository extends JpaRepository<CourtDao, UUID> {
  Optional<CourtDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<CourtDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
