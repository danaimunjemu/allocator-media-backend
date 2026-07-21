package com.allocator.contentservice.service;

import com.allocator.contentservice.extractor.MetadataExtractor;
import com.allocator.contentservice.model.DetectedIdentifier;
import com.allocator.contentservice.model.ExtractionResult;
import com.allocator.contentservice.model.IdentifierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataExtractionPipeline {

    private final List<MetadataExtractor> extractors;

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;

    private static final int MIN_CONFIDENCE_THRESHOLD = 60;

    // URL-based types that benefit from pre-fetching
    private static final java.util.Set<IdentifierType> WEB_TYPES = java.util.Set.of(
            IdentifierType.GENERIC_URL, IdentifierType.NEWS_URL,
            IdentifierType.GOVERNMENT_URL, IdentifierType.PODCAST_URL,
            IdentifierType.VIMEO_URL);

    public ExtractionResult run(DetectedIdentifier identifier) {
        // Pre-fetch HTML once for web-based extractors to share
        if (WEB_TYPES.contains(identifier.getType())) {
            String html = fetchHtml(identifier.getNormalizedInput());
            if (html != null) identifier.setFetchedContent(html);
        }

        List<MetadataExtractor> candidates = extractors.stream()
                .filter(e -> e.supports(identifier.getType()))
                .sorted(Comparator.comparingInt(MetadataExtractor::getPriority))
                .toList();

        if (candidates.isEmpty()) {
            log.warn("No extractor found for identifier type: {}", identifier.getType());
            return ExtractionResult.failed("None", "No extractor available for type: " + identifier.getType());
        }

        ExtractionResult best = null;
        for (MetadataExtractor extractor : candidates) {
            try {
                log.debug("Trying extractor: {} for type: {}", extractor.getName(), identifier.getType());
                ExtractionResult result = extractor.extract(identifier);
                if (result.isSuccessful()) {
                    if (best == null || result.getConfidence() > best.getConfidence()) {
                        best = result;
                    }
                    if (result.getConfidence() >= MIN_CONFIDENCE_THRESHOLD) {
                        log.info("Extraction succeeded via {} with confidence {}", extractor.getName(), result.getConfidence());
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("Extractor {} threw exception: {}", extractor.getName(), e.getMessage());
            }
        }

        if (best != null) {
            log.info("Best extraction: {} at confidence {} (below threshold, marking manual)", best.getExtractorName(), best.getConfidence());
            best.setRequiresManualCompletion(true);
            return best;
        }

        return ExtractionResult.builder()
                .confidence(0)
                .extractorName("None")
                .requiresManualCompletion(true)
                .failureReason("All extractors failed")
                .build();
    }

    private String fetchHtml(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 AllocatorMediaBot/1.0");
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("Pipeline pre-fetch failed for {}: {}", url, e.getMessage());
            return null;
        }
    }
}
