package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.ReviewDecision;
import com.allocator.contentservice.model.ReviewRequest;
import com.allocator.contentservice.model.ReviewType;
import com.allocator.contentservice.service.ReviewRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/{contentId}/review-requests")
@RequiredArgsConstructor
@Slf4j
public class ReviewRequestController {

    private final ReviewRequestService reviewRequestService;

    record ReviewRequestBody(UUID assigneeId, String assigneeRole, ReviewType reviewType,
                             String notes, Instant dueDate) {}
    record DecisionBody(ReviewDecision decision, String decisionNote) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewRequest>>> getForContent(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.success(reviewRequestService.getForContent(contentId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewRequest>> create(
            @PathVariable UUID contentId,
            @RequestBody ReviewRequestBody req,
            @RequestHeader("X-User-Id") UUID userId) {
        ReviewRequest saved = reviewRequestService.create(contentId, userId,
                req.assigneeId(), req.assigneeRole(), req.reviewType(), req.notes(), req.dueDate());
        return ResponseEntity.ok(ApiResponse.success(saved, "Review request created"));
    }

    @PostMapping("/{requestId}/decision")
    public ResponseEntity<ApiResponse<ReviewRequest>> submitDecision(
            @PathVariable UUID contentId,
            @PathVariable UUID requestId,
            @RequestBody DecisionBody req,
            @RequestHeader("X-User-Id") UUID userId) {
        ReviewRequest updated = reviewRequestService.submitDecision(requestId, userId,
                req.decision(), req.decisionNote());
        return ResponseEntity.ok(ApiResponse.success(updated, "Decision submitted"));
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse<ReviewRequest>> cancel(
            @PathVariable UUID contentId,
            @PathVariable UUID requestId,
            @RequestHeader("X-User-Id") UUID userId) {
        ReviewRequest cancelled = reviewRequestService.cancel(requestId, userId);
        return ResponseEntity.ok(ApiResponse.success(cancelled, "Review request cancelled"));
    }
}
