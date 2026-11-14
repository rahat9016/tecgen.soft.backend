package com.tecgensoft.turf.jpa.repositories;

import com.tecgensoft.turf.jpa.daos.PaymentDao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentDao, UUID> {
  Optional<PaymentDao> findByIdAndOrganizationIdAndBranchId(
      UUID id, UUID organizationId, UUID branchId);

  List<PaymentDao> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);
}
