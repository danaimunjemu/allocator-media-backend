package com.allocator.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {

    @NotBlank(message = "Newsletter name is required")
    private String name;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String previewText;

    /** Legacy topic-based targeting — optional, superseded by brandId + minTier. */
    private String topic;

    /** Rendered HTML of the intro content, shown before the linked content items — used for send/PDF. */
    private String content;

    /** Raw TipTap editor JSON of the intro content — used to restore the editor when reopening a draft. */
    private String contentJson;

    private UUID brandId;

    private String thumbnailUrl;

    /** Minimum subscriber plan tier that should receive this newsletter — null/blank means everyone. */
    private String minTier;
}
