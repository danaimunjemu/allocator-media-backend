package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenLibraryExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OPEN_LIBRARY_API =
            "https://openlibrary.org/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data";

    @Override public String getName() { return "Open Library"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.ISBN; }
    @Override public int getPriority() { return 2; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String url = OPEN_LIBRARY_API.replace("{isbn}", identifier.getValue());
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            String key = "ISBN:" + identifier.getValue();
            JsonNode book = root.path(key);
            if (book.isMissingNode())
                return ExtractionResult.failed(getName(), "No Open Library entry found");

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.BOOK)
                    .isbn(identifier.getValue())
                    .title(book.path("title").asText(null))
                    .url(book.path("url").asText(null));

            // Publisher
            JsonNode publishers = book.path("publishers");
            if (publishers.isArray() && publishers.size() > 0)
                src.publisher(publishers.get(0).path("name").asText(null));

            // Publish date
            String pubDate = book.path("publish_date").asText(null);
            if (pubDate != null) {
                try { src.pubYear(Integer.parseInt(pubDate.replaceAll("[^0-9]", "").substring(0, 4))); }
                catch (Exception ignored) {}
            }

            // Authors
            JsonNode authorsNode = book.path("authors");
            if (authorsNode.isArray()) {
                List<CitationAuthor> authors = new ArrayList<>();
                for (JsonNode a : authorsNode) {
                    String fullName = a.path("name").asText("");
                    String[] parts = fullName.split(",", 2);
                    if (parts.length == 2) {
                        authors.add(CitationAuthor.builder()
                                .lastName(parts[0].trim())
                                .firstName(parts[1].trim())
                                .role("AUTHOR").build());
                    } else {
                        String[] nameParts = fullName.split(" ");
                        authors.add(CitationAuthor.builder()
                                .firstName(nameParts.length > 1 ? fullName.substring(0, fullName.lastIndexOf(" ")).trim() : "")
                                .lastName(nameParts[nameParts.length - 1])
                                .role("AUTHOR").build());
                    }
                }
                src.authors(authors);
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(80)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("Open Library extraction failed for ISBN {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }
}
