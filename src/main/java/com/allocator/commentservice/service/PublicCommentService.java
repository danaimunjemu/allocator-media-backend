package com.allocator.commentservice.service;

import com.allocator.commentservice.dto.ApiResponse;
import com.allocator.commentservice.dto.CommentRequest;
import com.allocator.commentservice.dto.CommentResponse;
import com.allocator.commentservice.model.CommentRemovalReason;
import com.allocator.commentservice.model.PublicComment;
import com.allocator.commentservice.model.PublicCommentLike;
import com.allocator.commentservice.model.PublicCommentStatus;
import com.allocator.commentservice.model.PublicCommentVote;
import com.allocator.commentservice.repository.PublicCommentLikeRepository;
import com.allocator.commentservice.repository.PublicCommentRepository;
import com.allocator.commentservice.repository.PublicCommentVoteRepository;
import com.allocator.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicCommentService {

    public enum SortOrder { RECENT, OLDEST, TOP }

    private final PublicCommentRepository commentRepository;
    private final PublicCommentLikeRepository likeRepository;
    private final PublicCommentVoteRepository voteRepository;
    private final ContentRepository contentRepository;

    // ── Create / update / delete ────────────────────────────────────────────

    @Transactional
    public CommentResponse createComment(UUID contentId, UUID authorId, CommentRequest req) {
        UUID brandId = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId))
                .getBrandId();

        PublicComment comment = PublicComment.builder()
                .contentId(contentId)
                .brandId(brandId)
                .authorId(authorId)
                .authorName(displayName(req.getAuthorName(), authorId))
                .authorAvatarUrl(req.getAuthorAvatarUrl())
                .body(req.getBody())
                .createdBy(authorId)
                .build();

        PublicComment saved = commentRepository.save(comment);
        log.info("Comment {} added to content {} by user {}", saved.getId(), contentId, authorId);
        return toResponse(saved, authorId);
    }

    @Transactional
    public CommentResponse createReply(UUID parentCommentId, UUID authorId, CommentRequest req) {
        PublicComment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + parentCommentId));

        UUID root = parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getId();

        PublicComment reply = PublicComment.builder()
                .contentId(parent.getContentId())
                .brandId(parent.getBrandId())
                .rootCommentId(root)
                .parentCommentId(parentCommentId)
                .authorId(authorId)
                .authorName(displayName(req.getAuthorName(), authorId))
                .authorAvatarUrl(req.getAuthorAvatarUrl())
                .body(req.getBody())
                .createdBy(authorId)
                .build();

        PublicComment saved = commentRepository.save(reply);
        log.info("Reply {} added to comment {} by user {}", saved.getId(), root, authorId);
        return toResponse(saved, authorId);
    }

    @Transactional
    public CommentResponse updateComment(UUID commentId, UUID requestingUserId, String newBody) {
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!comment.getAuthorId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the comment author can edit their comment");
        }
        if (comment.getStatus() != PublicCommentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot edit a deleted or removed comment");
        }
        comment.setBody(newBody);
        comment.setEditedAt(Instant.now());
        PublicComment saved = commentRepository.save(comment);
        return toResponse(saved, requestingUserId);
    }

    @Transactional
    public void deleteComment(UUID commentId, UUID requestingUserId) {
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (!comment.getAuthorId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the comment author can delete their comment");
        }
        comment.setStatus(PublicCommentStatus.DELETED);
        commentRepository.save(comment);
    }

    // ── Moderation (admin / super-admin only) ───────────────────────────────

    @Transactional
    public CommentResponse removeComment(UUID commentId, UUID moderatorId, String moderatorRoles, CommentRemovalReason reason) {
        requireModerator(moderatorRoles);
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        comment.setStatus(PublicCommentStatus.REMOVED);
        comment.setRemovedBy(moderatorId);
        comment.setRemovedAt(Instant.now());
        comment.setRemovalReason(reason);
        PublicComment saved = commentRepository.save(comment);
        log.info("Comment {} removed by moderator {} — reason: {}", commentId, moderatorId, reason);
        return toResponse(saved, moderatorId);
    }

    @Transactional
    public CommentResponse restoreComment(UUID commentId, UUID moderatorId, String moderatorRoles) {
        requireModerator(moderatorRoles);
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        if (comment.getStatus() != PublicCommentStatus.REMOVED) {
            throw new IllegalStateException("Comment is not currently removed");
        }

        comment.setStatus(PublicCommentStatus.ACTIVE);
        comment.setRemovedBy(null);
        comment.setRemovedAt(null);
        comment.setRemovalReason(null);
        PublicComment saved = commentRepository.save(comment);
        log.info("Comment {} restored by moderator {}", commentId, moderatorId);
        return toResponse(saved, moderatorId);
    }

    private void requireModerator(String rolesHeader) {
        boolean isModerator = rolesHeader != null && Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN")
                            || r.equalsIgnoreCase("SUPER_ADMIN") || r.equalsIgnoreCase("ROLE_SUPER_ADMIN"));
        if (!isModerator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required to moderate comments");
        }
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse.PageResponse<CommentResponse> listTopLevel(
            UUID contentId, SortOrder sort, int page, int size, UUID viewerId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PublicComment> result = switch (sort) {
            case OLDEST -> commentRepository.findByContentIdAndRootCommentIdIsNullOrderByCreatedAtAsc(contentId, pageable);
            case TOP -> commentRepository.findTopLevelOrderByScore(contentId, pageable);
            default -> commentRepository.findByContentIdAndRootCommentIdIsNullOrderByCreatedAtDesc(contentId, pageable);
        };
        return ApiResponse.PageResponse.from(enrich(result, viewerId));
    }

    @Transactional(readOnly = true)
    public ApiResponse.PageResponse<CommentResponse> listReplies(
            UUID rootCommentId, int page, int size, UUID viewerId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PublicComment> result = commentRepository.findByRootCommentIdOrderByCreatedAtAsc(rootCommentId, pageable);
        return ApiResponse.PageResponse.from(enrich(result, viewerId));
    }

    @Transactional(readOnly = true)
    public long countAll(UUID contentId) {
        return commentRepository.countByContentId(contentId);
    }

    // ── Likes / votes ────────────────────────────────────────────────────────

    @Transactional
    public CommentResponse toggleLike(UUID commentId, UUID userId) {
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        Optional<PublicCommentLike> existing = likeRepository.findByCommentIdAndUserId(commentId, userId);
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
        } else {
            likeRepository.save(PublicCommentLike.builder()
                    .commentId(commentId).userId(userId).createdBy(userId).build());
        }
        return toResponse(comment, userId);
    }

    @Transactional
    public CommentResponse vote(UUID commentId, UUID userId, short value) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }
        PublicComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        Optional<PublicCommentVote> existing = voteRepository.findByCommentIdAndUserId(commentId, userId);
        if (existing.isPresent()) {
            if (existing.get().getValue() == value) {
                voteRepository.delete(existing.get());
            } else {
                existing.get().setValue(value);
                voteRepository.save(existing.get());
            }
        } else {
            voteRepository.save(PublicCommentVote.builder()
                    .commentId(commentId).userId(userId).value(value).createdBy(userId).build());
        }
        return toResponse(comment, userId);
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private Page<CommentResponse> enrich(Page<PublicComment> page, UUID viewerId) {
        List<PublicComment> comments = page.getContent();
        List<UUID> ids = comments.stream().map(PublicComment::getId).toList();

        Map<UUID, Long> likeCounts = ids.isEmpty() ? Map.of() : likeRepository.countGroupedByCommentIds(ids).stream()
                .collect(Collectors.toMap(
                        PublicCommentLikeRepository.CommentCount::getCommentId,
                        PublicCommentLikeRepository.CommentCount::getTotal));
        Set<UUID> likedByMe = (viewerId == null || ids.isEmpty())
                ? Set.of() : new HashSet<>(likeRepository.findLikedCommentIds(ids, viewerId));

        Map<UUID, Long> scores = ids.isEmpty() ? Map.of() : voteRepository.sumScoresGroupedByCommentIds(ids).stream()
                .collect(Collectors.toMap(
                        PublicCommentVoteRepository.CommentScore::getCommentId,
                        PublicCommentVoteRepository.CommentScore::getTotal));
        Map<UUID, Short> myVotes = (viewerId == null || ids.isEmpty())
                ? Map.of() : voteRepository.findByCommentIdInAndUserId(ids, viewerId).stream()
                        .collect(Collectors.toMap(PublicCommentVote::getCommentId, PublicCommentVote::getValue));

        Map<UUID, Long> replyCounts = new HashMap<>();
        for (PublicComment c : comments) {
            if (c.getRootCommentId() == null) {
                replyCounts.put(c.getId(), commentRepository.countByRootCommentId(c.getId()));
            }
        }

        return page.map(c -> buildResponse(c,
                likeCounts.getOrDefault(c.getId(), 0L),
                likedByMe.contains(c.getId()),
                scores.getOrDefault(c.getId(), 0L),
                myVotes.get(c.getId()),
                replyCounts.getOrDefault(c.getId(), 0L)));
    }

    private CommentResponse toResponse(PublicComment c, UUID viewerId) {
        long likeCount = likeRepository.countByCommentId(c.getId());
        boolean likedByMe = viewerId != null
                && likeRepository.findByCommentIdAndUserId(c.getId(), viewerId).isPresent();
        long score = voteRepository.sumScoresGroupedByCommentIds(List.of(c.getId())).stream()
                .findFirst().map(PublicCommentVoteRepository.CommentScore::getTotal).orElse(0L);
        Short myVote = viewerId == null ? null
                : voteRepository.findByCommentIdAndUserId(c.getId(), viewerId).map(PublicCommentVote::getValue).orElse(null);
        long replyCount = c.getRootCommentId() == null ? commentRepository.countByRootCommentId(c.getId()) : 0;
        return buildResponse(c, likeCount, likedByMe, score, myVote, replyCount);
    }

    private CommentResponse buildResponse(PublicComment c, long likeCount, boolean likedByMe,
                                           long score, Short myVote, long replyCount) {
        boolean deleted = c.getStatus() == PublicCommentStatus.DELETED;
        boolean removed = c.getStatus() == PublicCommentStatus.REMOVED;

        String body = c.getBody();
        if (deleted) body = "[Comment deleted]";
        if (removed) body = "[Comment removed by moderator]";

        return CommentResponse.builder()
                .id(c.getId())
                .contentId(c.getContentId())
                .rootCommentId(c.getRootCommentId())
                .parentCommentId(c.getParentCommentId())
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .authorAvatarUrl(c.getAuthorAvatarUrl())
                .body(body)
                .deleted(deleted)
                .removed(removed)
                .removalReason(removed && c.getRemovalReason() != null ? c.getRemovalReason().getLabel() : null)
                .removedAt(removed ? c.getRemovedAt() : null)
                .createdAt(c.getCreatedAt())
                .editedAt(c.getStatus() == PublicCommentStatus.ACTIVE ? c.getEditedAt() : null)
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .score(score)
                .myVote(myVote)
                .replyCount(replyCount)
                .build();
    }

    private String displayName(String name, UUID authorId) {
        return (name == null || name.isBlank()) ? "User " + authorId.toString().substring(0, 8) : name;
    }
}
