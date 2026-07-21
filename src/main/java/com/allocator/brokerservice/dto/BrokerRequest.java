package com.allocator.brokerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BrokerRequest {

    @NotNull
    private UUID brandId;

    @NotBlank
    private String name;

    private String logoUrl;
    private String country;

    /** Comma-separated licence acronyms, e.g. "FSCA, FCA, SEC" */
    private String licenseType;

    private String tradingPlatform;
    private BigDecimal minimumInvestment;
    private String currency;
    private String websiteUrl;
    private String description;
    private Boolean featured;
}
