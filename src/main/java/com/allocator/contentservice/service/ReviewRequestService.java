package com.allocator.contentservice.service;

import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewRequestService {

    private final ReviewRequestRepository reviewRequestRepository;
    private final InAppNotificationRepository notificationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final ContentRepository contentRepository;

    public List<ReviewRequest> getForContent(UUID contentId) {
        return reviewRequestRepository.findByContentIdOrderByRequestedAtDesc(contentId);
    }

    public List<ReviewRequest> getPendingForAssignee(UUID assigneeId) {
        return reviewRequestRepository.findByAssigneeIdAndStatusOrderByRequestedAtDesc(
                assigneeId, ReviewStatus.PENDING);
    }

    public List<ReviewRequest> getPendingForBrand(UUID brandId) {
        return reviewRequestRepository.findByBrandIdAndStatusOrderByRequestedAtDesc(
                brandId, ReviewStatus.PENDING);
    }

    @Transactional
    public ReviewRequest create(UUID contentId, UUID requestedBy, UUID assigneeId,
                                String assigneeRole, ReviewType reviewType, String notes, Instant dueDate) {
        UUID brandId = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId))
                .getBrandId();
        ReviewRequest request = ReviewRequest.builder()
                .contentId(contentId)
                .brandId(brandId)
                .requestedBy(requestedBy)
                .assigneeId(assigneeId)
                .assigneeRole(assigneeRole)
                .reviewType(reviewType)
                .notes(notes)
                .dueDate(dueDate)
                .status(ReviewStatus.PENDING)
                .build();

        ReviewRequest saved = reviewRequestRepository.save(request);

        recordAudit(contentId, brandId, WorkflowEventType.REVIEW_REQUESTED, requestedBy, null,
                "Review type: " + reviewType.name());

        if (assigneeId != null) {
            notificationRepository.save(InAppNotification.builder()
                    .userId(assigneeId)
                    .brandId(brandId)
                    .notificationType(InAppNotificationType.REVIEW_REQUESTED)
                    .contentId(contentId)
                    .actorId(requestedBy)
                    .message("You have been requested to review an article (" + reviewType.name() + ")")
                    .linkPath("/content/" + contentId + "/review")
                    .build());
        }

        log.info("Review request created for content {} by {}", contentId, requestedBy);
        return saved;
    }

    @Transactional
    public ReviewRequest submitDecision(UUID requestId, UUID deciderId, ReviewDecision decision,
                                        String decisionNote) {
        ReviewRequest request = reviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Review request not found: " + requestId));

        if (request.getStatus() == ReviewStatus.CANCELLED) {
            throw new IllegalStateException("Cannot decide on a cancelled review request");
        }

        request.setDecision(decision);
        request.setDecisionNote(decisionNote);
        request.setCompletedBy(deciderId);
        request.setCompletedAt(Instant.now());
        request.setStatus(ReviewStatus.COMPLETED);

        ReviewRequest saved = reviewRequestRepository.save(request);

        recordAudit(request.getContentId(), request.getBrandId(),
                WorkflowEventType.REVIEW_COMPLETED, deciderId, null,
                decision.name() + (decisionNote != null ? ": " + decisionNote : ""));

        InAppNotificationType notifType = switch (decision) {
            case APPROVED                -> InAppNotificationType.REVIEW_APPROVED;
            case APPROVED_WITH_CHANGES   -> InAppNotificationType.REVIEW_APPROVED_WITH_CHANGES;
            case CHANGES_REQUESTED       -> InAppNotificationType.REVIEW_CHANGES_REQUESTED;
            case REJECTED                -> InAppNotificationType.REVIEW_REJECTED;
            case ESCALATED               -> InAppNotificationType.REVIEW_ESCALATED;
        };

        notificationRepository.save(InAppNotification.builder()
                .userId(request.getRequestedBy())
                .brandId(request.getBrandId())
                .notificationType(notifType)
                .contentId(request.getContentId())
                .actorId(deciderId)
                .message("Review decision: " + decision.name().replace('_', ' ').toLowerCase()
                        + (decisionNote != null ? " — " + decisionNote : ""))
                .linkPath("/content/" + request.getContentId() + "/review")
                .build());

        return saved;
    }

    @Transactional
    public ReviewRequest cancel(UUID requestId, UUID cancellingUserId) {
        ReviewRequest request = reviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Review request not found: " + requestId));
        request.setStatus(ReviewStatus.CANCELLED);
        return reviewRequestRepository.save(request);
    }

    private void recordAudit(UUID contentId, UUID brandId, WorkflowEventType eventType,
                              UUID actorId, String actorRole, String reason) {
        auditLogRepository.save(WorkflowAuditLog.builder()
                .contentId(contentId).brandId(brandId).eventType(eventType)
                .actorId(actorId).actorRole(actorRole).reason(reason)
                .timestamp(Instant.now()).build());
    }
}
