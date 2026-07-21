package com.allocator.contentservice.service;

import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditorialAnnotationService {

    private final EditorialAnnotationRepository annotationRepository;
    private final InAppNotificationRepository notificationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final EditorialTaskRepository taskRepository;
    private final EditorialCommentRepository commentRepository;
    private final ContentRepository contentRepository;

    public List<EditorialAnnotation> getAnnotations(UUID contentId, AnnotationType typeFilter,
                                                     CommentStatus statusFilter) {
        if (typeFilter != null && statusFilter != null) {
            return annotationRepository.findByContentIdAndAnnotationTypeAndStatusOrderByCreatedAtAsc(
                    contentId, typeFilter, statusFilter);
        }
        if (typeFilter != null) {
            return annotationRepository.findByContentIdAndAnnotationTypeOrderByCreatedAtAsc(contentId, typeFilter);
        }
        if (statusFilter != null) {
            return annotationRepository.findByContentIdAndStatusOrderByCreatedAtAsc(contentId, statusFilter);
        }
        return annotationRepository.findByContentIdOrderByCreatedAtAsc(contentId);
    }

    @Transactional
    public EditorialAnnotation addAnnotation(UUID contentId, UUID annotatorId,
                                              String annotatorName, String annotatorRole,
                                              AnnotationType type, String body,
                                              Integer anchorStart, Integer anchorEnd,
                                              String anchorText, Integer contentVersionNumber) {
        UUID brandId = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId))
                .getBrandId();
        EditorialAnnotation annotation = EditorialAnnotation.builder()
                .contentId(contentId)
                .brandId(brandId)
                .annotatorId(annotatorId)
                .annotatorName(annotatorName)
                .annotatorRole(annotatorRole)
                .annotationType(type)
                .body(body)
                .anchorStart(anchorStart)
                .anchorEnd(anchorEnd)
                .anchorText(anchorText)
                .contentVersionNumber(contentVersionNumber)
                .createdBy(annotatorId)
                .build();

        EditorialAnnotation saved = annotationRepository.save(annotation);
        recordAudit(contentId, brandId, WorkflowEventType.ANNOTATION_CREATED, annotatorId, annotatorRole, null);
        notifyCollaborators(contentId, brandId, annotatorId, annotatorName,
                InAppNotificationType.ANNOTATION_CREATED,
                annotatorName + " added a " + type.name().toLowerCase().replace('_', ' ') + " annotation",
                "/content/" + contentId + "/review");
        return saved;
    }

    @Transactional
    public EditorialAnnotation resolve(UUID annotationId, UUID resolvingUserId, String resolvedNote) {
        EditorialAnnotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found: " + annotationId));
        annotation.setStatus(CommentStatus.RESOLVED);
        annotation.setResolvedBy(resolvingUserId);
        annotation.setResolvedAt(Instant.now());
        annotation.setResolvedNote(resolvedNote);
        EditorialAnnotation saved = annotationRepository.save(annotation);
        recordAudit(annotation.getContentId(), annotation.getBrandId(),
                WorkflowEventType.ANNOTATION_RESOLVED, resolvingUserId, null, resolvedNote);
        notifyCollaborators(annotation.getContentId(), annotation.getBrandId(), resolvingUserId, "A reviewer",
                InAppNotificationType.ANNOTATION_RESOLVED,
                "An annotation was resolved",
                "/content/" + annotation.getContentId() + "/review");
        return saved;
    }

    @Transactional
    public void markAnchorsStale(UUID contentId, int newVersionNumber) {
        annotationRepository.markAnchorsStale(contentId, newVersionNumber);
        log.info("Marked annotation anchors stale for content {} at version {}", contentId, newVersionNumber);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void notifyCollaborators(UUID contentId, UUID brandId, UUID actorId, String actorName,
                                     InAppNotificationType type, String message, String linkPath) {
        Set<UUID> recipients = new HashSet<>();
        reviewRequestRepository.findDistinctAssigneeIdsByContentId(contentId).forEach(recipients::add);
        reviewRequestRepository.findDistinctRequesterIdsByContentId(contentId).forEach(recipients::add);
        taskRepository.findDistinctAssigneeIdsByContentId(contentId).forEach(recipients::add);
        commentRepository.findDistinctAuthorIdsByContentId(contentId).forEach(recipients::add);
        annotationRepository.findDistinctAnnotatorIdsByContentId(contentId).forEach(recipients::add);
        recipients.remove(actorId);

        notificationRepository.saveAll(recipients.stream()
                .map(uid -> InAppNotification.builder()
                        .userId(uid).brandId(brandId).notificationType(type)
                        .contentId(contentId).actorId(actorId).actorName(actorName)
                        .message(message).linkPath(linkPath).build())
                .collect(Collectors.toList()));
    }

    private void recordAudit(UUID contentId, UUID brandId, WorkflowEventType eventType,
                              UUID actorId, String actorRole, String reason) {
        auditLogRepository.save(WorkflowAuditLog.builder()
                .contentId(contentId).brandId(brandId).eventType(eventType)
                .actorId(actorId).actorRole(actorRole).reason(reason)
                .timestamp(Instant.now()).build());
    }
}
