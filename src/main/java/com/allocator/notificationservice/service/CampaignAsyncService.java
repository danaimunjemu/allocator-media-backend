package com.allocator.notificationservice.service;

import com.allocator.authservice.model.Brand;
import com.allocator.authservice.repository.BrandRepository;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.contentservice.service.ContentWorkflowService;
import com.allocator.notificationservice.config.NotificationMetrics;
import com.allocator.notificationservice.model.*;
import com.allocator.notificationservice.provider.EmailProvider;
import com.allocator.notificationservice.provider.MailerLiteClient;
import com.allocator.notificationservice.repository.CampaignAuditLogRepository;
import com.allocator.notificationservice.repository.CampaignItemRepository;
import com.allocator.notificationservice.repository.CampaignRepository;
import com.allocator.notificationservice.repository.NotificationRepository;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.PlanTier;
import com.allocator.paymentservice.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAsyncService {

    private final SubscriberRepository subscriberRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignItemRepository campaignItemRepository;
    private final CampaignAuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final ContentRepository contentRepository;
    private final ContentWorkflowService contentWorkflowService;
    private final BrandRepository brandRepository;
    private final EmailProvider emailProvider;
    private final MailerLiteClient mailerLiteClient;
    private final NewsletterHtmlRenderer htmlRenderer;
    private final NotificationMetrics metrics;

    @Async
    public void triggerAsyncDelivery(Campaign campaign) {
        try {
            log.info("Async delivery started for newsletter: {}", campaign.getName());
            metrics.incrementCampaignSent();

            List<CampaignItem> items = campaignItemRepository.findByCampaignIdOrderBySortOrderAsc(campaign.getId());

            autoPublishLinkedContent(campaign, items);

            List<String> recipients = resolveRecipients(campaign);
            if (recipients.isEmpty()) {
                log.warn("No matching subscribers found for newsletter {} (minTier={})",
                        campaign.getName(), campaign.getMinTier());
            }

            String siteUrl = siteUrlForBrand(campaign.getBrandId());
            String html = htmlRenderer.render(campaign, items, siteUrl, siteUrl + "/unsubscribe");

            if (mailerLiteClient.isConfigured() && !recipients.isEmpty()) {
                sendViaMailerLite(campaign, recipients, html);
            } else if (!recipients.isEmpty()) {
                sendViaFallbackProvider(campaign, recipients, html);
            }

            campaign.setStatus(CampaignStatus.SENT);
            campaign.setSentAt(LocalDateTime.now());
            campaignRepository.save(campaign);
            recordAudit(campaign, CampaignWorkflowEventType.SENT, null, "SYSTEM",
                    CampaignStatus.SENDING, CampaignStatus.SENT, "Sent to " + recipients.size() + " recipients");
            log.info("Newsletter {} sent successfully to {} recipients", campaign.getName(), recipients.size());

        } catch (Exception e) {
            log.error("Critical failure sending newsletter {}: {}", campaign.getName(), e.getMessage(), e);
            campaign.setStatus(CampaignStatus.FAILED);
            campaignRepository.save(campaign);
            recordAudit(campaign, CampaignWorkflowEventType.SEND_FAILED, null, "SYSTEM",
                    CampaignStatus.SENDING, CampaignStatus.FAILED, e.getMessage());
        }
    }

    // ─── Auto-publish ───────────────────────────────────────────────────────

    private void autoPublishLinkedContent(Campaign campaign, List<CampaignItem> items) {
        for (CampaignItem item : items) {
            if (item.getItemType() != CampaignItemType.CONTENT_LINK || item.getContentId() == null) continue;

            Content content = contentRepository.findById(item.getContentId()).orElse(null);
            if (content == null || content.getStatus() == ContentStatus.PUBLISHED) continue;

            boolean published = contentWorkflowService.publishContentNow(content.getId(),
                    "Auto-published: included in newsletter \"" + campaign.getName() + "\"");
            if (published) {
                log.info("Auto-published content {} as part of newsletter {}", content.getId(), campaign.getId());
            } else {
                log.warn("Could not auto-publish content {} for newsletter {} (likely blocked by compliance approval)",
                        content.getId(), campaign.getId());
            }
        }
    }

    // ─── Recipient resolution ────────────────────────────────────────────────
    // Newsletters are not brand-specific — every active subscriber (regardless
    // of which brand they signed up under) receives every newsletter that
    // meets their plan tier.

    private List<String> resolveRecipients(Campaign campaign) {
        Set<String> emails = new LinkedHashSet<>();
        for (Subscriber subscriber : subscriberRepository.findAllActiveWithPlan()) {
            if (subscriber.getEmail() == null) continue;
            if (meetsMinTier(subscriber, campaign.getMinTier())) {
                emails.add(subscriber.getEmail());
            }
        }
        return List.copyOf(emails);
    }

    private boolean meetsMinTier(Subscriber subscriber, String minTierRaw) {
        if (minTierRaw == null || minTierRaw.isBlank()) {
            return true; // no minimum — every active subscriber qualifies
        }
        PlanTier minTier;
        try {
            minTier = PlanTier.valueOf(minTierRaw);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown minTier '{}' on newsletter — treating as no minimum", minTierRaw);
            return true;
        }
        return subscriber.getPlan().getTier().ordinal() >= minTier.ordinal();
    }

    // ─── Sending ────────────────────────────────────────────────────────────

    private void sendViaMailerLite(Campaign campaign, List<String> recipients, String html) {
        Brand brand = campaign.getBrandId() != null ? brandRepository.findById(campaign.getBrandId()).orElse(null) : null;
        String brandName = brand != null ? brand.getName() : "Allocator Media";
        String fromEmail = brand != null && brand.getContactEmail() != null ? brand.getContactEmail() : "newsletter@allocatormedia.com";

        String groupName = "Newsletter: " + campaign.getName() + " — " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String groupId = mailerLiteClient.createGroup(groupName);
        for (String email : recipients) {
            mailerLiteClient.upsertSubscriberInGroup(email, groupId);
        }
        String campaignId = mailerLiteClient.createCampaign(campaign.getName(), campaign.getSubject(), brandName, fromEmail, groupId, html);
        mailerLiteClient.sendCampaignNow(campaignId);

        for (String email : recipients) {
            trackNotification(campaign, email);
        }
    }

    // Used when MailerLite isn't configured yet, so newsletters remain usable
    // via the existing SMTP path rather than being blocked entirely.
    private void sendViaFallbackProvider(Campaign campaign, List<String> recipients, String html) {
        log.warn("MailerLite not configured — falling back to the default email provider for newsletter {}", campaign.getId());
        for (String email : recipients) {
            try {
                emailProvider.sendEmail(email, campaign.getSubject(), html);
            } catch (Exception e) {
                log.error("Failed to send newsletter to {}", email, e);
            }
            trackNotification(campaign, email);
        }
    }

    private void trackNotification(Campaign campaign, String email) {
        Notification notification = Notification.builder()
                .email(email)
                .subject(campaign.getSubject())
                .status(NotificationStatus.SENT)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    private String siteUrlForBrand(UUID brandId) {
        // Single public demo site for now; would come from a per-brand
        // domain field once this platform serves multiple live sites.
        return System.getProperty("app.public-site-url", "http://localhost:3001");
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
