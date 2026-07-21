package com.allocator.marketdataservice.model;

import com.allocator.authservice.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "market_ticker_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MarketTickerItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Stable slug, e.g. "exchange:KENYA" or "stock:SOUTHAFRICA:MTN" — never shown to users. */
    @Column(name = "item_key", nullable = false, unique = true, length = 100)
    private String itemKey;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "change_percent", nullable = false)
    private double changePercent;

    /** Fixed display order (indices first, then stocks) — not alphabetical/insertion order. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Mansa's own last_updated for this instrument — distinct from updatedAt (when *our* job last wrote this row). */
    @Column(name = "market_last_updated_at")
    private LocalDateTime marketLastUpdatedAt;
}
