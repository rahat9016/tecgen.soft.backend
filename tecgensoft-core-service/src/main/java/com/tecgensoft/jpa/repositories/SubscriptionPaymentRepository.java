package com.tecgensoft.jpa.repositories;

import com.tecgensoft.jpa.daos.SubscriptionPaymentDao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentDao,UUID> {
  List<SubscriptionPaymentDao> findAllBySubscription_IdOrderByCreatedAtDesc(UUID subscriptionId);
  Optional<SubscriptionPaymentDao> findByIdAndSubscription_Id(UUID id,UUID subscriptionId);
}
