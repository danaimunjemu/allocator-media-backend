package com.allocator.paymentservice.dto;

import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.AccountStatus;
import com.allocator.paymentservice.enums.PlanTier;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Data
public class SubscriptionDto {
    private UUID id;
    private UUID userId;
    private PlanDto plan;
    private SubscriptionStatus status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Boolean cancelAtPeriodEnd;
    private String cancellationReason;

    // Flat convenience fields for the admin subscribers list UI.
    private String email;
    private String name;
    private String firstName;
    private String lastName;
    private AccountStatus accountStatus;
    private Instant lastActiveAt;
    private String location;
    private String primaryDevice;
    private String webBrowser;
    private String customerId;
    private String stripeCustomerId;
    private PlanTier tier;
    private BigDecimal mrr;
    private String currency;
    private Instant renewalDate;
    private Instant joinDate;
    private UUID planId;
    private String planName;
    private String stripePriceId;

    public static SubscriptionDto from(Subscriber sub) {
        Plan plan = sub.getPlan();

        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(sub.getId());
        dto.setUserId(sub.getUserId());
        dto.setPlan(PlanDto.from(plan));
        dto.setStatus(sub.getStatus());
        dto.setCurrentPeriodStart(sub.getCurrentPeriodStart());
        dto.setCurrentPeriodEnd(sub.getCurrentPeriodEnd());
        dto.setCancelAtPeriodEnd(sub.getCancelAtPeriodEnd());
        dto.setCancellationReason(sub.getCancellationReason());

        dto.setEmail(sub.getEmail());
        dto.setName(sub.getName());
        dto.setFirstName(sub.getFirstName());
        dto.setLastName(sub.getLastName());
        dto.setAccountStatus(sub.getAccountStatus());
        dto.setLastActiveAt(sub.getLastActiveAt());
        dto.setLocation(sub.getLocation());
        dto.setPrimaryDevice(sub.getPrimaryDevice());
        dto.setWebBrowser(sub.getWebBrowser());
        dto.setCustomerId(sub.getStripeCustomerId());
        dto.setStripeCustomerId(sub.getStripeCustomerId());
        dto.setTier(plan.getTier());
        dto.setMrr("year".equalsIgnoreCase(plan.getInterval())
                ? plan.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : plan.getAmount());
        dto.setCurrency(plan.getCurrency());
        dto.setRenewalDate(sub.getCurrentPeriodEnd());
        dto.setJoinDate(sub.getCreatedAt());
        dto.setPlanId(plan.getId());
        dto.setPlanName(plan.getName());
        dto.setStripePriceId(plan.getStripePriceId());
        return dto;
    }
}
