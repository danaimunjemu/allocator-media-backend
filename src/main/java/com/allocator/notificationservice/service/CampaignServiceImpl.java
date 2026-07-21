package com.allocator.notificationservice.service;

import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.notificationservice.dto.CampaignItemRequest;
import com.allocator.notificationservice.dto.CampaignItemResponse;
import com.allocator.notificationservice.dto.CampaignRequest;
import com.allocator.notificationservice.dto.CampaignResponse;
import com.allocator.notificationservice.mapper.CampaignMapper;
import com.allocator.notificationservice.model.*;
import com.allocator.notificationservice.repository.CampaignAuditLogRepository;
import com.allocator.notificationservice.repository.CampaignItemRepository;
import com.allocator.notificationservice.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignItemRepository campaignItemRepository;
    private final CampaignAuditLogRepository auditLogRepository;
    private final CampaignMapper campaignMapper;
    private final CampaignAsyncService asyncService;
    private final ContentRepository contentRepository;
    private final NewsletterHtmlRenderer htmlRenderer;
    private final NewsletterPdfService pdfService;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request, UUID actorId) {
        log.info("Creating new newsletter: {}", request.getName());
        Campaign campaign = campaignMapper.toEntity(request);
        campaign.setBrandId(request.getBrandId());
        campaign.setThumbnailUrl(request.getThumbnailUrl());
        campaign.setMinTier(request.getMinTier());
        campaign.setPreviewText(request.getPreviewText());
        campaign.setCreatedBy(actorId);
        campaign = campaignRepository.save(campaign);
        recordAudit(campaign, CampaignWorkflowEventType.CREATED, actorId, null, null, campaign.getStatus(), null);
        return toResponse(campaign);
    }

    @Override
    public List<CampaignResponse> getAllCampaigns() {
        return campaignRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CampaignResponse getCampaign(UUID id) {
        return toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(UUID id, CampaignRequest request) {
        Campaign campaign = getEntity(id);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only a draft newsletter can be edited — reject it back to draft first");
        }
        campaign.setName(request.getName());
        campaign.setSubject(request.getSubject());
        campaign.setPreviewText(request.getPreviewText());
        campaign.setTopic(request.getTopic());
        campaign.setContent(request.getContent());
        campaign.setContentJson(request.getContentJson());
        campaign.setBrandId(request.getBrandId());
        campaign.setThumbnailUrl(request.getThumbnailUrl());
        campaign.setMinTier(request.getMinTier());
        campaign = campaignRepository.save(campaign);
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public void deleteCampaign(UUID id) {
        Campaign campaign = getEntity(id);
        if (campaign.getStatus() == CampaignStatus.SENDING || campaign.getStatus() == CampaignStatus.SENT) {
            throw new IllegalStateException("Cannot delete a newsletter that is sending or has already been sent");
        }
        campaignItemRepository.deleteByCampaignId(id);
        campaignRepository.delete(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse addItem(UUID campaignId, CampaignItemRequest request) {
        Campaign campaign = getEntity(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Newsletter items can only be added while in draft");
        }
        if (request.getItemType() == CampaignItemType.CONTENT_LINK && request.getContentId() == null) {
            throw new IllegalArgumentException("contentId is required for a CONTENT_LINK item");
        }

        List<CampaignItem> existing = campaignItemRepository.findByCampaignIdOrderBySortOrderAsc(campaignId);
        int nextOrder = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getSortOrder() + 1;

        CampaignItem item = CampaignItem.builder()
                .campaignId(campaignId)
                .sortOrder(nextOrder)
                .itemType(request.getItemType())
                .contentId(request.getContentId())
                .label(request.getLabel())
                .build();
        campaignItemRepository.save(item);
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse removeItem(UUID campaignId, UUID itemId, UUID actorId, String actorRole) {
        Campaign campaign = getEntity(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.IN_REVIEW) {
            throw new IllegalStateException("Newsletter items can only be removed while in draft or under review");
        }
        CampaignItem item = campaignItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (!item.getCampaignId().equals(campaignId)) {
            throw new IllegalArgumentException("Item does not belong to this newsletter");
        }
        campaignItemRepository.delete(item);

        if (campaign.getStatus() == CampaignStatus.IN_REVIEW) {
            String description = item.getItemType() == CampaignItemType.SECTION_BREAK
                    ? "Removed section '" + item.getLabel() + "' during review"
                    : "Removed linked content " + item.getContentId() + " during review";
            recordAudit(campaign, CampaignWorkflowEventType.ITEM_REMOVED, actorId, actorRole,
                    campaign.getStatus(), campaign.getStatus(), description);
        }
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse reorderItems(UUID campaignId, List<UUID> orderedItemIds) {
        Campaign campaign = getEntity(campaignId);
        Map<UUID, CampaignItem> byId = campaignItemRepository.findByCampaignIdOrderBySortOrderAsc(campaignId)
                .stream().collect(Collectors.toMap(CampaignItem::getId, i -> i));
        int order = 0;
        for (UUID itemId : orderedItemIds) {
            CampaignItem item = byId.get(itemId);
            if (item == null) continue;
            item.setSortOrder(order++);
            campaignItemRepository.save(item);
        }
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse submitForReview(UUID campaignId, UUID actorId, String actorRole) {
        Campaign campaign = getEntity(campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only a draft newsletter can be submitted for review");
        }
        CampaignStatus prev = campaign.getStatus();
        campaign.setStatus(CampaignStatus.IN_REVIEW);
        campaign.setSubmittedBy(actorId);
        campaign.setSubmittedAt(LocalDateTime.now());
        campaign = campaignRepository.save(campaign);
        recordAudit(campaign, CampaignWorkflowEventType.SUBMITTED_FOR_REVIEW, actorId, actorRole,
                prev, campaign.getStatus(), null);
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse approve(UUID campaignId, UUID actorId, String actorRole, LocalDateTime scheduledAt, boolean sendNow) {
        Campaign campaign = getEntity(campaignId);
        if (campaign.getStatus() != CampaignStatus.IN_REVIEW) {
            throw new IllegalStateException("Only a newsletter under review can be approved");
        }
        CampaignStatus prev = campaign.getStatus();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setReviewedBy(actorId);
        campaign.setReviewedAt(LocalDateTime.now());
        campaign.setScheduledAt(scheduledAt != null ? scheduledAt : LocalDateTime.now());
        campaign.setRejectionReason(null);
        campaign = campaignRepository.save(campaign);
        recordAudit(campaign, CampaignWorkflowEventType.APPROVED, actorId, actorRole, prev, campaign.getStatus(), null);

        if (sendNow) {
            // "Approve & Send Now" means immediately — not "schedule for the
            // current instant and wait for the once-a-minute poller to notice".
            log.info("Triggering immediate newsletter delivery for: {}", campaign.getName());
            campaign.setStatus(CampaignStatus.SENDING);
            campaign = campaignRepository.save(campaign);
            asyncService.triggerAsyncDelivery(campaign);
        }

        return toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse reject(UUID campaignId, UUID actorId, String actorRole, String reason) {
        Campaign campaign = getEntity(campaignId);
        if (campaign.getStatus() != CampaignStatus.IN_REVIEW) {
            throw new IllegalStateException("Only a newsletter under review can be rejected");
        }
        CampaignStatus prev = campaign.getStatus();
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setReviewedBy(actorId);
        campaign.setReviewedAt(LocalDateTime.now());
        campaign.setRejectionReason(reason);
        campaign = campaignRepository.save(campaign);
        recordAudit(campaign, CampaignWorkflowEventType.REJECTED, actorId, actorRole, prev, campaign.getStatus(), reason);
        return toResponse(campaign);
    }

    @Override
    @Transactional
    public void sendCampaign(UUID id) {
        Campaign campaign = getEntity(id);

        if (campaign.getStatus() == CampaignStatus.SENT || campaign.getStatus() == CampaignStatus.SENDING) {
            throw new IllegalStateException("Newsletter has already been sent or is currently sending");
        }
        if (campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled (approved) newsletter can be sent");
        }

        log.info("Triggering newsletter delivery for: {}", campaign.getName());

        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepository.save(campaign);

        asyncService.triggerAsyncDelivery(campaign);
    }

    @Override
    public byte[] exportPdf(UUID id) {
        Campaign campaign = getEntity(id);
        List<CampaignItem> items = campaignItemRepository.findByCampaignIdOrderBySortOrderAsc(id);
        String siteUrl = System.getProperty("app.public-site-url", "http://localhost:3001");
        String html = htmlRenderer.render(campaign, items, siteUrl, null);
        try {
            return pdfService.toPdf(html);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to render newsletter PDF", e);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Campaign getEntity(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found: " + id));
    }

    private CampaignResponse toResponse(Campaign campaign) {
        List<CampaignItem> items = campaignItemRepository.findByCampaignIdOrderBySortOrderAsc(campaign.getId());

        List<UUID> contentIds = items.stream()
                .filter(i -> i.getItemType() == CampaignItemType.CONTENT_LINK && i.getContentId() != null)
                .map(CampaignItem::getContentId)
                .toList();
        Map<UUID, Content> contentById = contentIds.isEmpty()
                ? Map.of()
                : contentRepository.findAllById(contentIds).stream()
                        .collect(Collectors.toMap(Content::getId, c -> c));

        List<CampaignItemResponse> itemResponses = items.stream().map(item -> {
            CampaignItemResponse.CampaignItemResponseBuilder b = CampaignItemResponse.builder()
                    .id(item.getId())
                    .itemType(item.getItemType())
                    .sortOrder(item.getSortOrder())
                    .label(item.getLabel())
                    .contentId(item.getContentId());
            Content content = item.getContentId() != null ? contentById.get(item.getContentId()) : null;
            if (content != null) {
                b.contentTitle(content.getTitle())
                        .contentSlug(content.getSlug())
                        .contentType(content.getContentType() != null ? content.getContentType().name() : null)
                        .contentHeroImage(content.getHeroImageUrl())
                        .contentStatus(content.getStatus() != null ? content.getStatus().name() : null);
            }
            return b.build();
        }).collect(Collectors.toList());

        CampaignResponse response = campaignMapper.toResponse(campaign);
        response.setItems(itemResponses);
        return response;
    }

    private void recordAudit(Campaign campaign, CampaignWorkflowEventType eventType, UUID actorId, String actorRole,
                              CampaignStatus fromStatus, CampaignStatus toStatus, String reason) {
        auditLogRepository.save(CampaignAuditLog.builder()
                .campaignId(campaign.getId())
                .brandId(campaign.getBrandId())
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus != null ? toStatus.name() : null)
                .reason(reason)
                .build());
    }
}
