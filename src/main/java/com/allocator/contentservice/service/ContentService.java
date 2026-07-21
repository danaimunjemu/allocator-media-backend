package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.ContentFilter;
import com.allocator.contentservice.dto.ContentRequest;
import com.allocator.contentservice.dto.ContentResponse;
import com.allocator.contentservice.dto.VersionHistoryResponse;
import com.allocator.contentservice.mapper.ContentMapper;
import com.allocator.contentservice.model.AccessTier;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.model.Reference;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.contentservice.repository.ReferenceRepository;
import com.allocator.contentservice.repository.WorkflowAuditLogRepository;
import com.allocator.contentservice.util.RichContentTruncator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentWorkflowService workflowService;
    private final ContentMapper contentMapper;
    private final EventPublisherService eventPublisherService;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final com.allocator.contentservice.repository.AuthorRepository authorRepository;
    private final ReferenceRepository referenceRepository;
    private final com.allocator.mediaservice.service.MediaService mediaService;

    // ─── Create / Update ─────────────────────────────────────────────────────

    @Transactional
    public ContentResponse createContent(ContentRequest request, UUID userId, String userRoles) {
        validateCreationRequirements(request);
        ContentWorkflowService.UserRole role = resolveRole(userRoles);

        Content content = contentMapper.toEntity(request);
        List<UUID> targetAuthorIds = (request.getAuthorIds() != null)
                ? request.getAuthorIds()
                : Collections.emptyList();

        Content savedContent = workflowService.createDraft(content, userId, targetAuthorIds, role);
        eventPublisherService.publishContentMediaLinkedEvent(savedContent);

        log.info("Created content: {} by user: {}", savedContent.getId(), userId);
        ContentResponse response = contentMapper.toResponseEnriched(savedContent);
        return enrichMedia(response, savedContent);
    }

    @Transactional
    public ContentResponse updateContent(UUID id, ContentRequest request, UUID userId, String userRoles) {
        validateCreationRequirements(request);
        ContentWorkflowService.UserRole role = resolveRole(userRoles);

        Content existingContent = contentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + id));

        Content updatedContent = contentMapper.toEntity(request);
        updatedContent.setId(existingContent.getId());

        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            List<com.allocator.contentservice.model.Author> authors = request.getAuthorIds().stream()
                    .map(aid -> authorRepository.findById(aid)
                            .orElseThrow(() -> new IllegalArgumentException("Author not found: " + aid)))
                    .collect(Collectors.toList());
            updatedContent.setAuthors(authors);
        } else {
            updatedContent.setAuthors(existingContent.getAuthors());
        }

        // Sync references: if the request includes referenceIds, treat it as the authoritative list
        if (request.getReferenceIds() != null) {
            Set<Reference> requestedRefs = request.getReferenceIds().stream()
                    .map(rid -> referenceRepository.findById(rid).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            updatedContent.setReferences(requestedRefs);
        } else {
            // Preserve existing references when not specified (autosave path)
            updatedContent.setReferences(existingContent.getReferences());
        }

        Content savedContent = workflowService.updateDraft(id, updatedContent, userId, role);
        eventPublisherService.publishContentMediaLinkedEvent(savedContent);

        log.info("Updated content: {} by user: {}", savedContent.getId(), userId);
        ContentResponse response = contentMapper.toResponseEnriched(savedContent);
        return enrichMedia(response, savedContent);
    }

    @Transactional
    public ContentResponse changeBrand(UUID id, UUID newBrandId, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.changeBrand(id, newBrandId, userId, role);
        ContentResponse response = contentMapper.toResponseEnriched(content);
        return enrichMedia(response, content);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    // Fraction of a locked article's text revealed as a preview before the paywall gate.
    private static final double PAYWALL_PREVIEW_FRACTION = 0.30;

    @Transactional(readOnly = true)
    public ContentResponse getContent(UUID id, UUID viewerId, Optional<AccessTier> viewerTier) {
        Content content = contentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + id));

        eventPublisherService.publishArticleViewedEvent(content, viewerId);

        ContentResponse response = contentMapper.toResponseEnriched(content);
        response = enrichMedia(response, content);
        applyPaywallGating(response, content, viewerTier);
        return response;
    }

    @Transactional(readOnly = true)
    public ContentResponse getContentBySlug(String slug, Optional<AccessTier> viewerTier) {
        Content content = contentRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Content not found with slug: " + slug));

        ContentResponse response = contentMapper.toResponseEnriched(content);
        response = enrichMedia(response, content);
        applyPaywallGating(response, content, viewerTier);
        return response;
    }

    // Decides whether the viewer's tier meets the content's required tier and,
    // if not, truncates the payload server-side so the full article never
    // reaches the network response — hiding it client-side would still let a
    // developer read it straight from the network tab.
    private void applyPaywallGating(ContentResponse response, Content content, Optional<AccessTier> viewerTier) {
        AccessTier required = content.getAccessTier() != null ? content.getAccessTier() : AccessTier.FREE;
        boolean locked = required != AccessTier.FREE
                && viewerTier.map(tier -> tier.ordinal() < required.ordinal()).orElse(true);

        response.setRequiredTier(required);
        response.setViewerTier(viewerTier.orElse(null));
        response.setLocked(locked);

        if (locked) {
            response.setContent(RichContentTruncator.truncateToFraction(response.getContent(), PAYWALL_PREVIEW_FRACTION));
            response.setBody(RichContentTruncator.truncateToFraction(response.getBody(), PAYWALL_PREVIEW_FRACTION));
        }
    }

    // ─── Workflow actions ─────────────────────────────────────────────────────

    @Transactional
    public ContentResponse submitForReview(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        validateSubmitRequirements(id);
        Content content = workflowService.submitForReview(id, userId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse rejectContent(UUID id, UUID editorId, String userRoles, String reason) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.rejectContent(id, editorId, role, reason);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse approveContent(UUID id, UUID editorId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        validateApprovalRequirements(id);
        Content content = workflowService.approveContent(id, editorId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse retractApproval(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.retractApproval(id, userId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse scheduleContent(UUID id, Instant scheduledAt, UUID editorId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.scheduleContent(id, scheduledAt, editorId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse publishContent(UUID id, UUID editorId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        validatePublishRequirements(id);
        Content content = workflowService.publishContent(id, editorId, role);
        eventPublisherService.publishArticlePublishedEvent(content);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse unpublishContent(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.unpublishContent(id, userId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse archiveContent(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.archiveContent(id, userId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse restoreFromArchive(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.restoreFromArchive(id, userId, role);
        return contentMapper.toResponseEnriched(content);
    }

    // ─── Compliance ───────────────────────────────────────────────────────────

    @Transactional
    public ContentResponse approveCompliance(UUID id, UUID officerId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.approveCompliance(id, officerId, role);
        return contentMapper.toResponseEnriched(content);
    }

    @Transactional
    public ContentResponse rejectCompliance(UUID id, UUID officerId, String userRoles, String reason) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content content = workflowService.rejectCompliance(id, officerId, role, reason);
        return contentMapper.toResponseEnriched(content);
    }

    // ─── Versioning ───────────────────────────────────────────────────────────

    @Transactional
    public ContentResponse createContentRevision(UUID id, UUID userId, String userRoles) {
        ContentWorkflowService.UserRole role = resolveRole(userRoles);
        Content newRevision = workflowService.createContentRevision(id, userId, role);
        return contentMapper.toResponseEnriched(newRevision);
    }

    @Transactional(readOnly = true)
    public List<ContentResponse> getVersionHistory(UUID id) {
        return workflowService.getVersionHistory(id).stream()
                .map(contentMapper::toResponseEnriched)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VersionHistoryResponse> getVersionHistoryRich(UUID id) {
        return workflowService.getVersionHistory(id).stream()
                .map(VersionHistoryResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

    // ─── Audit trail ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<com.allocator.contentservice.model.WorkflowAuditLog> getWorkflowAuditTrail(UUID contentId) {
        return auditLogRepository.findByContentIdOrderByTimestampAsc(contentId);
    }

    // ─── List / search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ContentResponse> listContent(ContentFilter filter, Pageable pageable) {
        Specification<Content> spec = Specification.where(null);

        if (filter.getBrandId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("brandId"), filter.getBrandId()));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getContentType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("contentType"), filter.getContentType()));
        }
        if (filter.getCategoryId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoryId"), filter.getCategoryId()));
        }
        if (filter.getFeatured() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("featured"), filter.getFeatured()));
        }
        if (filter.getHighlighted() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("highlighted"), filter.getHighlighted()));
        }
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            String pattern = "%" + filter.getKeyword().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.upper(root.get("title")), pattern.toUpperCase()),
                    cb.like(cb.upper(root.get("summary")), pattern.toUpperCase())
            ));
        }
        if (filter.getTags() != null && !filter.getTags().isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return root.join("tags").in(filter.getTags());
            });
        }
        if (filter.getCreatedFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedFrom()));
        }
        if (filter.getCreatedTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), filter.getCreatedTo()));
        }

        Page<Content> contentPage = contentRepository.findAll(spec, pageable);
        return contentPage.map(contentMapper::toResponseEnriched);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> listContentByBrand(UUID brandId, Pageable pageable) {
        return contentRepository.findByBrandId(brandId, pageable)
                .map(contentMapper::toResponseEnriched);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse> listContentByStatus(ContentStatus status, Pageable pageable) {
        return contentRepository.findByStatus(status, pageable)
                .map(contentMapper::toResponseEnriched);
    }

    // ─── Stage-aware validation ───────────────────────────────────────────────

    private void validateCreationRequirements(ContentRequest request) {
        if (request.getContentType() == null) {
            throw new IllegalArgumentException("contentType is required");
        }
        Map<String, Object> metadata = request.getMetadata();
        switch (request.getContentType()) {
            case ARTICLE -> {
                requireMetadata(metadata, "ARTICLE");
                requireMetadataField(metadata, "topic", "ARTICLE");
                requireMetadataField(metadata, "content", "ARTICLE");
            }
            case RESEARCH -> {
                requireMetadata(metadata, "RESEARCH");
                requireMetadataField(metadata, "abstract", "RESEARCH");
                requireMetadataField(metadata, "downloadUrl", "RESEARCH");
                requireNumericMetadataField(metadata, "pages", "RESEARCH");
            }
            case PODCAST -> {
                requireMetadata(metadata, "PODCAST");
                requireAnyMetadataField(metadata, List.of("spotifyUrl", "applePodcastsUrl"), "PODCAST");
                requireNumericMetadataField(metadata, "episodeNumber", "PODCAST");
                requireMetadataField(metadata, "duration", "PODCAST");
            }
            case VIDEO -> {
                requireMetadata(metadata, "VIDEO");
                requireMetadataField(metadata, "youtubeId", "VIDEO");
            }
            case INTERVIEW -> {
                requireMetadata(metadata, "INTERVIEW");
                requireMetadataField(metadata, "interviewSubject", "INTERVIEW");
                // Either a transcript or a media URL must be present
                boolean hasTranscript = metadata.containsKey("transcript") && metadata.get("transcript") != null
                        && !metadata.get("transcript").toString().isBlank();
                boolean hasMediaUrl = metadata.containsKey("mediaUrl") && metadata.get("mediaUrl") != null
                        && !metadata.get("mediaUrl").toString().isBlank();
                if (!hasTranscript && !hasMediaUrl) {
                    throw new IllegalArgumentException("INTERVIEW content requires either 'transcript' or 'mediaUrl' in metadata");
                }
            }
        }
    }

    private void validateSubmitRequirements(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        if (content.getTitle() == null || content.getTitle().isBlank()) {
            throw new IllegalArgumentException("Cannot submit for review: title is required");
        }
        if (content.getExcerpt() == null || content.getExcerpt().isBlank()) {
            throw new IllegalArgumentException("Cannot submit for review: excerpt is required");
        }

        // Ensure required metadata fields are still present (they could have been cleared after initial save)
        if (content.getMetadata() != null) {
            validateMetadataForType(content.getContentType(), content.getMetadata(), "submit for review");
        }
    }

    private void validateApprovalRequirements(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        // RESEARCH requires disclosure field before approval
        if (content.getContentType() == ContentType.RESEARCH) {
            Map<String, Object> metadata = content.getMetadata();
            if (metadata == null || !metadata.containsKey("disclosure") || metadata.get("disclosure") == null
                    || metadata.get("disclosure").toString().isBlank()) {
                throw new IllegalArgumentException(
                        "Cannot approve RESEARCH content without a 'disclosure' field in metadata");
            }
        }
    }

    private void validatePublishRequirements(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        if (content.getContentType() == ContentType.ARTICLE || content.getContentType() == ContentType.INTERVIEW) {
            boolean hasBody = content.getBody() != null && !content.getBody().isBlank();
            Map<String, Object> meta = content.getMetadata();
            boolean hasMetaContent = meta != null && meta.containsKey("content")
                    && meta.get("content") != null && !meta.get("content").toString().isBlank();
            if (!hasBody && !hasMetaContent) {
                throw new IllegalArgumentException("Cannot publish: body/content is required before publication");
            }
        }
    }

    private void validateMetadataForType(ContentType type, Map<String, Object> metadata, String stage) {
        if (type == null) return;
        switch (type) {
            case ARTICLE -> requireMetadataField(metadata, "topic", "ARTICLE @ " + stage);
            case RESEARCH -> {
                requireMetadataField(metadata, "abstract", "RESEARCH @ " + stage);
                requireMetadataField(metadata, "downloadUrl", "RESEARCH @ " + stage);
            }
            case PODCAST -> requireAnyMetadataField(metadata, List.of("spotifyUrl", "applePodcastsUrl"), "PODCAST @ " + stage);
            case VIDEO -> requireMetadataField(metadata, "youtubeId", "VIDEO @ " + stage);
            default -> { /* INTERVIEW handled at creation */ }
        }
    }

    // ─── Role resolution ──────────────────────────────────────────────────────

    private ContentWorkflowService.UserRole resolveRole(String roles) {
        if (roles == null || roles.isEmpty()) {
            return ContentWorkflowService.UserRole.READER;
        }

        String[] roleArray = roles.split(",");
        ContentWorkflowService.UserRole highest = ContentWorkflowService.UserRole.READER;

        for (String roleStr : roleArray) {
            String sanitized = roleStr.trim().toUpperCase();
            if (sanitized.startsWith("ROLE_")) {
                sanitized = sanitized.substring(5);
            }
            try {
                ContentWorkflowService.UserRole role = ContentWorkflowService.UserRole.valueOf(sanitized);
                if (getRolePriority(role) > getRolePriority(highest)) {
                    highest = role;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Unknown role in X-User-Roles header: {}", sanitized);
            }
        }

        log.debug("Resolved role: {} from header: {}", highest, roles);
        return highest;
    }

    private int getRolePriority(ContentWorkflowService.UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> 100;
            case ADMIN -> 90;
            case EDITOR -> 80;
            case COMPLIANCE_OFFICER -> 75;
            case ANALYST -> 50;
            case SUPPORT -> 40;
            case READER -> 10;
        };
    }

    // ─── Metadata helpers ─────────────────────────────────────────────────────

    private void requireMetadata(Map<String, Object> metadata, String type) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata is required for " + type + " content type");
        }
    }

    private void requireMetadataField(Map<String, Object> metadata, String field, String context) {
        if (!metadata.containsKey(field) || metadata.get(field) == null
                || metadata.get(field).toString().trim().isEmpty()) {
            throw new IllegalArgumentException("metadata '" + field + "' is required for " + context);
        }
    }

    private void requireNumericMetadataField(Map<String, Object> metadata, String field, String context) {
        requireMetadataField(metadata, field, context);
        try {
            Double.parseDouble(metadata.get(field).toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("metadata '" + field + "' must be a number for " + context);
        }
    }

    private void requireAnyMetadataField(Map<String, Object> metadata, List<String> fields, String context) {
        boolean present = fields.stream().anyMatch(field -> metadata.containsKey(field) && metadata.get(field) != null
                && !metadata.get(field).toString().trim().isEmpty());
        if (!present) {
            throw new IllegalArgumentException("metadata requires one of " + fields + " for " + context);
        }
    }

    // ─── Media enrichment ─────────────────────────────────────────────────────

    private ContentResponse enrichMedia(ContentResponse response, Content content) {
        if (content.getMediaIds() == null || content.getMediaIds().isEmpty()) {
            return response;
        }

        List<ContentResponse.MediaResponse> mediaDetails = content.getMediaIds().stream()
                .map(idStr -> {
                    try {
                        UUID mediaId = UUID.fromString(idStr);
                        com.allocator.mediaservice.dto.MediaResponse media = mediaService.getMedia(mediaId);
                        return ContentResponse.MediaResponse.builder()
                                .id(idStr)
                                .url(media.getStorageUrl())
                                .type(media.getContentType().name())
                                .build();
                    } catch (Exception e) {
                        log.error("Error fetching media details for id: {}", idStr, e);
                        return ContentResponse.MediaResponse.builder()
                                .id(idStr)
                                .url("https://via.placeholder.com/150?text=Media+Unavailable")
                                .type("IMAGE")
                                .build();
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        response.setMedia(mediaDetails);
        return response;
    }
}
