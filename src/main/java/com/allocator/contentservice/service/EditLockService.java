package com.allocator.contentservice.service;

import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditLockService {

    private static final long LOCK_DURATION_MINUTES = 30;

    private final EditLockRepository lockRepository;
    private final InAppNotificationRepository notificationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;

    public Optional<EditLock> getLock(UUID contentId) {
        return lockRepository.findByContentId(contentId)
                .filter(lock -> lock.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public EditLock acquireLock(UUID contentId, UUID userId, String userName, String userRole) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);

        int affected = lockRepository.upsertLock(UUID.randomUUID(), contentId, userId, userName, userRole, now, expiresAt);

        if (affected == 0) {
            // A different user holds a still-live lock — the upsert's WHERE clause
            // made it a no-op, so surface the current holder as a conflict.
            EditLock lock = lockRepository.findByContentId(contentId)
                    .orElseThrow(() -> new IllegalStateException("Lock conflict but no existing lock found for content: " + contentId));
            throw new LockConflictException(lock);
        }

        EditLock lock = lockRepository.findByContentId(contentId)
                .orElseThrow(() -> new IllegalStateException("Lock upserted but not found for content: " + contentId));
        recordAudit(contentId, null, WorkflowEventType.EDIT_LOCK_ACQUIRED, userId, userRole, null);
        log.info("Edit lock acquired/renewed for content {} by user {}", contentId, userId);
        return lock;
    }

    @Transactional
    public EditLock renewLock(UUID contentId, UUID userId) {
        EditLock lock = lockRepository.findByContentId(contentId)
                .orElseThrow(() -> new IllegalStateException("No lock held for content: " + contentId));
        if (!lock.getUserId().equals(userId)) {
            throw new IllegalStateException("Lock is held by a different user");
        }
        Instant now = Instant.now();
        lock.setHeartbeatAt(now);
        lock.setExpiresAt(now.plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
        return lockRepository.save(lock);
    }

    @Transactional
    public void releaseLock(UUID contentId, UUID userId) {
        lockRepository.findByContentId(contentId).ifPresent(lock -> {
            if (!lock.getUserId().equals(userId)) {
                throw new IllegalStateException("Cannot release a lock held by another user");
            }
            lockRepository.deleteByContentId(contentId);
            recordAudit(contentId, null, WorkflowEventType.EDIT_LOCK_RELEASED, userId, null, null);
            log.info("Edit lock released for content {} by user {}", contentId, userId);
        });
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cleanExpiredLocks() {
        lockRepository.deleteExpiredLocks(Instant.now());
    }

    private void recordAudit(UUID contentId, UUID brandId, WorkflowEventType eventType,
                              UUID actorId, String actorRole, String reason) {
        auditLogRepository.save(WorkflowAuditLog.builder()
                .contentId(contentId).brandId(brandId).eventType(eventType)
                .actorId(actorId).actorRole(actorRole).reason(reason)
                .timestamp(Instant.now()).build());
    }

    // ── LockConflictException ─────────────────────────────────────────────────

    public static class LockConflictException extends RuntimeException {
        private final EditLock lock;

        public LockConflictException(EditLock lock) {
            super("Content is being edited by " + lock.getUserName());
            this.lock = lock;
        }

        public EditLock getLock() { return lock; }
    }
}
