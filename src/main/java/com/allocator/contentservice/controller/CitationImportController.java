package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.CitationDetectionResult;
import com.allocator.contentservice.dto.CitationImportRequest;
import com.allocator.contentservice.model.CitationImportHistory;
import com.allocator.contentservice.repository.CitationImportHistoryRepository;
import com.allocator.contentservice.service.CitationImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/citations")
@RequiredArgsConstructor
@Slf4j
public class CitationImportController {

    private final CitationImportService citationImportService;
    private final CitationImportHistoryRepository historyRepository;

    @PostMapping("/detect")
    public ResponseEntity<ApiResponse<CitationDetectionResult>> detect(
            @Valid @RequestBody CitationImportRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        CitationDetectionResult result = citationImportService.detect(request, userId);
        return ResponseEntity.ok(ApiResponse.success(result, "Citation detected"));
    }

    @PostMapping("/{historyId}/link/{referenceId}")
    public ResponseEntity<ApiResponse<Void>> linkReference(
            @PathVariable UUID historyId,
            @PathVariable UUID referenceId) {
        citationImportService.linkReferenceToHistory(historyId, referenceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Reference linked to import history"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CitationImportHistory>>> getHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        List<CitationImportHistory> history = userId != null
                ? historyRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId)
                : historyRepository.findTop50ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
