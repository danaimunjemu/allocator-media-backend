package com.allocator.marketdataservice.repository;

import com.allocator.marketdataservice.model.MarketTickerItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketTickerItemRepository extends JpaRepository<MarketTickerItem, UUID> {
    Optional<MarketTickerItem> findByItemKey(String itemKey);
    List<MarketTickerItem> findAllByOrderBySortOrderAsc();
    List<MarketTickerItem> findByItemKeyStartingWith(String prefix);
}
