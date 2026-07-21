package com.allocator.brokerservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "BrokerServiceBroker")
@Table(name = "brokers", indexes = {
    @Index(name = "idx_broker_brand_id", columnList = "brand_id"),
    @Index(name = "idx_broker_active", columnList = "active"),
    @Index(name = "idx_broker_country", columnList = "country")
})
@Getter
@Setter
public class Broker extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_url")
    private String logoUrl;

    private String country;

    /** Comma-separated licence acronyms, e.g. "FSCA, FCA, SEC" */
    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "trading_platform")
    private String tradingPlatform;

    @Column(name = "minimum_investment", precision = 14, scale = 2)
    private BigDecimal minimumInvestment;

    @Column(length = 3)
    private String currency;

    @Column(name = "website_url")
    private String websiteUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean featured = false;

    /** Soft-delete flag — false means logically deleted. */
    @Column(nullable = false)
    private Boolean active = true;
}
