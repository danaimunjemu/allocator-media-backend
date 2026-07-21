package com.allocator.bookmarkservice.controller;

import com.allocator.bookmarkservice.dto.ApiResponse;
import com.allocator.bookmarkservice.dto.SavedContentResponse;
import com.allocator.bookmarkservice.dto.SavedStatusResponse;
import com.allocator.bookmarkservice.service.SavedContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-content")
@RequiredArgsConstructor
public class SavedContentController {

    private final SavedContentService savedContentService;

    @GetMapping
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<SavedContentResponse>>> listSaved(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") UUID userId) {

        var result = savedContentService.listSaved(userId, page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{contentId}/status")
    public ResponseEntity<ApiResponse<SavedStatusResponse>> status(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId) {

        boolean saved = savedContentService.isSaved(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(SavedStatusResponse.builder().saved(saved).build()));
    }

    @PostMapping("/{contentId}")
    public ResponseEntity<ApiResponse<Void>> save(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId) {

        savedContentService.save(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Saved"));
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<ApiResponse<Void>> unsave(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId) {

        savedContentService.unsave(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Removed"));
    }
}
