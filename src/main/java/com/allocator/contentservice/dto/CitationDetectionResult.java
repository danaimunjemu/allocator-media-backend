package com.allocator.contentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationDetectionResult {

    private UUID importHistoryId;
    private String extractorUsed;
    private int confidence;
    private boolean requiresManualCompletion;
    private String failureReason;
    private String resourceType;
    private String detectedIdentifierType;
    private String detectedIdentifierValue;

    // All formatted citation strings by style
    private Map<String, String> generatedCitations;

    // Pre-built request the client can edit and POST to /references
    private ReferenceRequest suggestedRequest;
}
