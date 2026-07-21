package com.allocator.notificationservice.repository;

import com.allocator.notificationservice.model.CampaignItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignItemRepository extends JpaRepository<CampaignItem, UUID> {
    List<CampaignItem> findByCampaignIdOrderBySortOrderAsc(UUID campaignId);
    void deleteByCampaignId(UUID campaignId);
}
