package com.allocator.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

// Append-only record of every newsletter workflow transition (draft -> in
// review -> scheduled -> sent, plus item removals during review). Mirrors
// contentservice's WorkflowAuditLog pattern but is campaign-scoped — never
// update or delete rows here.
@Entity
@Table(name = "campaign_audit_log", indexes = {
        @Index(name = "idx_campaign_audit_campaign_id", columnList = "campaign_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignAuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "brand_id")
    private UUID brandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CampaignWorkflowEventType eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role")
    private String actorRole;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        timestamp = LocalDateTime.now();
    }
}
