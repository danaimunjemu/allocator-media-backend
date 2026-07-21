package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.EditLock;
import com.allocator.contentservice.service.EditLockService;
import com.allocator.contentservice.service.EditLockService.LockConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content/{contentId}/lock")
@RequiredArgsConstructor
@Slf4j
public class EditLockController {

    private final EditLockService lockService;

    record LockConflictResponse(UUID userId, String userName, Instant acquiredAt) {}

    @GetMapping
    public ResponseEntity<?> getLock(@PathVariable UUID contentId) {
        Optional<EditLock> lock = lockService.getLock(contentId);
        if (lock.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ApiResponse.success(lock.get()));
    }

    @PostMapping
    public ResponseEntity<?> acquireLock(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Roles") String userRoles,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        try {
            EditLock lock = lockService.acquireLock(contentId, userId,
                    userName != null ? userName : userId.toString(), userRoles);
            return ResponseEntity.ok(ApiResponse.success(lock, "Lock acquired"));
        } catch (LockConflictException e) {
            EditLock holder = e.getLock();
            LockConflictResponse conflict = new LockConflictResponse(
                    holder.getUserId(), holder.getUserName(), holder.getAcquiredAt());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/heartbeat")
    public ResponseEntity<ApiResponse<EditLock>> renewLock(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId) {
        EditLock renewed = lockService.renewLock(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(renewed));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> releaseLock(
            @PathVariable UUID contentId,
            @RequestHeader("X-User-Id") UUID userId) {
        lockService.releaseLock(contentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Lock released"));
    }
}
