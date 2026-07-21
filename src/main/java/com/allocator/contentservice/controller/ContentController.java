package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.ChangeBrandRequest;
import com.allocator.contentservice.dto.ContentFilter;
import com.allocator.contentservice.dto.ContentRequest;
import com.allocator.contentservice.dto.ContentResponse;
import com.allocator.contentservice.dto.ComplianceDecisionRequest;
import com.allocator.contentservice.dto.RejectRequest;
import com.allocator.contentservice.dto.ScheduleRequest;
import com.allocator.contentservice.dto.VersionHistoryResponse;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.WorkflowAuditLog;
import com.allocator.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Slf4j
public class ContentController {

    private final ContentService contentService;

    // ─── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<ContentResponse>> createContent(
            @Valid @RequestBody ContentRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Creating content by user: {} with roles: {}", userId, userRoles);
        ContentResponse response = contentService.createContent(request, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentResponse>> getContent(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            jakarta.servlet.http.HttpServletRequest request) {

        ContentResponse response = contentService.getContent(id, userId,
                com.allocator.contentservice.security.ViewerTierResolver.resolve(request));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ContentResponse>> getOneBySlug(
            @PathVariable String slug,
            jakarta.servlet.http.HttpServletRequest request) {
        ContentResponse response = contentService.getContentBySlug(slug,
                com.allocator.contentservice.security.ViewerTierResolver.resolve(request));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContentResponse>> updateContent(
            @PathVariable UUID id,
            @Valid @RequestBody ContentRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Updating content: {} by user: {}", id, userId);
        ContentResponse response = contentService.updateContent(id, request, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content updated successfully"));
    }

    // Global ADMIN/SUPER_ADMIN only — reassigns content to a different brand
    // regardless of its current status (draft, published, archived, etc.),
    // unlike the general update endpoint above which is DRAFT-only.
    @PatchMapping("/{id}/brand")
    public ResponseEntity<ApiResponse<ContentResponse>> changeBrand(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeBrandRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Changing brand for content: {} to: {} by user: {}", id, request.getBrandId(), userId);
        ContentResponse response = contentService.changeBrand(id, request.getBrandId(), userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content brand updated"));
    }

    // ─── Workflow transitions ─────────────────────────────────────────────────

    @PostMapping("/{id}/submit-review")
    public ResponseEntity<ApiResponse<ContentResponse>> submitForReview(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Submitting content for review: {} by user: {}", id, userId);
        ContentResponse response = contentService.submitForReview(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content submitted for review"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ContentResponse>> rejectContent(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            @RequestHeader("X-User-Id") UUID editorId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Rejecting content: {} by editor: {}", id, editorId);
        ContentResponse response = contentService.rejectContent(id, editorId, userRoles, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(response, "Content returned to draft"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ContentResponse>> approveContent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID editorId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Approving content: {} by editor: {}", id, editorId);
        ContentResponse response = contentService.approveContent(id, editorId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content approved"));
    }

    @PostMapping("/{id}/retract-approval")
    public ResponseEntity<ApiResponse<ContentResponse>> retractApproval(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Retracting approval for content: {} by user: {}", id, userId);
        ContentResponse response = contentService.retractApproval(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Approval retracted — content returned to draft"));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<ContentResponse>> scheduleContent(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequest request,
            @RequestHeader("X-User-Id") UUID editorId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Scheduling content: {} for: {} by editor: {}", id, request.getScheduledAt(), editorId);
        ContentResponse response = contentService.scheduleContent(id, request.getScheduledAt(), editorId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content scheduled for auto-publication"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<ContentResponse>> publishContent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID editorId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Publishing content: {} by editor: {}", id, editorId);
        ContentResponse response = contentService.publishContent(id, editorId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content published"));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<ContentResponse>> unpublishContent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Unpublishing content: {} by user: {}", id, userId);
        ContentResponse response = contentService.unpublishContent(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content unpublished — returned to draft"));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<ContentResponse>> archiveContent(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Archiving content: {} by user: {}", id, userId);
        ContentResponse response = contentService.archiveContent(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content archived"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ContentResponse>> restoreFromArchive(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Restoring content from archive: {} by user: {}", id, userId);
        ContentResponse response = contentService.restoreFromArchive(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Content restored from archive"));
    }

    // ─── Compliance ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/compliance/approve")
    public ResponseEntity<ApiResponse<ContentResponse>> approveCompliance(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID officerId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Compliance approved for content: {} by officer: {}", id, officerId);
        ContentResponse response = contentService.approveCompliance(id, officerId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "Compliance approved"));
    }

    @PostMapping("/{id}/compliance/reject")
    public ResponseEntity<ApiResponse<ContentResponse>> rejectCompliance(
            @PathVariable UUID id,
            @Valid @RequestBody ComplianceDecisionRequest request,
            @RequestHeader("X-User-Id") UUID officerId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Compliance rejected for content: {} by officer: {}", id, officerId);
        ContentResponse response = contentService.rejectCompliance(id, officerId, userRoles, request.getReason());
        return ResponseEntity.ok(ApiResponse.success(response, "Compliance rejected — content returned to draft"));
    }

    // ─── Versioning ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/revise")
    public ResponseEntity<ApiResponse<ContentResponse>> createContentRevision(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        log.info("Creating new revision of content: {} by user: {}", id, userId);
        ContentResponse response = contentService.createContentRevision(id, userId, userRoles);
        return ResponseEntity.ok(ApiResponse.success(response, "New revision created"));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<VersionHistoryResponse>>> getVersionHistory(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {

        List<VersionHistoryResponse> versions = contentService.getVersionHistoryRich(id);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    // ─── Audit trail ─────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    public ResponseEntity<ApiResponse<List<WorkflowAuditLog>>> getAuditTrail(
            @PathVariable UUID id,
            @RequestHeader("X-User-Roles") String userRoles) {

        List<WorkflowAuditLog> trail = contentService.getWorkflowAuditTrail(id);
        return ResponseEntity.ok(ApiResponse.success(trail));
    }

    // ─── List / search ────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<ContentResponse>>> listContent(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "type") String contentType,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean highlighted,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        ContentFilter filter = ContentFilter.builder()
                .brandId(brandId)
                .status(status != null ? com.allocator.contentservice.model.ContentStatus.valueOf(status.toUpperCase()) : null)
                .contentType(contentType != null ? com.allocator.contentservice.model.ContentType.valueOf(contentType.toUpperCase()) : null)
                .categoryId(categoryId)
                .topic(topic)
                .sector(sector)
                .year(year)
                .tags(tags != null && !tags.isBlank()
                        ? java.util.Arrays.stream(tags.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList()
                        : null)
                .keyword(keyword)
                .featured(featured)
                .highlighted(highlighted)
                .createdFrom(createdFrom != null && !createdFrom.isBlank()
                        ? java.time.LocalDate.parse(createdFrom).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                        : null)
                .createdTo(createdTo != null && !createdTo.isBlank()
                        ? java.time.LocalDate.parse(createdTo).plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
                        : null)
                .build();

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ContentResponse> contentPage = contentService.listContent(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiResponse.PageResponse.from(contentPage)));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<ContentResponse>>> listContentByBrand(
            @PathVariable UUID brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ContentResponse> contentPage = contentService.listContentByBrand(brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiResponse.PageResponse.from(contentPage)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<ContentResponse>>> listContentByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        com.allocator.contentservice.model.ContentStatus contentStatus =
                com.allocator.contentservice.model.ContentStatus.valueOf(status.toUpperCase());

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ContentResponse> contentPage = contentService.listContentByStatus(contentStatus, pageable);
        return ResponseEntity.ok(ApiResponse.success(ApiResponse.PageResponse.from(contentPage)));
    }
}
