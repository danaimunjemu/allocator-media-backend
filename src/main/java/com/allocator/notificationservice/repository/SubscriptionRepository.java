package com.allocator.notificationservice.repository;

import com.allocator.notificationservice.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByTopicAndEnabledTrue(String topic);

    Optional<Subscription> findByEmailAndTopic(String email, String topic);

    List<Subscription> findByBrandIdAndEnabledTrue(String brandId);
}
