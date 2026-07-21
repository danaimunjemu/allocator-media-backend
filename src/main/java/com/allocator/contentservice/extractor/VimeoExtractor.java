package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Extracts Vimeo video metadata via the oEmbed API.
 * Falls back to JSON-LD if oEmbed is insufficient.
 * Priority 1 (before the generic JSON-LD extractor at priority 3).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class VimeoExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OEMBED_URL =
            "https://vimeo.com/api/oembed.json?url=";

    @Override public String getName()                       { return "Vimeo"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.VIMEO_URL; }
    @Override public int getPriority()                     { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String videoUrl = identifier.getNormalizedInput();

            // Phase 1: oEmbed
            String oembedJson = restTemplate.getForObject(
                    OEMBED_URL + java.net.URLEncoder.encode(videoUrl, java.nio.charset.StandardCharsets.UTF_8),
                    String.class);
            JsonNode oembed = objectMapper.readTree(oembedJson);

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(videoUrl)
                    .resourceType(ResourceType.VIMEO_VIDEO)
                    .url(videoUrl);

            src.title(oembed.path("title").asText(null));
            src.channelName(oembed.path("author_name").asText(null));
            src.thumbnailUrl(oembed.path("thumbnail_url").asText(null));
            src.websiteName("Vimeo");

            // Duration from oEmbed (seconds → mm:ss)
            int durationSecs = oembed.path("duration").asInt(0);
            if (durationSecs > 0) {
                src.duration(String.format("%d:%02d", durationSecs / 60, durationSecs % 60));
            }

            // Phase 2: parse upload_date from JSON-LD in pre-fetched HTML if present
            String fetchedHtml = identifier.getFetchedContent();
            if (fetchedHtml != null) {
                extractFromJsonLd(fetchedHtml, src);
            }

            // Build author list from channel name
            String authorName = oembed.path("author_name").asText(null);
            if (authorName != null) {
                src.authors(List.of(CitationAuthor.builder()
                        .lastName(authorName)
                        .firstName("")
                        .role("AUTHOR")
                        .build()));
            }

            int confidence = src.build().getPubYear() != null ? 90 : 75;

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(confidence)
                    .extractorName(getName())
                    .requiresManualCompletion(src.build().getPubYear() == null)
                    .build();

        } catch (Exception e) {
            log.warn("Vimeo extraction failed for {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }

    private void extractFromJsonLd(String html, CitationSource.CitationSourceBuilder src) {
        try {
            int start = html.indexOf("\"uploadDate\"");
            if (start >= 0) {
                int valStart = html.indexOf("\"", start + 13) + 1;
                int valEnd = html.indexOf("\"", valStart);
                if (valEnd > valStart) {
                    String dateStr = html.substring(valStart, valEnd);
                    if (dateStr.length() >= 4) {
                        src.pubYear(Integer.parseInt(dateStr.substring(0, 4)));
                        if (dateStr.length() >= 7) src.pubMonth(Integer.parseInt(dateStr.substring(5, 7)));
                    }
                }
            }
            // Description
            int descStart = html.indexOf("\"description\"");
            if (descStart >= 0) {
                int valStart = html.indexOf("\"", descStart + 14) + 1;
                int valEnd = html.indexOf("\"", valStart);
                if (valEnd > valStart) {
                    src.description(html.substring(valStart, valEnd));
                }
            }
        } catch (Exception ignored) {}
    }
}
