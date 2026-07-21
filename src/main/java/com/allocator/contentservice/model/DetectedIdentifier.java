package com.allocator.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectedIdentifier {
    private IdentifierType type;
    private String value;
    private String normalizedInput;
    private String fetchedContent;
}
