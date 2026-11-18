package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.SubscriptionDao;
import com.tecgensoft.service.enums.ProductCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface SubscriptionRepository extends JpaRepository<SubscriptionDao, UUID> {
    List<SubscriptionDao> findAllByStatusInAndEndsAtBefore(List<com.tecgensoft.service.enums.SubscriptionStatus> statuses, Instant now);
    boolean existsByIdAndOrganization_Id(UUID id, UUID organizationId);
    Optional<SubscriptionDao> findTopByOrganization_IdAndProductOrderByCreatedAtDesc(
            UUID organizationId,
            ProductCode product
    );

    List<SubscriptionDao> findAllByOrganization_IdOrderByCreatedAtDesc(UUID organizationId);

    Optional<SubscriptionDao> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Optional<SubscriptionDao> findFirstByOrganization_IdAndProductAndStatusOrderByCreatedAtDesc(
            UUID organizationId,
            ProductCode product,
            com.tecgensoft.service.enums.SubscriptionStatus status
    );
}
