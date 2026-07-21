package com.allocator.notificationservice.service;

import com.allocator.notificationservice.dto.CampaignItemRequest;
import com.allocator.notificationservice.dto.CampaignRequest;
import com.allocator.notificationservice.dto.CampaignResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignService {

    CampaignResponse createCampaign(CampaignRequest request, UUID actorId);

    List<CampaignResponse> getAllCampaigns();

    CampaignResponse getCampaign(UUID id);

    CampaignResponse updateCampaign(UUID id, CampaignRequest request);

    void deleteCampaign(UUID id);

    CampaignResponse addItem(UUID campaignId, CampaignItemRequest request);

    CampaignResponse removeItem(UUID campaignId, UUID itemId, UUID actorId, String actorRole);

    CampaignResponse reorderItems(UUID campaignId, List<UUID> orderedItemIds);

    CampaignResponse submitForReview(UUID campaignId, UUID actorId, String actorRole);

    CampaignResponse approve(UUID campaignId, UUID actorId, String actorRole, LocalDateTime scheduledAt, boolean sendNow);

    CampaignResponse reject(UUID campaignId, UUID actorId, String actorRole, String reason);

    void sendCampaign(UUID id);

    byte[] exportPdf(UUID id);
}
