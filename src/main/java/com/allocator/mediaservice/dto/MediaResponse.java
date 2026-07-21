package com.allocator.mediaservice.dto;

import com.allocator.mediaservice.model.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private UUID id;
    private String fileName;
    private String originalName;
    private Media.MediaType contentType;
    private Long fileSize;
    private String filePath;
    private String storageUrl;
    private UUID brandId;
    private UUID uploadedBy;
    private Boolean isPublic;
    private Boolean isArchived;
    private String thumbnailUrl;
    private String metadata;
    private Instant expiresAt;
    private Integer linkedContentCount;
    private Media.ProcessingStatus processingStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
