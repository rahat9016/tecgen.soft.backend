package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.BookingDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingDao, UUID> {
  Optional<BookingDao> findByIdAndOrganizationId(
      UUID id, UUID organizationId);

  List<BookingDao> findByOrganizationId(UUID organizationId);
}
