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
public class CrossrefExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String CROSSREF_API = "https://api.crossref.org/works/";

    @Override public String getName() { return "Crossref"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.DOI; }
    @Override public int getPriority() { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String url = CROSSREF_API + identifier.getValue();
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(json);
            JsonNode msg = root.path("message");

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.JOURNAL_ARTICLE)
                    .doi(identifier.getValue())
                    .url("https://doi.org/" + identifier.getValue());

            JsonNode titleNode = msg.path("title");
            if (titleNode.isArray() && titleNode.size() > 0)
                src.title(titleNode.get(0).asText());

            JsonNode containerNode = msg.path("container-title");
            if (containerNode.isArray() && containerNode.size() > 0)
                src.containerTitle(containerNode.get(0).asText());

            JsonNode authors = msg.path("author");
            if (authors.isArray()) {
                List<CitationAuthor> authorList = new ArrayList<>();
                for (JsonNode a : authors) {
                    CitationAuthor.CitationAuthorBuilder ab = CitationAuthor.builder()
                            .firstName(a.path("given").asText(""))
                            .lastName(a.path("family").asText(""))
                            .role("AUTHOR");
                    if (!a.path("ORCID").isMissingNode())
                        ab.orcid(a.path("ORCID").asText());
                    authorList.add(ab.build());
                }
                src.authors(authorList);
            }

            JsonNode published = msg.path("published");
            if (published.isMissingNode()) published = msg.path("published-print");
            JsonNode dateParts = published.path("date-parts");
            if (dateParts.isArray() && dateParts.size() > 0 && dateParts.get(0).isArray()) {
                JsonNode parts = dateParts.get(0);
                if (parts.size() > 0) src.pubYear(parts.get(0).asInt());
                if (parts.size() > 1) src.pubMonth(parts.get(1).asInt());
                if (parts.size() > 2) src.pubDay(parts.get(2).asInt());
            }

            src.volume(msg.path("volume").asText(null));
            src.issue(msg.path("issue").asText(null));

            String page = msg.path("page").asText(null);
            if (page != null && page.contains("-")) {
                String[] parts = page.split("-", 2);
                src.pageStart(parts[0].trim());
                src.pageEnd(parts[1].trim());
            } else if (page != null) {
                src.pageStart(page.trim());
            }

            src.publisher(msg.path("publisher").asText(null));
            src.abstractText(msg.path("abstract").asText(null));

            String type = msg.path("type").asText("");
            src.resourceType(mapCrossrefType(type));

            String link = msg.path("URL").asText(null);
            if (link != null) src.url(link);

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(95)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("Crossref extraction failed for DOI {}: {}", identifier.getValue(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }

    private ResourceType mapCrossrefType(String crossrefType) {
        return switch (crossrefType) {
            case "journal-article" -> ResourceType.JOURNAL_ARTICLE;
            case "book" -> ResourceType.BOOK;
            case "book-chapter" -> ResourceType.BOOK_CHAPTER;
            case "proceedings-article" -> ResourceType.CONFERENCE_PAPER;
            case "report" -> ResourceType.REPORT;
            case "dataset" -> ResourceType.DATASET;
            case "dissertation" -> ResourceType.DISSERTATION;
            default -> ResourceType.JOURNAL_ARTICLE;
        };
    }
}
