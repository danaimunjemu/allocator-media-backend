package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonLdExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Pattern JSONLD_SCRIPT = Pattern.compile(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE);

    @Override public String getName() { return "JSON-LD"; }

    @Override
    public boolean supports(IdentifierType type) {
        return type == IdentifierType.GENERIC_URL
                || type == IdentifierType.NEWS_URL
                || type == IdentifierType.GOVERNMENT_URL
                || type == IdentifierType.PODCAST_URL
                || type == IdentifierType.VIMEO_URL;
    }

    @Override public int getPriority() { return 3; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String html = getHtml(identifier);
            if (html == null) return ExtractionResult.failed(getName(), "Could not fetch page");

            Matcher m = JSONLD_SCRIPT.matcher(html);
            while (m.find()) {
                try {
                    JsonNode ld = objectMapper.readTree(m.group(1).trim());
                    if (ld.isArray()) ld = ld.get(0);
                    CitationSource src = parseJsonLd(ld, identifier);
                    if (src != null) {
                        return ExtractionResult.builder()
                                .source(src)
                                .confidence(80)
                                .extractorName(getName())
                                .requiresManualCompletion(src.getTitle() == null)
                                .build();
                    }
                } catch (Exception ignored) {}
            }
            return ExtractionResult.failed(getName(), "No valid JSON-LD found");
        } catch (Exception e) {
            log.warn("JSON-LD extraction failed for {}: {}", identifier.getNormalizedInput(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }

    private CitationSource parseJsonLd(JsonNode ld, DetectedIdentifier identifier) {
        String type = ld.path("@type").asText("");
        String title = coalesce(ld.path("headline").asText(null), ld.path("name").asText(null));
        if (title == null) return null;

        CitationSource.CitationSourceBuilder src = CitationSource.builder()
                .rawInput(identifier.getNormalizedInput())
                .resourceType(mapJsonLdType(type))
                .url(identifier.getNormalizedInput())
                .title(title)
                .abstractText(ld.path("description").asText(null))
                .accessDate(LocalDate.now().toString());

        // Author
        JsonNode authorNode = ld.path("author");
        src.authors(parseAuthors(authorNode));

        // Publisher / site name
        JsonNode publisher = ld.path("publisher");
        if (!publisher.isMissingNode())
            src.websiteName(publisher.path("name").asText(null));

        // Date
        String dateStr = coalesce(ld.path("datePublished").asText(null), ld.path("dateCreated").asText(null));
        if (dateStr != null && dateStr.length() >= 4) {
            try { src.pubYear(Integer.parseInt(dateStr.substring(0, 4))); } catch (Exception ignored) {}
            if (dateStr.length() >= 7) {
                try { src.pubMonth(Integer.parseInt(dateStr.substring(5, 7))); } catch (Exception ignored) {}
            }
        }

        // DOI / ISBN if present
        src.doi(ld.path("identifier").asText(null));

        return src.build();
    }

    private List<CitationAuthor> parseAuthors(JsonNode authorNode) {
        List<CitationAuthor> authors = new ArrayList<>();
        if (authorNode.isMissingNode()) return authors;
        if (authorNode.isArray()) {
            for (JsonNode a : authorNode) authors.add(singleAuthor(a));
        } else {
            authors.add(singleAuthor(authorNode));
        }
        return authors;
    }

    private CitationAuthor singleAuthor(JsonNode a) {
        String name = coalesce(a.path("name").asText(null), a.asText(null));
        if (name == null) return CitationAuthor.builder().build();
        String[] parts = name.split(" ");
        String lastName = parts[parts.length - 1];
        String firstName = parts.length > 1 ? name.substring(0, name.length() - lastName.length()).trim() : "";
        return CitationAuthor.builder().firstName(firstName).lastName(lastName).role("AUTHOR").build();
    }

    private ResourceType mapJsonLdType(String ldType) {
        return switch (ldType) {
            case "NewsArticle", "ReportageNewsArticle" -> ResourceType.NEWS_ARTICLE;
            case "Article", "TechArticle", "ScholarlyArticle" -> ResourceType.JOURNAL_ARTICLE;
            case "Book" -> ResourceType.BOOK;
            case "BlogPosting" -> ResourceType.BLOG_POST;
            case "PodcastEpisode" -> ResourceType.PODCAST_EPISODE;
            case "VideoObject" -> ResourceType.YOUTUBE_VIDEO;
            case "GovernmentOrganization" -> ResourceType.GOVERNMENT_PUBLICATION;
            default -> ResourceType.WEBPAGE;
        };
    }

    private String getHtml(DetectedIdentifier identifier) {
        if (identifier.getFetchedContent() != null) return identifier.getFetchedContent();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 AllocatorMediaBot/1.0");
            ResponseEntity<String> resp = restTemplate.exchange(
                    identifier.getNormalizedInput(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("Failed to fetch URL {}: {}", identifier.getNormalizedInput(), e.getMessage());
            return null;
        }
    }

    private String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
