package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class HeuristicExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;

    private static final Pattern TITLE_TAG = Pattern.compile(
            "<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_DESCRIPTION = Pattern.compile(
            "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_AUTHOR = Pattern.compile(
            "<meta[^>]+name=[\"']author[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern H1_TAG = Pattern.compile(
            "<h1[^>]*>\\s*([^<]+)\\s*</h1>", Pattern.CASE_INSENSITIVE);

    @Override public String getName() { return "Heuristic"; }

    @Override
    public boolean supports(IdentifierType type) {
        return type == IdentifierType.GENERIC_URL
                || type == IdentifierType.NEWS_URL
                || type == IdentifierType.GOVERNMENT_URL
                || type == IdentifierType.PODCAST_URL
                || type == IdentifierType.VIMEO_URL
                || type == IdentifierType.YOUTUBE_URL;
    }

    @Override public int getPriority() { return 99; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String html = getHtml(identifier);

            String title = null;
            String description = null;
            String author = null;
            String host = null;

            if (html != null) {
                title = coalesce(extractPattern(H1_TAG, html), extractPattern(TITLE_TAG, html));
                description = extractPattern(META_DESCRIPTION, html);
                author = extractPattern(META_AUTHOR, html);
            }

            try {
                host = new URI(identifier.getNormalizedInput()).getHost();
                if (host != null && host.startsWith("www.")) host = host.substring(4);
            } catch (Exception ignored) {}

            if (title == null) title = host != null ? "Document from " + host : "Unknown Title";

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(ResourceType.WEBPAGE)
                    .url(identifier.getNormalizedInput())
                    .title(title)
                    .abstractText(description)
                    .websiteName(host)
                    .accessDate(LocalDate.now().toString())
                    .pubYear(LocalDate.now().getYear());

            if (author != null) {
                String[] parts = author.split(" ");
                src.authors(java.util.List.of(CitationAuthor.builder()
                        .firstName(parts.length > 1 ? author.substring(0, author.lastIndexOf(" ")).trim() : "")
                        .lastName(parts[parts.length - 1])
                        .role("AUTHOR").build()));
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(50)
                    .extractorName(getName())
                    .requiresManualCompletion(true)
                    .build();

        } catch (Exception e) {
            log.warn("Heuristic extraction failed for {}: {}", identifier.getNormalizedInput(), e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
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
            return null;
        }
    }

    private String extractPattern(Pattern p, String html) {
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1).trim() : null;
    }

    private String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
