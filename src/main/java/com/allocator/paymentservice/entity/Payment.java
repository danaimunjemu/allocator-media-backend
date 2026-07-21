package com.allocator.paymentservice.entity;

import com.allocator.paymentservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "stripeInvoiceId"))
@Getter
@Setter
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private UUID subscriptionId;

    private String stripePaymentIntentId;

    @Column(unique = true)
    private String stripeInvoiceId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private Instant paidAt;
}
