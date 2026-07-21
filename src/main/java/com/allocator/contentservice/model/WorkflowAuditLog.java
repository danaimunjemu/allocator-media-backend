package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only record of every workflow state transition.
 * Records must never be updated or deleted — they form a tamper-evident audit chain.
 */
@Entity
@Table(name = "workflow_audit_log", indexes = {
    @Index(name = "idx_wal_content_id", columnList = "content_id"),
    @Index(name = "idx_wal_brand_id", columnList = "brand_id"),
    @Index(name = "idx_wal_actor_id", columnList = "actor_id"),
    @Index(name = "idx_wal_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id")
    private UUID brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private WorkflowEventType eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", length = 50)
    private String toStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
