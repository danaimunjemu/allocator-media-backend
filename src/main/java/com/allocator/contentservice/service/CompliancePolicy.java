package com.allocator.contentservice.service;

import com.allocator.contentservice.model.ContentType;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Determines which content types require compliance review before publication.
 * Configurable here rather than scattered across the workflow service.
 *
 * Compliance flow:  APPROVED → COMPLIANCE_APPROVED → PUBLISHED
 * Standard flow:    APPROVED → PUBLISHED
 */
@Service
public class CompliancePolicy {

    private static final Set<ContentType> COMPLIANCE_REQUIRED_TYPES = Set.of(
        ContentType.RESEARCH,
        ContentType.INTERVIEW
    );

    public boolean requiresCompliance(ContentType contentType) {
        return contentType != null && COMPLIANCE_REQUIRED_TYPES.contains(contentType);
    }

    public Set<ContentType> getComplianceRequiredTypes() {
        return COMPLIANCE_REQUIRED_TYPES;
    }
}
