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

/**
 * Extracts citation metadata from the PubMed E-utilities API.
 * Endpoint: https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&id=PMID
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PubMedExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String ESUMMARY_URL =
            "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&id=";

    @Override public String getName()                       { return "PubMed"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.PMID; }
    @Override public int getPriority()                     { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String pmid = identifier.getValue();
            String json = restTemplate.getForObject(ESUMMARY_URL + pmid, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode result = root.path("result").path(pmid);

            if (result.isMissingNode()) {
                return ExtractionResult.failed(getName(), "PMID not found in PubMed response");
            }

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.JOURNAL_ARTICLE)
                    .pmid(pmid)
                    .url("https://pubmed.ncbi.nlm.nih.gov/" + pmid);

            src.title(result.path("title").asText(null));
            src.containerTitle(result.path("source").asText(null));
            src.volume(result.path("volume").asText(null));
            src.issue(result.path("issue").asText(null));
            src.publisher(result.path("publishername").asText(null));

            // Publication date
            String pubDate = result.path("pubdate").asText(null);
            if (pubDate != null && !pubDate.isBlank()) {
                String[] parts = pubDate.split(" ");
                try { src.pubYear(Integer.parseInt(parts[0])); } catch (NumberFormatException ignored) {}
            }

            // Pages
            String pages = result.path("pages").asText(null);
            if (pages != null && pages.contains("-")) {
                String[] parts = pages.split("-", 2);
                src.pageStart(parts[0].trim());
                src.pageEnd(parts[1].trim());
            } else if (pages != null) {
                src.pageStart(pages.trim());
            }

            // Authors
            JsonNode authorsNode = result.path("authors");
            if (authorsNode.isArray()) {
                List<CitationAuthor> authors = new ArrayList<>();
                for (JsonNode a : authorsNode) {
                    String name = a.path("name").asText("");
                    String[] nameParts = name.split(",", 2);
                    CitationAuthor author = CitationAuthor.builder()
                            .lastName(nameParts[0].trim())
                            .firstName(nameParts.length > 1 ? nameParts[1].trim() : "")
                            .role("AUTHOR")
                            .build();
                    authors.add(author);
                }
                src.authors(authors);
            }

            // DOI from articleids
            JsonNode articleIds = result.path("articleids");
            if (articleIds.isArray()) {
                for (JsonNode aid : articleIds) {
                    if ("doi".equals(aid.path("idtype").asText())) {
                        src.doi(aid.path("value").asText(null));
                    }
                }
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(90)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("PubMed extraction failed for {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }
}
