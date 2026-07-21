package com.allocator.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaign_topic", columnList = "topic"),
        @Index(name = "idx_campaign_brand_id", columnList = "brand_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    // Legacy topic-based targeting — superseded by brandId + minTier, kept
    // nullable for backward compatibility with pre-newsletter campaigns.
    private String topic;

    // Rendered HTML — used for the sent email and PDF export.
    @Column(columnDefinition = "TEXT")
    private String content;

    // Raw TipTap editor JSON — used to restore the editor when reopening a draft.
    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // ─── Newsletter fields ──────────────────────────────────────────────────

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "preview_text")
    private String previewText;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    /** Minimum subscriber plan tier that should receive this newsletter — null means everyone. */
    @Column(name = "min_tier")
    private String minTier;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "submitted_by")
    private UUID submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
