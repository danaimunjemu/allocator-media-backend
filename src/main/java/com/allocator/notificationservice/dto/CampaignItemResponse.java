package com.allocator.notificationservice.dto;

import com.allocator.notificationservice.model.CampaignItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignItemResponse {

    private UUID id;
    private CampaignItemType itemType;
    private Integer sortOrder;

    // SECTION_BREAK
    private String label;

    // CONTENT_LINK — denormalized from Content so the admin UI doesn't need a
    // second round trip per item.
    private UUID contentId;
    private String contentTitle;
    private String contentSlug;
    private String contentType;
    private String contentHeroImage;
    private String contentStatus;
}
