package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.CourtDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtRepository extends JpaRepository<CourtDao, UUID> {
  Optional<CourtDao> findByIdAndOrganizationId(
      UUID id, UUID organizationId);

  List<CourtDao> findByOrganizationId(UUID organizationId);
}
