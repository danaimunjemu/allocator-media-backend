package com.allocator.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {
    private CitationSource source;
    private int confidence;
    private String extractorName;
    private boolean requiresManualCompletion;
    private String failureReason;

    public static ExtractionResult failed(String extractorName, String reason) {
        return ExtractionResult.builder()
                .extractorName(extractorName)
                .confidence(0)
                .requiresManualCompletion(true)
                .failureReason(reason)
                .build();
    }

    public boolean isSuccessful() {
        return source != null && confidence > 0;
    }
}
