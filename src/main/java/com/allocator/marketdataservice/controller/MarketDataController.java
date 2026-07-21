package com.allocator.marketdataservice.controller;

import com.allocator.marketdataservice.dto.MarketTickerItemDto;
import com.allocator.marketdataservice.service.MansaMarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-ticker")
@RequiredArgsConstructor
public class MarketDataController {

    private final MansaMarketDataService mansaMarketDataService;

    /** Public, unauthenticated — same visibility as GET /api/v1/payments/plans. */
    @GetMapping
    public ResponseEntity<List<MarketTickerItemDto>> getTickerItems() {
        return ResponseEntity.ok(mansaMarketDataService.getTickerItems());
    }
}
