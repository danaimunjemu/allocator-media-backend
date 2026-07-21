package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.AccessTier;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.dto.ReferenceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentResponse {

    private UUID id;
    private UUID brandId;
    private UUID createdBy;
    private String title;
    private String subtitle;
    private String slug;
    private String summary;
    private String body;
    private String heroImage;
    private String readTime;
    private UUID categoryId;
    private ContentType contentType;
    private List<AuthorResponse> authors;
    private Boolean anonymous;
    private UUID editorId;
    private ContentStatus status;

    // Publishing fields
    private Instant publishedAt;
    private UUID publishedBy;
    private Instant scheduledAt;

    // Workflow tracking fields
    private Instant submittedAt;
    private UUID submittedBy;
    private Instant approvedAt;
    private UUID approvedBy;
    private Instant rejectedAt;
    private UUID rejectedBy;
    private String rejectionReason;

    private Boolean featured;
    private Boolean highlighted;
    private AccessTier accessTier;
    private String metaTitle;
    private String metaDescription;
    private String canonicalUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> metadata;
    private List<String> tags;
    private List<MediaResponse> media;

    // Flattened metadata fields for frontend convenience
    private String topic;
    private String abstractText; // 'abstract' is a keyword
    private Integer pages;
    private String downloadUrl;
    private String audioUrl;
    private String spotifyUrl;
    private String applePodcastsUrl;
    private String duration;
    private String youtubeId;
    private String content; // rich text or Editor.js output
    private String excerpt;
    private List<ReferenceResponse> references;
    private String citation;
    private List<String> samplePages;

    // INTERVIEW flattened fields
    private String interviewSubject;
    private String interviewMediaUrl;

    // Versioning fields
    private Integer versionNumber;
    private UUID parentContentId;
    private Boolean latestVersion;

    // Governance fields
    private UUID scheduledBy;
    private Instant archivedAt;

    // Compliance fields
    private Boolean complianceRequired;
    private Boolean complianceApproved;
    private UUID complianceApprovedBy;
    private Instant complianceApprovedAt;

    // Paywall fields — populated server-side from the viewer's resolved tier;
    // never trust a client to supply these. When locked, `content`/`body` above
    // have already been truncated server-side to a preview, not the full text.
    private Boolean locked;
    private AccessTier requiredTier;
    private AccessTier viewerTier; // null when the viewer is anonymous

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResponse {
        private String id;
        private String url;
        private String type;
    }
}
