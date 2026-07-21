package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "edit_locks", indexes = {
    @Index(name = "idx_el_user_id",    columnList = "user_id"),
    @Index(name = "idx_el_expires_at", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditLock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false, unique = true)
    private UUID contentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(name = "acquired_at", nullable = false)
    private Instant acquiredAt;

    @Column(name = "heartbeat_at", nullable = false)
    private Instant heartbeatAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (acquiredAt == null) acquiredAt = Instant.now();
        if (heartbeatAt == null) heartbeatAt = Instant.now();
    }
}
