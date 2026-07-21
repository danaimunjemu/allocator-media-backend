package com.allocator.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotBlank
    private String planId;
    private String successUrl;
    private String cancelUrl;
}
