package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.VenueDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<VenueDao, UUID> {
  Optional<VenueDao> findByIdAndOrganizationId(
      UUID id, UUID organizationId);

  List<VenueDao> findByOrganizationId(UUID organizationId);
}
