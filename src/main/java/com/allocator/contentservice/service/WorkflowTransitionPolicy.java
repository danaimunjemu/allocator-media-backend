package com.allocator.contentservice.service;

import com.allocator.contentservice.model.ContentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for all allowed workflow state transitions.
 * Every status change in the system must pass through here.
 * No direct status mutations are permitted outside this class.
 *
 * Allowed transition map:
 *
 *   DRAFT              → REVIEW
 *   REVIEW             → APPROVED | DRAFT         (DRAFT = editorial rejection)
 *   APPROVED           → COMPLIANCE_APPROVED | PUBLISHED | DRAFT   (DRAFT = retract)
 *   COMPLIANCE_APPROVED→ PUBLISHED | DRAFT                         (DRAFT = compliance rejection)
 *   PUBLISHED          → ARCHIVED | DRAFT                          (DRAFT = unpublish)
 *   ARCHIVED           → DRAFT                                     (restore)
 */
@Service
public class WorkflowTransitionPolicy {

    private static final Map<ContentStatus, Set<ContentStatus>> ALLOWED = Map.of(
        ContentStatus.DRAFT,               Set.of(ContentStatus.REVIEW),
        ContentStatus.REVIEW,              Set.of(ContentStatus.APPROVED, ContentStatus.DRAFT),
        ContentStatus.APPROVED,            Set.of(ContentStatus.COMPLIANCE_APPROVED, ContentStatus.PUBLISHED, ContentStatus.DRAFT),
        ContentStatus.COMPLIANCE_APPROVED, Set.of(ContentStatus.PUBLISHED, ContentStatus.DRAFT),
        ContentStatus.PUBLISHED,           Set.of(ContentStatus.ARCHIVED, ContentStatus.DRAFT),
        ContentStatus.ARCHIVED,            Set.of(ContentStatus.DRAFT)
    );

    /**
     * Throws {@link IllegalStateException} if the transition is not permitted.
     */
    public void assertTransitionAllowed(ContentStatus from, ContentStatus to) {
        Set<ContentStatus> allowed = ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException(
                "Invalid workflow transition: " + from + " → " + to +
                ". Permitted targets from " + from + ": " + allowed);
        }
    }

    public boolean isTransitionAllowed(ContentStatus from, ContentStatus to) {
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public Set<ContentStatus> allowedFrom(ContentStatus status) {
        return ALLOWED.getOrDefault(status, Set.of());
    }
}
