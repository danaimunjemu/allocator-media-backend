package com.allocator.analyticsservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/** Mirrors RevenueReportDto from payment-service — kept deliberately minimal. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSummaryDto {
    private long totalActiveSubscribers;
    private BigDecimal monthlyRecurringRevenue;
    private BigDecimal annualRecurringRevenue;
    /** Keys match PlanTier enum names: FREE, STARTER, PRO */
    private Map<String, Long> subscribersByTier;
}
