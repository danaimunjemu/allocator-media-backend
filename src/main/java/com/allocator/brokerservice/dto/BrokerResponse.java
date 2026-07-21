package com.allocator.brokerservice.dto;

import com.allocator.brokerservice.entity.Broker;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class BrokerResponse {

    private UUID id;
    private UUID brandId;
    private String name;
    private String logoUrl;
    private String country;
    private String licenseType;
    private String tradingPlatform;
    private BigDecimal minimumInvestment;
    private String currency;
    private String websiteUrl;
    private String description;
    private Boolean featured;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static BrokerResponse from(Broker broker) {
        BrokerResponse dto = new BrokerResponse();
        dto.setId(broker.getId());
        dto.setBrandId(broker.getBrandId());
        dto.setName(broker.getName());
        dto.setLogoUrl(broker.getLogoUrl());
        dto.setCountry(broker.getCountry());
        dto.setLicenseType(broker.getLicenseType());
        dto.setTradingPlatform(broker.getTradingPlatform());
        dto.setMinimumInvestment(broker.getMinimumInvestment());
        dto.setCurrency(broker.getCurrency());
        dto.setWebsiteUrl(broker.getWebsiteUrl());
        dto.setDescription(broker.getDescription());
        dto.setFeatured(broker.getFeatured());
        dto.setActive(broker.getActive());
        dto.setCreatedAt(broker.getCreatedAt());
        dto.setUpdatedAt(broker.getUpdatedAt());
        return dto;
    }
}
