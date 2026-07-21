package com.allocator.paymentservice.entity;

import com.allocator.paymentservice.enums.AccountStatus;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A reader/end-user who signed up on a public brand site — billing profile
 * plus engagement/marketing metadata. Deliberately separate from {@code User}
 * (staff/administrator accounts) even though it points back at the same
 * {@code users} row for authentication.
 */
@Entity
@Table(name = "subscribers",
        uniqueConstraints = @UniqueConstraint(columnNames = "stripeSubscriptionId"))
@Getter
@Setter
public class Subscriber extends BaseEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String email;

    private String firstName;

    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    private String stripeCustomerId;

    /** Null for FREE-tier users — they have no Stripe subscription. */
    @Column(unique = true)
    private String stripeSubscriptionId;

    /** Billing status of the current plan. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    /** Account-level enable/disable, independent of billing status. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;

    @Column(nullable = false)
    private Boolean cancelAtPeriodEnd = false;

    @Column(columnDefinition = "text")
    private String cancellationReason;

    /** Most recent login/activity timestamp — drives "Last active" in the admin UI. */
    private Instant lastActiveAt;

    /** Best-effort geolocation from IP; "Unknown" until a geolocation source is wired up. */
    private String location;

    /** Derived from the most recent session's parsed User-Agent (e.g. "MacBook"). */
    private String primaryDevice;

    /** Derived from the most recent session's parsed User-Agent (e.g. "Chrome"). */
    private String webBrowser;

    public String getName() {
        String full = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return full.isEmpty() ? null : full;
    }
}
