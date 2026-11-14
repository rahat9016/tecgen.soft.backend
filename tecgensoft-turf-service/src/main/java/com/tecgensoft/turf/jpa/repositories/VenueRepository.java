package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.VenueDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<VenueDao, UUID> {
  Optional<VenueDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<VenueDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
