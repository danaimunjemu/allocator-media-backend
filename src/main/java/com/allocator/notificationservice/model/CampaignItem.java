package com.allocator.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

// One entry in a newsletter's ordered body — either a link to an existing
// piece of Content (article/podcast/video/research), or a SECTION_BREAK
// divider with an optional label used to group the content links below it.
@Entity
@Table(name = "campaign_items", indexes = {
        @Index(name = "idx_campaign_item_campaign_id", columnList = "campaign_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private CampaignItemType itemType;

    /** Set only when itemType == CONTENT_LINK. */
    @Column(name = "content_id")
    private UUID contentId;

    /** Set only when itemType == SECTION_BREAK — the separator's heading text. */
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }
}
