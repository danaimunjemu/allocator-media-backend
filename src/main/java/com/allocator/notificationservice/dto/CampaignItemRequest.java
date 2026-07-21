package com.allocator.notificationservice.dto;

import com.allocator.notificationservice.model.CampaignItemType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignItemRequest {

    @NotNull
    private CampaignItemType itemType;

    /** Required when itemType == CONTENT_LINK. */
    private UUID contentId;

    /** Required when itemType == SECTION_BREAK. */
    private String label;
}
