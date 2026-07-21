package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.dto.ReferenceResponse;
import com.allocator.contentservice.service.DuplicateDetectionService;
import com.allocator.contentservice.service.ReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/references")
@RequiredArgsConstructor
@Slf4j
public class ReferenceController {

    private final ReferenceService referenceService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReferenceResponse>> create(
            @RequestBody ReferenceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId != null && request.getCreatedByUserId() == null) {
            request.setCreatedByUserId(userId);
        }
        ReferenceResponse response = referenceService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Reference created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(referenceService.getAll()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ReferenceResponse>>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.search(q, page, Math.min(size, 200))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferenceResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.get(id)));
    }

    @GetMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> getByContent(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.getByContentId(contentId)));
    }

    @GetMapping("/content/{contentId}/cited")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> getCitedByContent(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.getCitedByContentId(contentId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.getByUserId(userId)));
    }

    @PostMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<DuplicateDetectionService.DuplicateResult>> checkDuplicate(
            @RequestBody ReferenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.checkDuplicate(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferenceResponse>> update(
            @PathVariable UUID id,
            @RequestBody ReferenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.update(id, request)));
    }

    @PatchMapping("/{id}/inline-key")
    public ResponseEntity<ApiResponse<ReferenceResponse>> setInlineKey(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.setInlineKey(id, body.get("inlineKey"))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        referenceService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Reference deleted"));
    }

    @PostMapping("/{referenceId}/link/{contentId}")
    public ResponseEntity<ApiResponse<ReferenceResponse>> linkToContent(
            @PathVariable UUID referenceId,
            @PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.linkToContent(referenceId, contentId)));
    }

    @DeleteMapping("/{referenceId}/link/{contentId}")
    public ResponseEntity<ApiResponse<Void>> unlinkFromContent(
            @PathVariable UUID referenceId,
            @PathVariable UUID contentId) {
        referenceService.unlinkFromContent(referenceId, contentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unlinked"));
    }

    // ─── Export endpoints ──────────────────────────────────────────────────────

    @GetMapping("/export/bibtex")
    public ResponseEntity<String> exportBibTex(
            @RequestParam(required = false) List<UUID> ids) {
        String content = referenceService.exportBibTex(ids != null ? ids : List.of());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"references.bib\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/export/ris")
    public ResponseEntity<String> exportRis(
            @RequestParam(required = false) List<UUID> ids) {
        String content = referenceService.exportRis(ids != null ? ids : List.of());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"references.ris\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/export/csl-json")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportCslJson(
            @RequestParam(required = false) List<UUID> ids) {
        return ResponseEntity.ok(ApiResponse.success(referenceService.exportCslJson(ids != null ? ids : List.of())));
    }
}
