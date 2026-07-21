package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.ContentType;
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
public class VersionHistoryResponse {

    private UUID id;
    private UUID parentContentId;
    private Integer versionNumber;
    private Boolean latestVersion;

    private String title;
    private String slug;
    private ContentType contentType;
    private ContentStatus status;

    private UUID createdBy;
    private Instant createdAt;

    private UUID submittedBy;
    private Instant submittedAt;

    private UUID approvedBy;
    private Instant approvedAt;

    private UUID publishedBy;
    private Instant publishedAt;

    private UUID rejectedBy;
    private Instant rejectedAt;
    private String rejectionReason;

    private Instant archivedAt;

    private Boolean complianceRequired;
    private Boolean complianceApproved;
    private UUID complianceApprovedBy;
    private Instant complianceApprovedAt;

    private Instant scheduledAt;
    private UUID scheduledBy;

    public static VersionHistoryResponse from(com.allocator.contentservice.model.Content c) {
        return VersionHistoryResponse.builder()
                .id(c.getId())
                .parentContentId(c.getParentContentId())
                .versionNumber(c.getVersionNumber())
                .latestVersion(c.getLatestVersion())
                .title(c.getTitle())
                .slug(c.getSlug())
                .contentType(c.getContentType())
                .status(c.getStatus())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .submittedBy(c.getSubmittedBy())
                .submittedAt(c.getSubmittedAt())
                .approvedBy(c.getApprovedBy())
                .approvedAt(c.getApprovedAt())
                .publishedBy(c.getPublishedBy())
                .publishedAt(c.getPublishedAt())
                .rejectedBy(c.getRejectedBy())
                .rejectedAt(c.getRejectedAt())
                .rejectionReason(c.getRejectionReason())
                .archivedAt(c.getArchivedAt())
                .complianceRequired(c.getComplianceRequired())
                .complianceApproved(c.getComplianceApproved())
                .complianceApprovedBy(c.getComplianceApprovedBy())
                .complianceApprovedAt(c.getComplianceApprovedAt())
                .scheduledAt(c.getScheduledAt())
                .scheduledBy(c.getScheduledBy())
                .build();
    }
}
