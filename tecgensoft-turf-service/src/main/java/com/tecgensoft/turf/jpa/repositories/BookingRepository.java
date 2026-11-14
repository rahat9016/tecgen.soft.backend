package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.BookingDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingDao, UUID> {
  Optional<BookingDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<BookingDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
