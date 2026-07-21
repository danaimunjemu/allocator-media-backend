package com.allocator.notificationservice.repository;

import com.allocator.notificationservice.model.CampaignAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignAuditLogRepository extends JpaRepository<CampaignAuditLog, UUID> {
    List<CampaignAuditLog> findByCampaignIdOrderByTimestampAsc(UUID campaignId);
}
