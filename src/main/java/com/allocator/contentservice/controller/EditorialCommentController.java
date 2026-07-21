package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.CommentStatus;
import com.allocator.contentservice.model.EditorialComment;
import com.allocator.contentservice.service.EditorialCommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/{contentId}/comments")
@RequiredArgsConstructor
@Slf4j
public class EditorialCommentController {

    private final EditorialCommentService commentService;

    record CommentRequest(String body, Integer anchorStart, Integer anchorEnd,
                          String anchorText, Integer contentVersionNumber) {}
    record ReplyRequest(String body) {}
    record EditRequest(String body) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<EditorialComment>>> getComments(
            @PathVariable UUID contentId,
            @RequestParam Optional<CommentStatus> status) {
        List<EditorialComment> comments = commentService.getComments(contentId, status.orElse(null));
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EditorialComment>> addComment(
            @PathVariable UUID contentId,
            @RequestBody CommentRequest req,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        EditorialComment saved = commentService.addComment(contentId, userId,
                userName != null ? userName : userId.toString(), userRoles,
                req.body(), req.anchorStart(), req.anchorEnd(), req.anchorText(), req.contentVersionNumber());
        return ResponseEntity.ok(ApiResponse.success(saved, "Comment added"));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<EditorialComment>> editComment(
            @PathVariable UUID contentId,
            @PathVariable UUID commentId,
            @RequestBody EditRequest req,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialComment updated = commentService.editComment(commentId, userId, req.body());
        return ResponseEntity.ok(ApiResponse.success(updated, "Comment updated"));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID contentId,
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {
        commentService.softDelete(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<EditorialComment>> addReply(
            @PathVariable UUID contentId,
            @PathVariable UUID commentId,
            @RequestBody ReplyRequest req,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        EditorialComment reply = commentService.addReply(commentId, userId,
                userName != null ? userName : userId.toString(), userRoles, req.body());
        return ResponseEntity.ok(ApiResponse.success(reply, "Reply added"));
    }

    @PostMapping("/{commentId}/resolve")
    public ResponseEntity<ApiResponse<EditorialComment>> resolve(
            @PathVariable UUID contentId,
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialComment resolved = commentService.resolve(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(resolved, "Comment resolved"));
    }

    @PostMapping("/{commentId}/reopen")
    public ResponseEntity<ApiResponse<EditorialComment>> reopen(
            @PathVariable UUID contentId,
            @PathVariable UUID commentId,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialComment reopened = commentService.reopen(commentId, userId);
        return ResponseEntity.ok(ApiResponse.success(reopened, "Comment reopened"));
    }
}
