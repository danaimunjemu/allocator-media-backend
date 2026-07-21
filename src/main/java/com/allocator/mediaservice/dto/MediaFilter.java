package com.allocator.mediaservice.dto;

import com.allocator.mediaservice.model.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFilter {

    private UUID brandId;
    private Media.MediaType contentType;
    private UUID uploadedBy;
    private Boolean isPublic;
    private Boolean isArchived;
    private Media.ProcessingStatus processingStatus;
    private String keyword;
    private Instant uploadedAfter;
    private Instant uploadedBefore;
    private List<UUID> ids;
    private Boolean hasExpired;
}
