package com.allocator.paymentservice.entity;

import com.allocator.paymentservice.enums.PlanTier;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
@Setter
public class Plan extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String stripePriceId;

    private String stripeProductId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** "month" or "year" */
    @Column(nullable = false)
    private String interval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanTier tier;

    @Column(nullable = false)
    private Boolean active = true;
}
