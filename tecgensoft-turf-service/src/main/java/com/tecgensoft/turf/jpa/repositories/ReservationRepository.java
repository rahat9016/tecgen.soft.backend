package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.ReservationDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ReservationDao, UUID> {
  Optional<ReservationDao> findByIdAndOrganizationId(
      UUID id, UUID organizationId);

  List<ReservationDao> findByOrganizationId(UUID organizationId);
}
