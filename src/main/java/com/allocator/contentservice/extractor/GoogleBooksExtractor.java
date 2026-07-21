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
public class GoogleBooksExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    @Override public String getName() { return "Google Books"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.ISBN; }
    @Override public int getPriority() { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String url = GOOGLE_BOOKS_API + identifier.getValue();
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            if (root.path("totalItems").asInt(0) == 0)
                return ExtractionResult.failed(getName(), "No results found for ISBN");

            JsonNode info = root.path("items").get(0).path("volumeInfo");

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.BOOK)
                    .isbn(identifier.getValue())
                    .title(info.path("title").asText(null))
                    .subtitle(info.path("subtitle").asText(null))
                    .publisher(info.path("publisher").asText(null))
                    .abstractText(info.path("description").asText(null))
                    .url(info.path("infoLink").asText(null));

            // Authors
            JsonNode authorsNode = info.path("authors");
            if (authorsNode.isArray()) {
                List<CitationAuthor> authors = new ArrayList<>();
                for (JsonNode a : authorsNode) {
                    String fullName = a.asText();
                    String[] parts = fullName.split(" ");
                    String lastName = parts[parts.length - 1];
                    String firstName = parts.length > 1 ? fullName.substring(0, fullName.length() - lastName.length()).trim() : "";
                    authors.add(CitationAuthor.builder().firstName(firstName).lastName(lastName).role("AUTHOR").build());
                }
                src.authors(authors);
            }

            // Publication date
            String pubDate = info.path("publishedDate").asText(null);
            if (pubDate != null) {
                try { src.pubYear(Integer.parseInt(pubDate.substring(0, 4))); } catch (Exception ignored) {}
                if (pubDate.length() >= 7) {
                    try { src.pubMonth(Integer.parseInt(pubDate.substring(5, 7))); } catch (Exception ignored) {}
                }
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(85)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("Google Books extraction failed for ISBN {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }
}
