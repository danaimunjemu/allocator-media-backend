package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.AnnotationType;
import com.allocator.contentservice.model.CommentStatus;
import com.allocator.contentservice.model.EditorialAnnotation;
import com.allocator.contentservice.service.EditorialAnnotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/{contentId}/annotations")
@RequiredArgsConstructor
@Slf4j
public class EditorialAnnotationController {

    private final EditorialAnnotationService annotationService;

    record AnnotationRequest(AnnotationType annotationType, String body, Integer anchorStart,
                             Integer anchorEnd, String anchorText, Integer contentVersionNumber) {}
    record ResolveRequest(String note) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<EditorialAnnotation>>> getAnnotations(
            @PathVariable UUID contentId,
            @RequestParam Optional<AnnotationType> type,
            @RequestParam Optional<String> status) {
        CommentStatus statusFilter = status.map(s -> {
            try { return CommentStatus.valueOf(s.toUpperCase()); }
            catch (IllegalArgumentException e) { return null; }
        }).orElse(null);
        List<EditorialAnnotation> annotations = annotationService.getAnnotations(
                contentId, type.orElse(null), statusFilter);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EditorialAnnotation>> addAnnotation(
            @PathVariable UUID contentId,
            @RequestBody AnnotationRequest req,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        EditorialAnnotation saved = annotationService.addAnnotation(contentId, userId,
                userName != null ? userName : userId.toString(), userRoles,
                req.annotationType(), req.body(), req.anchorStart(), req.anchorEnd(),
                req.anchorText(), req.contentVersionNumber());
        return ResponseEntity.ok(ApiResponse.success(saved, "Annotation added"));
    }

    @PostMapping("/{annotationId}/resolve")
    public ResponseEntity<ApiResponse<EditorialAnnotation>> resolve(
            @PathVariable UUID contentId,
            @PathVariable UUID annotationId,
            @RequestBody ResolveRequest req,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialAnnotation resolved = annotationService.resolve(annotationId, userId, req.note());
        return ResponseEntity.ok(ApiResponse.success(resolved, "Annotation resolved"));
    }
}
