package com.allocator.paymentservice.repository;

import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {
    Optional<Subscriber> findByUserId(UUID userId);
    Optional<Subscriber> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Subscriber> findByStripeCustomerId(String stripeCustomerId);
    Optional<Subscriber> findByEmailIgnoreCase(String email);
    List<Subscriber> findByStatus(SubscriptionStatus status);

    long countByStatus(SubscriptionStatus status);
    long countByStatusAndCreatedAtAfter(SubscriptionStatus status, Instant since);

    /** All active-billing, active-account subscribers, with their plan eagerly fetched for tier checks. */
    @Query("SELECT s FROM Subscriber s JOIN FETCH s.plan " +
           "WHERE s.status = com.allocator.paymentservice.enums.SubscriptionStatus.ACTIVE " +
           "AND s.accountStatus = com.allocator.paymentservice.enums.AccountStatus.ACTIVE")
    List<Subscriber> findAllActiveWithPlan();

    @Query("SELECT COALESCE(SUM(s.plan.amount), 0) FROM Subscriber s " +
           "WHERE s.status = 'ACTIVE' AND s.plan.interval = :interval")
    BigDecimal sumAmountByInterval(String interval);
}
