package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.DetectedIdentifier;
import com.allocator.contentservice.model.ExtractionResult;
import com.allocator.contentservice.model.IdentifierType;

public interface MetadataExtractor {
    String getName();
    boolean supports(IdentifierType identifierType);
    int getPriority();
    ExtractionResult extract(DetectedIdentifier identifier);
}
