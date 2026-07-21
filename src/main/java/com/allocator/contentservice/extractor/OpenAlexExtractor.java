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
public class OpenAlexExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OPENALEX_API = "https://api.openalex.org/works/doi:";

    @Override public String getName() { return "OpenAlex"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.DOI; }
    @Override public int getPriority() { return 2; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String url = OPENALEX_API + identifier.getValue() + "?mailto=support@allocatormedia.com";
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.JOURNAL_ARTICLE)
                    .doi(identifier.getValue())
                    .url("https://doi.org/" + identifier.getValue())
                    .title(root.path("title").asText(null));

            // Authors from authorships
            JsonNode authorships = root.path("authorships");
            if (authorships.isArray()) {
                List<CitationAuthor> authors = new ArrayList<>();
                for (JsonNode a : authorships) {
                    String displayName = a.path("author").path("display_name").asText("");
                    String[] parts = displayName.split(" ", 2);
                    authors.add(CitationAuthor.builder()
                            .firstName(parts.length > 1 ? parts[0] : "")
                            .lastName(parts.length > 1 ? parts[1] : displayName)
                            .role("AUTHOR")
                            .build());
                }
                src.authors(authors);
            }

            // Journal via primary_location
            JsonNode venue = root.path("primary_location").path("source");
            if (!venue.isMissingNode())
                src.containerTitle(venue.path("display_name").asText(null));

            // Publication year
            int year = root.path("publication_year").asInt(0);
            if (year > 0) src.pubYear(year);

            String pubDate = root.path("publication_date").asText(null);
            if (pubDate != null && pubDate.length() >= 7) {
                try { src.pubMonth(Integer.parseInt(pubDate.substring(5, 7))); } catch (Exception ignored) {}
            }

            // Biblio
            JsonNode biblio = root.path("biblio");
            src.volume(biblio.path("volume").asText(null));
            src.issue(biblio.path("issue").asText(null));
            src.pageStart(biblio.path("first_page").asText(null));
            src.pageEnd(biblio.path("last_page").asText(null));

            src.abstractText(root.path("abstract").asText(null));

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(90)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("OpenAlex extraction failed for DOI {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }
}
