package com.allocator.paymentservice.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChangePlanRequest {
    private UUID newPlanId;
}
