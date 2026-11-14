package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.ReservationDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationDao, UUID> {
  Optional<ReservationDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<ReservationDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
