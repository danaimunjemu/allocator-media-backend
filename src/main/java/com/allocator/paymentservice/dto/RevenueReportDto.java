package com.allocator.paymentservice.dto;

import com.allocator.paymentservice.enums.PlanTier;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class RevenueReportDto {
    private long totalActiveSubscribers;
    private BigDecimal monthlyRecurringRevenue;
    private BigDecimal annualRecurringRevenue;
    private double churnRate;
    private Map<PlanTier, Long> subscribersByTier;
    private BigDecimal totalRevenueLast30Days;
}
