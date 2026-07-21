package com.allocator.paymentservice.dto;

import com.allocator.paymentservice.enums.PlanTier;
import lombok.Data;

@Data
public class CreatePlanRequest {
    private String name;
    private PlanTier tier;
    /** Amount in cents (e.g. 2900 = $29.00) */
    private long amountInCents;
    private String currency;
    /** "MONTHLY" or "YEARLY" */
    private String interval;
}
