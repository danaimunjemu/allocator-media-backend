package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citation_import_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "raw_input", nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Column(name = "detected_identifier_type")
    private String detectedIdentifierType;

    @Column(name = "detected_identifier_value")
    private String detectedIdentifierValue;

    @Column(name = "extractor_used")
    private String extractorUsed;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "created_reference_id")
    private UUID createdReferenceId;

    @Column(name = "requires_manual_completion")
    private Boolean requiresManualCompletion;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
