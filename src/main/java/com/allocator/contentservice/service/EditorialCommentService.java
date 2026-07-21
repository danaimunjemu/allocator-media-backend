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
public class EditorialCommentService {

    private final EditorialCommentRepository commentRepository;
    private final InAppNotificationRepository notificationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final EditorialTaskRepository taskRepository;
    private final ContentRepository contentRepository;

    public List<EditorialComment> getComments(UUID contentId, CommentStatus filterStatus) {
        if (filterStatus == null) {
            return commentRepository.findByContentIdAndDeletedAtIsNullOrderByCreatedAtAsc(contentId);
        }
        return commentRepository.findByContentIdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(contentId, filterStatus);
    }

    public List<EditorialComment> getReplies(UUID parentCommentId) {
        return commentRepository.findByParentCommentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentCommentId);
    }

    @Transactional
    public EditorialComment addComment(UUID contentId, UUID authorId, String authorName,
                                       String authorRole, String body, Integer anchorStart, Integer anchorEnd,
                                       String anchorText, Integer contentVersionNumber) {
        UUID brandId = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId))
                .getBrandId();
        EditorialComment comment = EditorialComment.builder()
                .contentId(contentId)
                .brandId(brandId)
                .authorId(authorId)
                .authorName(authorName)
                .authorRole(authorRole)
                .body(body)
                .anchorStart(anchorStart)
                .anchorEnd(anchorEnd)
                .anchorText(anchorText)
                .contentVersionNumber(contentVersionNumber)
                .createdBy(authorId)
                .build();

        EditorialComment saved = commentRepository.save(comment);
        recordAudit(contentId, brandId, WorkflowEventType.COMMENT_ADDED, authorId, authorRole, null);
        notifyCollaborators(contentId, brandId, authorId, authorName,
                InAppNotificationType.COMMENT_ADDED,
                authorName + " added a comment",
                "/content/" + contentId + "/review");
        log.info("Comment added to content {} by user {}", contentId, authorId);
        return saved;
    }

    @Transactional
    public EditorialComment addReply(UUID parentCommentId, UUID authorId, String authorName,
                                     String authorRole, String body) {
        EditorialComment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + parentCommentId));

        EditorialComment reply = EditorialComment.builder()
                .contentId(parent.getContentId())
                .brandId(parent.getBrandId())
                .parentCommentId(parentCommentId)
                .authorId(authorId)
                .authorName(authorName)
                .authorRole(authorRole)
                .body(body)
                .createdBy(authorId)
                .build();

        EditorialComment saved = commentRepository.save(reply);
        recordAudit(parent.getContentId(), parent.getBrandId(), WorkflowEventType.REPLY_ADDED,
                authorId, authorRole, null);
        notifyCollaborators(parent.getContentId(), parent.getBrandId(), authorId, authorName,
                InAppNotificationType.REPLY_ADDED,
                authorName + " replied to a comment",
                "/content/" + parent.getContentId() + "/review");
        return saved;
    }

    @Transactional
    public EditorialComment editComment(UUID commentId, UUID requestingUserId, String newBody) {
        EditorialComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!comment.getAuthorId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the comment author can edit their comment");
        }
        comment.setBody(newBody);
        return commentRepository.save(comment);
    }

    @Transactional
    public void softDelete(UUID commentId, UUID requestingUserId) {
        EditorialComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!comment.getAuthorId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the comment author can delete their comment");
        }
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
        recordAudit(comment.getContentId(), comment.getBrandId(), WorkflowEventType.COMMENT_DELETED,
                requestingUserId, null, null);
    }

    @Transactional
    public EditorialComment resolve(UUID commentId, UUID resolvingUserId) {
        EditorialComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        comment.setStatus(CommentStatus.RESOLVED);
        comment.setResolvedBy(resolvingUserId);
        comment.setResolvedAt(Instant.now());
        EditorialComment saved = commentRepository.save(comment);
        recordAudit(comment.getContentId(), comment.getBrandId(), WorkflowEventType.COMMENT_RESOLVED,
                resolvingUserId, null, null);
        notifyCollaborators(comment.getContentId(), comment.getBrandId(), resolvingUserId, "A reviewer",
                InAppNotificationType.COMMENT_RESOLVED,
                "A comment was resolved",
                "/content/" + comment.getContentId() + "/review");
        return saved;
    }

    @Transactional
    public EditorialComment reopen(UUID commentId, UUID reopeningUserId) {
        EditorialComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        comment.setStatus(CommentStatus.REOPENED);
        comment.setResolvedBy(null);
        comment.setResolvedAt(null);
        EditorialComment saved = commentRepository.save(comment);
        recordAudit(comment.getContentId(), comment.getBrandId(), WorkflowEventType.COMMENT_REOPENED,
                reopeningUserId, null, null);
        return saved;
    }

    @Transactional
    public void markAnchorsStale(UUID contentId, int newVersionNumber) {
        commentRepository.markAnchorsStale(contentId, newVersionNumber);
        log.info("Marked comment anchors stale for content {} at version {}", contentId, newVersionNumber);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void notifyCollaborators(UUID contentId, UUID brandId, UUID actorId, String actorName,
                                     InAppNotificationType type, String message, String linkPath) {
        Set<UUID> recipients = new HashSet<>();
        reviewRequestRepository.findDistinctAssigneeIdsByContentId(contentId).forEach(recipients::add);
        reviewRequestRepository.findDistinctRequesterIdsByContentId(contentId).forEach(recipients::add);
        taskRepository.findDistinctAssigneeIdsByContentId(contentId).forEach(recipients::add);
        commentRepository.findDistinctAuthorIdsByContentId(contentId).forEach(recipients::add);
        recipients.remove(actorId);

        List<InAppNotification> notifications = recipients.stream()
                .map(uid -> InAppNotification.builder()
                        .userId(uid)
                        .brandId(brandId)
                        .notificationType(type)
                        .contentId(contentId)
                        .actorId(actorId)
                        .actorName(actorName)
                        .message(message)
                        .linkPath(linkPath)
                        .build())
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

    private void recordAudit(UUID contentId, UUID brandId, WorkflowEventType eventType,
                              UUID actorId, String actorRole, String reason) {
        WorkflowAuditLog entry = WorkflowAuditLog.builder()
                .contentId(contentId)
                .brandId(brandId)
                .eventType(eventType)
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        auditLogRepository.save(entry);
    }
}
