package com.allocator.paymentservice.dto;

import lombok.Data;

@Data
public class UpdatePlanRequest {
    private String name;

    /** Amount in cents (e.g. 2900 = $29.00). If any of amountInCents/currency/interval is set, all three must be. */
    private Long amountInCents;
    private String currency;
    /** "MONTHLY" or "YEARLY" */
    private String interval;

    public boolean hasPriceChange() {
        return amountInCents != null || currency != null || interval != null;
    }
}
