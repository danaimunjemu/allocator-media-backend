package com.allocator.contentservice.service;

import com.allocator.contentservice.model.BrandRole;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentRevision;
import com.allocator.contentservice.model.ContentStatus;
import com.allocator.contentservice.model.WorkflowAuditLog;
import com.allocator.contentservice.model.WorkflowEventType;
import com.allocator.contentservice.repository.BrandAssignmentRepository;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.contentservice.repository.ContentRevisionRepository;
import com.allocator.contentservice.repository.WorkflowAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentWorkflowService {

    private final ContentRepository contentRepository;
    private final ContentRevisionRepository contentRevisionRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final BrandAssignmentRepository brandAssignmentRepository;
    private final ContentEventPublisher eventPublisher;
    private final SlugService slugService;
    private final com.allocator.contentservice.repository.AuthorRepository authorRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowTransitionPolicy transitionPolicy;
    private final CompliancePolicy compliancePolicy;
    private final Counter contentCreatedCounter;
    private final Counter contentPublishedCounter;
    private final Counter contentScheduledCounter;
    private final Counter contentFailedWorkflowCounter;

    // ─── Global role enum ─────────────────────────────────────────────────────

    /**
     * Global roles sourced from the auth-service JWT / X-User-Roles header.
     * ADMIN and SUPER_ADMIN bypass all brand-assignment checks.
     */
    public enum UserRole {
        EDITOR,
        ADMIN,
        SUPER_ADMIN,
        ANALYST,
        SUPPORT,
        READER,
        COMPLIANCE_OFFICER
    }

    // ─── Draft ───────────────────────────────────────────────────────────────

    @Transactional
    public Content createDraft(Content content, UUID createdByUserId, List<UUID> authorIds, UserRole userRole) {
        try {
            MDC.put("traceId", UUID.randomUUID().toString());
            MDC.put("userId", createdByUserId != null ? createdByUserId.toString() : "unknown");
            MDC.put("brandId", content.getBrandId().toString());

            if (content.getSlug() == null || content.getSlug().trim().isEmpty()) {
                content.setSlug(slugService.generateSlug(content.getTitle()));
            }

            // Set compliance required flag from policy
            content.setComplianceRequired(compliancePolicy.requiresCompliance(content.getContentType()));

            content.setStatus(ContentStatus.DRAFT);
            content.setCreatedBy(createdByUserId);
            content.setCreatedAt(Instant.now());

            List<com.allocator.contentservice.model.Author> authors = authorIds.stream()
                    .map(id -> authorRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Author not found: " + id)))
                    .collect(Collectors.toList());
            content.setAuthors(authors);

            Content savedContent = contentRepository.save(content);
            MDC.put("contentId", savedContent.getId().toString());

            createRevision(savedContent, createdByUserId, "Initial draft created");
            recordAudit(savedContent, WorkflowEventType.CREATED, createdByUserId, userRole.name(),
                    null, ContentStatus.DRAFT, null);
            eventPublisher.publishContentCreated(savedContent, createdByUserId, userRole.name());

            contentCreatedCounter.increment();
            log.info("Created draft content: {} by user: {}", savedContent.getId(), createdByUserId);
            return savedContent;
        } catch (Exception e) {
            contentFailedWorkflowCounter.increment();
            log.error("Failed to create draft content", e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @Transactional
    public Content updateDraft(UUID contentId, Content updatedContent, UUID userId, UserRole userRole) {
        Content existing = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireStatus(existing, "update", ContentStatus.DRAFT);
        validateOwnership(existing, userId, userRole);

        updateContentFields(existing, updatedContent);
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy(userId);

        // Refresh compliance required flag if contentType changed
        existing.setComplianceRequired(compliancePolicy.requiresCompliance(existing.getContentType()));

        Content saved = contentRepository.save(existing);

        createRevision(saved, userId, "Draft updated");
        recordAudit(saved, WorkflowEventType.UPDATED, userId, userRole.name(),
                ContentStatus.DRAFT, ContentStatus.DRAFT, null);
        eventPublisher.publishContentUpdated(saved, userId, userRole.name());

        log.info("Updated draft content: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Moves content to a different brand, regardless of its current workflow status —
     * unlike {@link #updateDraft}, this is intentionally not restricted to DRAFT, since
     * re-homing published content under the correct brand is a legitimate admin action
     * (e.g. correcting a mistaken brand assignment after publication). Restricted to
     * global ADMIN/SUPER_ADMIN since it reassigns ownership across brands rather than
     * editing within one.
     */
    @Transactional
    public Content changeBrand(UUID contentId, UUID newBrandId, UUID userId, UserRole userRole) {
        if (!isGlobalAdmin(userRole)) {
            throw new IllegalStateException(
                    "Changing a content item's brand requires a global ADMIN or SUPER_ADMIN role.");
        }

        Content existing = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        UUID oldBrandId = existing.getBrandId();
        if (oldBrandId.equals(newBrandId)) {
            return existing;
        }

        existing.setBrandId(newBrandId);
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy(userId);

        Content saved = contentRepository.save(existing);

        recordAudit(saved, WorkflowEventType.BRAND_CHANGED, userId, userRole.name(),
                saved.getStatus(), saved.getStatus(),
                "Brand changed from " + oldBrandId + " to " + newBrandId);

        log.info("Changed brand for content {} from {} to {} by user {}",
                contentId, oldBrandId, newBrandId, userId);
        return saved;
    }

    // ─── DRAFT → REVIEW ──────────────────────────────────────────────────────

    @Transactional
    public Content submitForReview(UUID contentId, UUID userId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireMinimumRole(userRole, UserRole.EDITOR, "submit for review");
        validateBrandRole(userId, content.getBrandId(), BrandRole.AUTHOR, userRole);

        ContentStatus prev = content.getStatus();
        transitionPolicy.assertTransitionAllowed(prev, ContentStatus.REVIEW);

        validateOwnership(content, userId, userRole);

        content.setStatus(ContentStatus.REVIEW);
        content.setSubmittedAt(Instant.now());
        content.setSubmittedBy(userId);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(userId);

        Content saved = contentRepository.save(content);
        createRevision(saved, userId, "Submitted for review");
        recordAudit(saved, WorkflowEventType.SUBMITTED, userId, userRole.name(), prev, ContentStatus.REVIEW, null);
        eventPublisher.publishContentSubmitted(saved, userId, userRole.name());

        log.info("Submitted content for review: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    // ─── REVIEW → DRAFT (editorial rejection) ────────────────────────────────

    @Transactional
    public Content rejectContent(UUID contentId, UUID editorId, UserRole userRole, String reason) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireMinimumRole(userRole, UserRole.EDITOR, "reject content");
        validateBrandRole(editorId, content.getBrandId(), BrandRole.EDITOR, userRole);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }

        requireStatus(content, "reject content", ContentStatus.REVIEW);
        ContentStatus prev = content.getStatus();
        transitionPolicy.assertTransitionAllowed(prev, ContentStatus.DRAFT);

        content.setStatus(ContentStatus.DRAFT);
        content.setRejectedAt(Instant.now());
        content.setRejectedBy(editorId);
        content.setRejectionReason(reason);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(editorId);

        Content saved = contentRepository.save(content);
        createRevision(saved, editorId, "Rejected: " + reason);
        recordAudit(saved, WorkflowEventType.RETURNED, editorId, userRole.name(), prev, ContentStatus.DRAFT, reason);
        eventPublisher.publishContentRejected(saved, editorId, userRole.name(), reason);

        log.info("Rejected content: {} by editor: {} — reason: {}", saved.getId(), editorId, reason);
        return saved;
    }

    // ─── REVIEW → APPROVED ───────────────────────────────────────────────────

    @Transactional
    public Content approveContent(UUID contentId, UUID editorId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireMinimumRole(userRole, UserRole.EDITOR, "approve content");
        validateBrandRole(editorId, content.getBrandId(), BrandRole.EDITOR, userRole);

        // Four-eyes principle does not apply to SUPER_ADMIN: they may create, review,
        // approve, and publish the same piece of content.
        if (!isSuperAdmin(userRole)) {
            // Four-eyes: approver must not be the content creator
            if (editorId != null && editorId.equals(content.getCreatedBy())) {
                throw new IllegalStateException(
                        "Four-eyes violation: the content creator cannot approve their own submission. " +
                        "A different editor must perform the review.");
            }
            // Four-eyes: approver must not be the original submitter (if different from creator)
            if (editorId != null && editorId.equals(content.getSubmittedBy())) {
                throw new IllegalStateException(
                        "Four-eyes violation: the user who submitted this content cannot approve it.");
            }
        }

        ContentStatus prev = content.getStatus();
        transitionPolicy.assertTransitionAllowed(prev, ContentStatus.APPROVED);

        content.setStatus(ContentStatus.APPROVED);
        content.setApprovedAt(Instant.now());
        content.setApprovedBy(editorId);
        content.setEditorId(editorId);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(editorId);

        Content saved = contentRepository.save(content);
        createRevision(saved, editorId, "Content approved");
        recordAudit(saved, WorkflowEventType.APPROVED, editorId, userRole.name(), prev, ContentStatus.APPROVED, null);
        eventPublisher.publishContentApproved(saved, editorId, userRole.name());

        log.info("Approved content: {} by editor: {}", saved.getId(), editorId);
        return saved;
    }

    // ─── APPROVED → COMPLIANCE_APPROVED (compliance-sensitive types) ──────────

    @Transactional
    public Content approveCompliance(UUID contentId, UUID officerId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        if (userRole != UserRole.COMPLIANCE_OFFICER && getRolePriority(userRole) < getRolePriority(UserRole.ADMIN)) {
            throw new IllegalStateException("Only COMPLIANCE_OFFICER or ADMIN can approve compliance");
        }
        if (!compliancePolicy.requiresCompliance(content.getContentType())) {
            throw new IllegalStateException(
                    "Compliance approval is only required for RESEARCH and INTERVIEW content types. " +
                    "Current type: " + content.getContentType());
        }
        if (content.getStatus() != ContentStatus.APPROVED) {
            throw new IllegalStateException(
                    "Content must be in APPROVED status before compliance review. Current: " + content.getStatus());
        }

        ContentStatus prev = content.getStatus();
        content.setStatus(ContentStatus.COMPLIANCE_APPROVED);
        content.setComplianceApproved(true);
        content.setComplianceApprovedBy(officerId);
        content.setComplianceApprovedAt(Instant.now());
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(officerId);

        Content saved = contentRepository.save(content);
        createRevision(saved, officerId, "Compliance approved — ready for publication");
        recordAudit(saved, WorkflowEventType.COMPLIANCE_APPROVED, officerId, userRole.name(),
                prev, ContentStatus.COMPLIANCE_APPROVED, null);

        log.info("Compliance approved for content: {} by officer: {}", saved.getId(), officerId);
        return saved;
    }

    @Transactional
    public Content rejectCompliance(UUID contentId, UUID officerId, UserRole userRole, String reason) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        if (userRole != UserRole.COMPLIANCE_OFFICER && getRolePriority(userRole) < getRolePriority(UserRole.ADMIN)) {
            throw new IllegalStateException("Only COMPLIANCE_OFFICER or ADMIN can reject compliance");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A compliance rejection reason is required");
        }
        if (content.getStatus() != ContentStatus.APPROVED && content.getStatus() != ContentStatus.COMPLIANCE_APPROVED) {
            throw new IllegalStateException(
                    "Compliance rejection requires content in APPROVED or COMPLIANCE_APPROVED status. Current: " + content.getStatus());
        }

        content.setComplianceApproved(false);
        content.setComplianceApprovedBy(null);
        content.setComplianceApprovedAt(null);
        ContentStatus prev = content.getStatus();
        content.setStatus(ContentStatus.DRAFT);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(officerId);

        Content saved = contentRepository.save(content);
        createRevision(saved, officerId, "Compliance rejected: " + reason);
        recordAudit(saved, WorkflowEventType.COMPLIANCE_REJECTED, officerId, userRole.name(),
                prev, ContentStatus.DRAFT, reason);

        log.info("Compliance rejected for content: {} by officer: {} — reason: {}", saved.getId(), officerId, reason);
        return saved;
    }

    // ─── APPROVED → set scheduledAt ──────────────────────────────────────────

    @Transactional
    public Content scheduleContent(UUID contentId, Instant scheduledAt, UUID editorId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        // Only PUBLISHER or SENIOR_EDITOR brand role may schedule publication
        requirePublishingAuthority(editorId, content.getBrandId(), userRole, "schedule content");

        ContentStatus requiredStatus = compliancePolicy.requiresCompliance(content.getContentType())
                ? ContentStatus.COMPLIANCE_APPROVED
                : ContentStatus.APPROVED;
        requireStatus(content, "schedule", requiredStatus);

        if (scheduledAt == null || scheduledAt.isBefore(Instant.now())) {
            throw new IllegalArgumentException("scheduledAt must be a future timestamp");
        }

        boolean isReschedule = content.getScheduledAt() != null;
        content.setScheduledAt(scheduledAt);
        content.setScheduledBy(editorId);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(editorId);

        Content saved = contentRepository.save(content);
        String summary = isReschedule
                ? "Rescheduled for publishing at: " + scheduledAt
                : "Scheduled for publishing at: " + scheduledAt;
        createRevision(saved, editorId, summary);
        recordAudit(saved, WorkflowEventType.SCHEDULED, editorId, userRole.name(),
                saved.getStatus(), saved.getStatus(), "Publish at: " + scheduledAt);
        eventPublisher.publishContentScheduled(saved, editorId, userRole.name());

        contentScheduledCounter.increment();
        log.info("Scheduled content: {} for: {} by editor: {}", saved.getId(), scheduledAt, editorId);
        return saved;
    }

    // ─── APPROVED / COMPLIANCE_APPROVED → PUBLISHED ──────────────────────────

    @Transactional
    public Content publishContent(UUID contentId, UUID publisherId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        // Publishing authority: requires PUBLISHER or SENIOR_EDITOR brand role
        requirePublishingAuthority(publisherId, content.getBrandId(), userRole, "publish content");

        // Determine required pre-publish status based on compliance policy
        boolean needsCompliance = compliancePolicy.requiresCompliance(content.getContentType());
        ContentStatus requiredStatus = needsCompliance
                ? ContentStatus.COMPLIANCE_APPROVED
                : ContentStatus.APPROVED;

        if (content.getStatus() != requiredStatus) {
            String hint = needsCompliance
                    ? "This content requires compliance approval (COMPLIANCE_APPROVED status) before publication."
                    : "Content must be in APPROVED status before publication.";
            throw new IllegalStateException(
                    "Cannot publish content with status " + content.getStatus() + ". " + hint);
        }

        // ── Four-eyes principle ────────────────────────────────────────────────
        // Publisher must differ from creator, approver, and compliance approver.
        // Does not apply to SUPER_ADMIN, who may publish content they created/approved themselves.
        if (!isSuperAdmin(userRole)) {
            if (publisherId != null && publisherId.equals(content.getCreatedBy())) {
                throw new IllegalStateException(
                        "Four-eyes violation: the content creator cannot publish their own content.");
            }
            if (publisherId != null && publisherId.equals(content.getApprovedBy())) {
                throw new IllegalStateException(
                        "Four-eyes violation: the editor who approved this content cannot also publish it.");
            }
            if (publisherId != null && publisherId.equals(content.getComplianceApprovedBy())) {
                throw new IllegalStateException(
                        "Four-eyes violation: the compliance officer who approved this content cannot also publish it.");
            }
        }

        ContentStatus prev = content.getStatus();
        content.setStatus(ContentStatus.PUBLISHED);
        content.setPublishedAt(Instant.now());
        content.setPublishedBy(publisherId);
        content.setEditorId(publisherId);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(publisherId);

        Content saved = contentRepository.save(content);

        // ── Auto-archive previous published versions in this group ─────────────
        archivePreviousVersions(saved);

        createRevision(saved, publisherId, "Content published");
        recordAudit(saved, WorkflowEventType.PUBLISHED, publisherId, userRole.name(),
                prev, ContentStatus.PUBLISHED, null);
        eventPublisher.publishContentPublished(saved, publisherId, userRole.name());
        eventPublisher.publishArticlePublished(saved);

        contentPublishedCounter.increment();
        log.info("Published content: {} by publisher: {}", saved.getId(), publisherId);
        return saved;
    }

    /**
     * Finds all other PUBLISHED versions in the same version group and archives them.
     * Called immediately after a new version reaches PUBLISHED status.
     */
    private void archivePreviousVersions(Content newlyPublished) {
        UUID rootId = newlyPublished.getParentContentId() != null
                ? newlyPublished.getParentContentId()
                : newlyPublished.getId();

        List<Content> group = contentRepository.findVersionGroup(rootId);
        UUID systemActor = UUID.fromString("00000000-0000-0000-0000-000000000001");

        for (Content sibling : group) {
            if (sibling.getId().equals(newlyPublished.getId())) continue;
            if (sibling.getStatus() == ContentStatus.PUBLISHED) {
                sibling.setStatus(ContentStatus.ARCHIVED);
                sibling.setArchivedAt(Instant.now());
                sibling.setUpdatedAt(Instant.now());
                sibling.setUpdatedBy(newlyPublished.getPublishedBy());
                contentRepository.save(sibling);
                createRevision(sibling, systemActor,
                        "Auto-archived: superseded by v" + newlyPublished.getVersionNumber());
                recordAudit(sibling, WorkflowEventType.VERSION_ARCHIVED, newlyPublished.getPublishedBy(),
                        "SYSTEM", ContentStatus.PUBLISHED, ContentStatus.ARCHIVED,
                        "Superseded by content " + newlyPublished.getId() + " v" + newlyPublished.getVersionNumber());
                log.info("Auto-archived version {} (id={}) superseded by v{}",
                        sibling.getVersionNumber(), sibling.getId(), newlyPublished.getVersionNumber());
            }
        }
    }

    // ─── PUBLISHED → DRAFT (unpublish) ───────────────────────────────────────

    @Transactional
    public Content unpublishContent(UUID contentId, UUID userId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requirePublishingAuthority(userId, content.getBrandId(), userRole, "unpublish content");
        requireStatus(content, "unpublish", ContentStatus.PUBLISHED);

        ContentStatus prev = content.getStatus();
        content.setStatus(ContentStatus.DRAFT);
        content.setPublishedAt(null);
        content.setPublishedBy(null);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(userId);

        Content saved = contentRepository.save(content);
        createRevision(saved, userId, "Content unpublished — returned to draft");
        recordAudit(saved, WorkflowEventType.UNPUBLISHED, userId, userRole.name(), prev, ContentStatus.DRAFT, null);
        eventPublisher.publishContentUnpublished(saved, userId, userRole.name());

        log.info("Unpublished content: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    // ─── APPROVED → DRAFT (retract approval) ─────────────────────────────────

    @Transactional
    public Content retractApproval(UUID contentId, UUID userId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireMinimumRole(userRole, UserRole.EDITOR, "retract approval");
        validateBrandRole(userId, content.getBrandId(), BrandRole.SENIOR_EDITOR, userRole);

        ContentStatus prev = content.getStatus();
        if (prev != ContentStatus.APPROVED && prev != ContentStatus.COMPLIANCE_APPROVED) {
            throw new IllegalStateException(
                    "Can only retract approval from APPROVED or COMPLIANCE_APPROVED content. Current: " + prev);
        }

        content.setStatus(ContentStatus.DRAFT);
        content.setApprovedAt(null);
        content.setApprovedBy(null);
        content.setScheduledAt(null);
        content.setScheduledBy(null);
        content.setComplianceApproved(null);
        content.setComplianceApprovedBy(null);
        content.setComplianceApprovedAt(null);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(userId);

        Content saved = contentRepository.save(content);
        createRevision(saved, userId, "Approval retracted — returned to draft");
        recordAudit(saved, WorkflowEventType.APPROVAL_RETRACTED, userId, userRole.name(),
                prev, ContentStatus.DRAFT, "Approval retracted");

        log.info("Retracted approval for content: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    // ─── ANY → ARCHIVED ──────────────────────────────────────────────────────

    @Transactional
    public Content archiveContent(UUID contentId, UUID userId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        // SENIOR_EDITOR or above may archive
        requireMinimumRole(userRole, UserRole.EDITOR, "archive content");
        validateBrandRole(userId, content.getBrandId(), BrandRole.SENIOR_EDITOR, userRole);

        if (content.getStatus() == ContentStatus.ARCHIVED) {
            throw new IllegalStateException("Content is already archived");
        }

        ContentStatus prev = content.getStatus();
        content.setStatus(ContentStatus.ARCHIVED);
        content.setArchivedAt(Instant.now());
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(userId);

        Content saved = contentRepository.save(content);
        createRevision(saved, userId, "Content archived");
        recordAudit(saved, WorkflowEventType.ARCHIVED, userId, userRole.name(), prev, ContentStatus.ARCHIVED, null);
        eventPublisher.publishContentArchived(saved, userId, userRole.name());

        log.info("Archived content: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    // ─── ARCHIVED → DRAFT (restore) ──────────────────────────────────────────

    @Transactional
    public Content restoreFromArchive(UUID contentId, UUID userId, UserRole userRole) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        requireMinimumRole(userRole, UserRole.ADMIN, "restore from archive");
        requireStatus(content, "restore", ContentStatus.ARCHIVED);

        content.setStatus(ContentStatus.DRAFT);
        content.setArchivedAt(null);
        content.setUpdatedAt(Instant.now());
        content.setUpdatedBy(userId);

        Content saved = contentRepository.save(content);
        createRevision(saved, userId, "Content restored from archive");
        recordAudit(saved, WorkflowEventType.RESTORED, userId, userRole.name(),
                ContentStatus.ARCHIVED, ContentStatus.DRAFT, null);
        eventPublisher.publishContentRestored(saved, userId, userRole.name());

        log.info("Restored content from archive: {} by user: {}", saved.getId(), userId);
        return saved;
    }

    // ─── Auto-publish scheduled content ──────────────────────────────────────

    @Transactional
    public List<Content> publishScheduledContent() {
        List<Content> due = contentRepository.findScheduledContentToPublish(Instant.now());
        List<Content> published = new ArrayList<>();

        for (Content content : due) {
            if (publishAsSystem(content, "Scheduled auto-publish. Originally scheduled by: " + content.getScheduledBy())) {
                published.add(content);
            }
        }

        return published;
    }

    // Used when some other workflow (e.g. sending a newsletter that links to
    // still-unpublished content) needs to force-publish a specific piece of
    // content immediately as a system action, bypassing the four-eyes/publishing
    // authority checks in publishContent() — those exist to protect the manual
    // publish button, not this kind of system-initiated bulk trigger. The
    // compliance-approval gate is still respected (never bypassed).
    @Transactional
    public boolean publishContentNow(UUID contentId, String note) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        if (content.getStatus() == ContentStatus.PUBLISHED) {
            return false;
        }
        return publishAsSystem(content, note);
    }

    private boolean publishAsSystem(Content content, String note) {
        UUID systemActor = UUID.fromString("00000000-0000-0000-0000-000000000001");
        try {
            // Skip compliance-required content that hasn't been compliance-approved
            if (Boolean.TRUE.equals(content.getComplianceRequired())
                    && content.getStatus() != ContentStatus.COMPLIANCE_APPROVED) {
                log.warn("Skipping system publish for content {} — compliance not approved", content.getId());
                recordAudit(content, WorkflowEventType.SCHEDULE_FAILED, systemActor, "SYSTEM",
                        content.getStatus(), content.getStatus(),
                        "System publish skipped: compliance approval required");
                return false;
            }

            ContentStatus prev = content.getStatus();
            content.setStatus(ContentStatus.PUBLISHED);
            content.setPublishedAt(Instant.now());
            content.setPublishedBy(systemActor);
            content.setUpdatedAt(Instant.now());
            content.setUpdatedBy(systemActor);

            contentRepository.save(content);
            archivePreviousVersions(content);
            createRevision(content, systemActor, note);
            recordAudit(content, WorkflowEventType.PUBLISHED, systemActor, "SYSTEM", prev, ContentStatus.PUBLISHED, note);
            eventPublisher.publishContentPublished(content, systemActor, "SYSTEM");
            eventPublisher.publishArticlePublished(content);

            log.info("System-published content: {}", content.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to system-publish content: {}", content.getId(), e);
            recordAudit(content, WorkflowEventType.SCHEDULE_FAILED, systemActor, "SYSTEM",
                    content.getStatus(), content.getStatus(), "System publish failed: " + e.getMessage());
            contentFailedWorkflowCounter.increment();
            return false;
        }
    }

    // ─── Versioning ───────────────────────────────────────────────────────────

    @Transactional
    public Content createContentRevision(UUID contentId, UUID userId, UserRole userRole) {
        requireMinimumRole(userRole, UserRole.EDITOR, "create content revision");

        Content original = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));

        validateBrandRole(userId, original.getBrandId(), BrandRole.EDITOR, userRole);

        if (original.getStatus() != ContentStatus.PUBLISHED && original.getStatus() != ContentStatus.APPROVED) {
            throw new IllegalStateException(
                    "A new revision can only be created from PUBLISHED or APPROVED content. " +
                    "Current status: " + original.getStatus());
        }

        UUID rootId = original.getParentContentId() != null ? original.getParentContentId() : original.getId();

        int nextVersion = contentRepository.findMaxVersionInGroup(rootId)
                .map(v -> v + 1)
                .orElse(original.getVersionNumber() + 1);

        Content newRevision = Content.builder()
                .brandId(original.getBrandId())
                .title(original.getTitle())
                .subtitle(original.getSubtitle())
                .slug(slugService.generateSlug(original.getTitle() + "-v" + nextVersion))
                .summary(original.getSummary())
                .body(original.getBody())
                .heroImageUrl(original.getHeroImageUrl())
                .readTime(original.getReadTime())
                .contentType(original.getContentType())
                .authors(new ArrayList<>(original.getAuthors()))
                .anonymous(original.getAnonymous())
                .accessTier(original.getAccessTier())
                .metaTitle(original.getMetaTitle())
                .metaDescription(original.getMetaDescription())
                .canonicalUrl(original.getCanonicalUrl())
                .metadata(original.getMetadata() != null
                        ? new java.util.LinkedHashMap<>(original.getMetadata()) : null)
                .tags(original.getTags() != null ? new ArrayList<>(original.getTags()) : null)
                .excerpt(original.getExcerpt())
                .status(ContentStatus.DRAFT)
                .versionNumber(nextVersion)
                .parentContentId(rootId)
                .latestVersion(true)
                .complianceRequired(compliancePolicy.requiresCompliance(original.getContentType()))
                .build();

        newRevision.setCreatedBy(userId);
        newRevision.setCreatedAt(Instant.now());

        original.setLatestVersion(false);
        contentRepository.save(original);

        Content saved = contentRepository.save(newRevision);
        createRevision(saved, userId, "New revision v" + nextVersion + " created from v" + original.getVersionNumber());
        recordAudit(saved, WorkflowEventType.VERSION_CREATED, userId, userRole.name(),
                null, ContentStatus.DRAFT, "Revision of content " + contentId + " (v" + original.getVersionNumber() + ")");
        eventPublisher.publishContentCreated(saved, userId, userRole.name());

        log.info("Created content revision v{} for: {} → new id: {}", nextVersion, contentId, saved.getId());
        return saved;
    }

    public List<Content> getVersionHistory(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        UUID rootId = content.getParentContentId() != null ? content.getParentContentId() : content.getId();
        return contentRepository.findVersionGroup(rootId);
    }

    // ─── Brand and role enforcement ───────────────────────────────────────────

    /**
     * Publishing operations (publish, schedule, unpublish) require the user to hold
     * PUBLISHER or SENIOR_EDITOR brand role, or a global ADMIN/SUPER_ADMIN role.
     */
    private void requirePublishingAuthority(UUID userId, UUID brandId, UserRole globalRole, String action) {
        if (isGlobalAdmin(globalRole)) return;

        Optional<com.allocator.contentservice.model.BrandAssignment> assignment =
                brandAssignmentRepository.findByUserIdAndBrandId(userId, brandId);

        if (assignment.isEmpty()) {
            throw new IllegalStateException(
                    "Access denied: user has no brand assignment for brand " + brandId +
                    ". Cannot " + action + ".");
        }

        BrandRole brandRole = BrandRole.fromString(assignment.get().getRole());
        if (brandRole == null || (!brandRole.hasMinimum(BrandRole.PUBLISHER)
                && brandRole != BrandRole.SENIOR_EDITOR)) {
            throw new IllegalStateException(
                    "Publishing authority required. User's brand role (" + brandRole +
                    ") is insufficient to " + action + ". Required: PUBLISHER or SENIOR_EDITOR.");
        }
    }

    /**
     * Validates that the user has at least {@code minimumBrandRole} for the given brand,
     * unless they hold a global ADMIN or SUPER_ADMIN role (which bypass brand checks).
     */
    private void validateBrandRole(UUID userId, UUID brandId, BrandRole minimumBrandRole, UserRole globalRole) {
        if (isGlobalAdmin(globalRole)) return;
        if (userId == null || brandId == null) return;

        Optional<com.allocator.contentservice.model.BrandAssignment> assignment =
                brandAssignmentRepository.findByUserIdAndBrandId(userId, brandId);

        if (assignment.isEmpty()) {
            throw new IllegalStateException(
                    "Access denied: user " + userId + " has no assignment for brand " + brandId + ".");
        }

        BrandRole brandRole = BrandRole.fromString(assignment.get().getRole());
        if (brandRole == null || !brandRole.hasMinimum(minimumBrandRole)) {
            throw new IllegalStateException(
                    "Insufficient brand role. Required: " + minimumBrandRole +
                    ", actual: " + brandRole + " for brand " + brandId + ".");
        }
    }

    private boolean isGlobalAdmin(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    /**
     * SUPER_ADMIN is exempt from the four-eyes principle: they may create, review,
     * approve, and publish the same piece of content without a second reviewer.
     */
    private boolean isSuperAdmin(UserRole role) {
        return role == UserRole.SUPER_ADMIN;
    }

    private void requireMinimumRole(UserRole userRole, UserRole minimumRole, String action) {
        if (getRolePriority(userRole) < getRolePriority(minimumRole)) {
            throw new IllegalStateException(
                    "Role " + userRole + " is not authorised to " + action +
                    " (minimum global role: " + minimumRole + ")");
        }
    }

    private void requireStatus(Content content, String action, ContentStatus... allowed) {
        ContentStatus current = content.getStatus();
        for (ContentStatus s : allowed) {
            if (current == s) return;
        }
        throw new IllegalStateException(
                "Cannot " + action + " content with status " + current +
                ". Allowed: " + java.util.Arrays.toString(allowed));
    }

    private void validateOwnership(Content content, UUID userId, UserRole userRole) {
        if (isGlobalAdmin(userRole) || userRole == UserRole.EDITOR) return;
        UUID creator = content.getCreatedBy();
        if (creator != null && !creator.equals(userId)) {
            throw new IllegalStateException("Only the content creator or an editor can modify this content");
        }
    }

    private int getRolePriority(UserRole role) {
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

    // ─── Revision and audit helpers ───────────────────────────────────────────

    private void createRevision(Content content, UUID actorId, String changeSummary) {
        Integer maxRevision = contentRevisionRepository.findMaxRevisionNumber(content.getId());
        Integer nextRevision = maxRevision != null ? maxRevision + 1 : 1;

        ContentRevision revision = ContentRevision.builder()
                .content(content)
                .revisionNumber(nextRevision)
                .contentSnapshot(snapshotContent(content))
                .editorId(actorId)
                .changeSummary(changeSummary)
                .build();

        contentRevisionRepository.save(revision);
    }

    private String snapshotContent(Content content) {
        try {
            java.util.Map<String, Object> snap = new java.util.LinkedHashMap<>();
            snap.put("id", content.getId() != null ? content.getId().toString() : null);
            snap.put("title", content.getTitle());
            snap.put("status", content.getStatus() != null ? content.getStatus().name() : null);
            snap.put("versionNumber", content.getVersionNumber());
            snap.put("body", content.getBody());
            snap.put("contentType", content.getContentType() != null ? content.getContentType().name() : null);
            snap.put("metadata", content.getMetadata());
            snap.put("complianceRequired", content.getComplianceRequired());
            snap.put("complianceApproved", content.getComplianceApproved());
            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            log.warn("Could not serialize content snapshot for contentId={}", content.getId(), e);
            return content.getId() != null ? content.getId().toString() : "unknown";
        }
    }

    void recordAudit(Content content, WorkflowEventType eventType, UUID actorId, String actorRole,
                     ContentStatus fromStatus, ContentStatus toStatus, String reason) {
        WorkflowAuditLog entry = WorkflowAuditLog.builder()
                .contentId(content.getId())
                .brandId(content.getBrandId())
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus != null ? toStatus.name() : null)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(entry);
    }

    // ─── Field update helper ──────────────────────────────────────────────────

    private void updateContentFields(Content existing, Content updated) {
        if (updated.getTitle() != null)           existing.setTitle(updated.getTitle());
        if (updated.getSlug() != null)            existing.setSlug(updated.getSlug());
        if (updated.getSummary() != null)         existing.setSummary(updated.getSummary());
        if (updated.getBody() != null)            existing.setBody(updated.getBody());
        if (updated.getContentType() != null)     existing.setContentType(updated.getContentType());
        if (updated.getMetaTitle() != null)       existing.setMetaTitle(updated.getMetaTitle());
        if (updated.getMetaDescription() != null) existing.setMetaDescription(updated.getMetaDescription());
        if (updated.getCanonicalUrl() != null)    existing.setCanonicalUrl(updated.getCanonicalUrl());
        if (updated.getMetadata() != null) {
            java.util.Map<String, Object> mergedMetadata = existing.getMetadata() != null
                    ? new java.util.HashMap<>(existing.getMetadata())
                    : new java.util.HashMap<>();
            mergedMetadata.putAll(updated.getMetadata());
            existing.setMetadata(mergedMetadata);
        }
        if (updated.getTags() != null)            existing.setTags(updated.getTags());
        if (updated.getAuthors() != null)         existing.setAuthors(updated.getAuthors());
        if (updated.getReferences() != null)      existing.setReferences(updated.getReferences());
        if (updated.getMedia() != null)           existing.setMedia(updated.getMedia());
        if (updated.getHeroImageUrl() != null)    existing.setHeroImageUrl(updated.getHeroImageUrl());
        if (updated.getReadTime() != null)        existing.setReadTime(updated.getReadTime());
        if (updated.getAccessTier() != null)      existing.setAccessTier(updated.getAccessTier());
        if (updated.getAnonymous() != null)       existing.setAnonymous(updated.getAnonymous());
        if (updated.getExcerpt() != null)         existing.setExcerpt(updated.getExcerpt());
        if (updated.getSubtitle() != null)        existing.setSubtitle(updated.getSubtitle());
    }
}
