package com.allocator.notificationservice.dto;

import com.allocator.notificationservice.model.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private UUID id;
    private String name;
    private String subject;
    private String previewText;
    private String content;
    private String contentJson;
    private CampaignStatus status;
    private UUID brandId;
    private String thumbnailUrl;
    private String minTier;

    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID createdBy;
    private UUID submittedBy;
    private LocalDateTime submittedAt;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectionReason;

    private List<CampaignItemResponse> items;
}
