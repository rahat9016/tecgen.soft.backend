package com.familiemunshi.jpa.repositories;

import com.familiemunshi.jpa.daos.SubscriptionDao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionDao, UUID> {}
