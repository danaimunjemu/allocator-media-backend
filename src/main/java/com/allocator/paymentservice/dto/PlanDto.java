package com.allocator.paymentservice.dto;

import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.enums.PlanTier;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PlanDto {
    private UUID id;
    private String name;
    private BigDecimal amount;
    private String currency;
    private String interval;
    private PlanTier tier;

    public static PlanDto from(Plan plan) {
        PlanDto dto = new PlanDto();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setAmount(plan.getAmount());
        dto.setCurrency(plan.getCurrency());
        dto.setInterval(plan.getInterval());
        dto.setTier(plan.getTier());
        return dto;
    }
}
