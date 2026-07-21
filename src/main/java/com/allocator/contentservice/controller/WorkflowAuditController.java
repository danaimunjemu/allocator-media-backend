package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.dto.AuditQueryFilter;
import com.allocator.contentservice.model.WorkflowAuditLog;
import com.allocator.contentservice.model.WorkflowEventType;
import com.allocator.contentservice.repository.WorkflowAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
public class WorkflowAuditController {

    private final WorkflowAuditLogRepository auditLogRepository;

    /**
     * Global paginated audit log endpoint with optional filters.
     *
     * GET /api/v1/audit
     *   ?contentId=UUID
     *   &brandId=UUID
     *   &actorId=UUID
     *   &eventType=PUBLISHED
     *   &from=2025-01-01T00:00:00Z
     *   &to=2025-12-31T23:59:59Z
     *   &page=0&size=50&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<WorkflowAuditLog>>> getAuditLog(
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestParam(required = false) UUID contentId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "desc") String sortDir) {

        WorkflowEventType eventTypeEnum = null;
        if (eventType != null && !eventType.isBlank()) {
            try {
                eventTypeEnum = WorkflowEventType.valueOf(eventType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Unknown eventType: " + eventType));
            }
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(direction, "timestamp"));

        Page<WorkflowAuditLog> result = auditLogRepository.findFiltered(
                contentId, brandId, actorId, eventTypeEnum, from, to, pageable);

        log.info("Audit query — contentId={} brandId={} actorId={} eventType={} from={} to={} → {} results",
                contentId, brandId, actorId, eventTypeEnum, from, to, result.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(ApiResponse.PageResponse.from(result)));
    }

    @GetMapping("/content/{contentId}")
    public ResponseEntity<ApiResponse<java.util.List<WorkflowAuditLog>>> getContentAuditTrail(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Roles") String userRoles) {

        var trail = auditLogRepository.findByContentIdOrderByTimestampAsc(contentId);
        return ResponseEntity.ok(ApiResponse.success(trail));
    }
}
