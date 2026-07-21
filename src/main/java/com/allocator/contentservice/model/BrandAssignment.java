package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks which users are assigned to which brands, and in what editorial capacity.
 * Used by ContentWorkflowService to enforce brand-scoped access control.
 */
@Entity
@Table(name = "brand_assignments", indexes = {
    @Index(name = "idx_brand_assignment_user_id", columnList = "user_id"),
    @Index(name = "idx_brand_assignment_brand_id", columnList = "brand_id"),
    @Index(name = "idx_brand_assignment_user_brand", columnList = "user_id, brand_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) assignedAt = Instant.now();
    }
}
