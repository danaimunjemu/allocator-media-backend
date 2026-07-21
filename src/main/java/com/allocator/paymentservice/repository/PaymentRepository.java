package com.allocator.paymentservice.repository;

import com.allocator.paymentservice.entity.Payment;
import com.allocator.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByStripeInvoiceId(String stripeInvoiceId);
    List<Payment> findBySubscriptionIdAndStatus(UUID subscriptionId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'SUCCEEDED' AND p.paidAt >= :since")
    BigDecimal sumSucceededAmountSince(Instant since);
}
