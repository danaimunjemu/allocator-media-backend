package com.allocator.paymentservice.dto;

import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.enums.PlanTier;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class AdminPlanDto {
    private UUID id;
    private String name;
    private PlanTier tier;
    private BigDecimal amount;
    private String currency;
    private String billingInterval;
    private String stripePriceId;
    private String stripeProductId;
    private boolean active;
    private Instant createdAt;

    public static AdminPlanDto from(Plan plan) {
        AdminPlanDto dto = new AdminPlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setTier(plan.getTier());
        dto.setAmount(plan.getAmount());
        dto.setCurrency(plan.getCurrency());
        dto.setBillingInterval("year".equalsIgnoreCase(plan.getInterval()) ? "YEARLY" : "MONTHLY");
        dto.setStripePriceId(plan.getStripePriceId());
        dto.setStripeProductId(plan.getStripeProductId());
        dto.setActive(Boolean.TRUE.equals(plan.getActive()));
        dto.setCreatedAt(plan.getCreatedAt());
        return dto;
    }
}
