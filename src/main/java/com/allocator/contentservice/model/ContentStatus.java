package com.allocator.contentservice.model;

/**
 * All reachable states in the editorial workflow.
 *
 * Standard flow:     DRAFT → REVIEW → APPROVED → PUBLISHED → ARCHIVED
 * Compliance flow:   DRAFT → REVIEW → APPROVED → COMPLIANCE_APPROVED → PUBLISHED → ARCHIVED
 * Rejection:         REVIEW → DRAFT  (returned to author)
 * Retract approval:  APPROVED → DRAFT
 * Unpublish:         PUBLISHED → DRAFT
 */
public enum ContentStatus {
    DRAFT,
    REVIEW,
    APPROVED,
    COMPLIANCE_APPROVED,
    PUBLISHED,
    ARCHIVED
}
