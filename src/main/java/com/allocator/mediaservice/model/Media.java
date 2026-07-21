package com.allocator.mediaservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_filename", columnList = "fileName"),
    @Index(name = "idx_media_content_type", columnList = "contentType"),
    @Index(name = "idx_media_brand_id", columnList = "brandId"),
    @Index(name = "idx_media_created_at", columnList = "createdAt")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Media extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false)
    private String fileName;

    @NotBlank
    @Size(max = 500)
    @Column(name = "original_name", nullable = false)
    private String originalName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private MediaType contentType;

    @NotNull
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Size(max = 500)
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Size(max = 500)
    @Column(name = "storage_url")
    private String storageUrl;

    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Additional fields for linking
    @Column(name = "linked_content_count")
    @Builder.Default
    private Integer linkedContentCount = 0;

    // File processing status
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status")
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    public enum MediaType {
        IMAGE,
        AUDIO,
        VIDEO,
        DOCUMENT,
        OTHER
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
