package com.allocator.commentservice.controller;

import com.allocator.commentservice.dto.ApiResponse;
import com.allocator.commentservice.dto.CommentRequest;
import com.allocator.commentservice.dto.CommentResponse;
import com.allocator.commentservice.dto.RemoveCommentRequest;
import com.allocator.commentservice.dto.VoteRequest;
import com.allocator.commentservice.service.PublicCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public-comments")
@RequiredArgsConstructor
@Slf4j
public class PublicCommentController {

    private final PublicCommentService commentService;

    record EditRequest(String body) {}

    // Combines the paginated top-level page with the content's total comment
    // count (top-level + all replies) so the first load is a single round trip.
    record CommentListResponse(ApiResponse.PageResponse<CommentResponse> page, long totalCount) {}

    @GetMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<CommentListResponse>> listTopLevel(
            @PathVariable UUID contentId,
            @RequestParam(defaultValue = "RECENT") PublicCommentService.SortOrder sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) UUID viewerId) {

        var result = commentService.listTopLevel(contentId, sort, page, Math.min(size, 50), viewerId);
        long totalCount = commentService.countAll(contentId);
        return ResponseEntity.ok(ApiResponse.success(new CommentListResponse(result, totalCount)));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<CommentResponse>>> listReplies(
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "X-User-Id", required = false) UUID viewerId) {

        var result = commentService.listReplies(commentId, page, Math.min(size, 50), viewerId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable UUID contentId,
            @Valid @RequestBody CommentRequest req,
            @RequestHeader("X-User-Id") UUID userId) {

        CommentResponse saved = commentService.createComment(contentId, userId, req);
        return ResponseEntity.ok(ApiResponse.success(saved, "Comment added"));
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> createReply(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest req,
            @RequestHeader("X-User-Id") UUID userId) {

        CommentResponse saved = commentService.createReply(commentId, userId, req);
        return ResponseEntity.ok(ApiResponse.success(saved, "Reply added"));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @RequestBody EditRequest req,
            @RequestHeader("X-User-Id") UUID userId) {

        CommentResponse updated = commentService.updateComment(commentId, userId, req.body());
        return ResponseEntity.ok(ApiResponse.success(updated, "Comment updated"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentResponse>> toggleLike(
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {

        CommentResponse updated = commentService.toggleLike(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<ApiResponse<CommentResponse>> vote(
            @PathVariable UUID commentId,
            @Valid @RequestBody VoteRequest req,
            @RequestHeader("X-User-Id") UUID userId) {

        CommentResponse updated = commentService.vote(commentId, userId, req.getValue());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    // ── Moderation (admin / super-admin only) ───────────────────────────────

    @PostMapping("/{commentId}/remove")
    public ResponseEntity<ApiResponse<CommentResponse>> removeComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody RemoveCommentRequest req,
            @RequestHeader("X-User-Id") UUID moderatorId,
            @RequestHeader("X-User-Roles") String moderatorRoles) {

        CommentResponse updated = commentService.removeComment(commentId, moderatorId, moderatorRoles, req.getReason());
        return ResponseEntity.ok(ApiResponse.success(updated, "Comment removed"));
    }

    @PostMapping("/{commentId}/restore")
    public ResponseEntity<ApiResponse<CommentResponse>> restoreComment(
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID moderatorId,
            @RequestHeader("X-User-Roles") String moderatorRoles) {

        CommentResponse updated = commentService.restoreComment(commentId, moderatorId, moderatorRoles);
        return ResponseEntity.ok(ApiResponse.success(updated, "Comment restored"));
    }
}
