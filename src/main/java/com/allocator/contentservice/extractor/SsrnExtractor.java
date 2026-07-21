package com.allocator.contentservice.extractor;

import com.allocator.contentservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts metadata from SSRN paper pages using meta-tag and JSON-LD patterns on raw HTML.
 * SSRN uses standard <meta name="citation_*"> tags, so no Jsoup required.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SsrnExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;

    private static final Pattern META_CONTENT  = Pattern.compile("<meta[^>]+name=[\"']([^\"']+)[\"'][^>]+content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_CONTENT2 = Pattern.compile("<meta[^>]+content=[\"']([^\"']*)[\"'][^>]+name=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    @Override public String getName()                       { return "SSRN"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.GENERIC_URL; }
    @Override public int getPriority()                     { return 2; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        String url = identifier.getNormalizedInput();
        if (!isSsrnUrl(url)) {
            return ExtractionResult.failed(getName(), "Not an SSRN URL");
        }

        try {
            String html = identifier.getFetchedContent();
            if (html == null || html.isBlank()) {
                html = restTemplate.getForObject(url, String.class);
            }
            if (html == null) return ExtractionResult.failed(getName(), "Empty response");

            java.util.Map<String, String> meta = extractMetaTags(html);

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(url)
                    .resourceType(ResourceType.RESEARCH_PAPER)
                    .url(url)
                    .websiteName("SSRN");

            String title = meta.get("citation_title");
            if (title != null) src.title(title);

            String doi = meta.get("citation_doi");
            if (doi != null) src.doi(doi);

            String journal = meta.get("citation_journal_title");
            if (journal != null) src.containerTitle(journal);

            // Parse date
            String dateStr = meta.get("citation_date");
            if (dateStr != null) {
                String[] parts = dateStr.split("/");
                try {
                    if (parts.length >= 3) {
                        src.pubYear(Integer.parseInt(parts[2].trim()));
                        src.pubMonth(Integer.parseInt(parts[0].trim()));
                        src.pubDay(Integer.parseInt(parts[1].trim()));
                    } else if (parts.length == 1 && parts[0].trim().length() == 4) {
                        src.pubYear(Integer.parseInt(parts[0].trim()));
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Authors — citation_author can appear multiple times
            List<CitationAuthor> authors = extractAuthors(html);
            if (!authors.isEmpty()) src.authors(authors);

            // Abstract from og:description or citation_abstract
            String abs = meta.get("citation_abstract");
            if (abs == null) abs = meta.get("og:description");
            if (abs != null) src.abstractText(abs);

            boolean hasTitle = title != null;
            boolean hasAuthors = !authors.isEmpty();
            boolean hasYear = src.build().getPubYear() != null;
            int confidence = (hasTitle ? 30 : 0) + (hasAuthors ? 30 : 0) + (hasYear ? 25 : 0) + 10;

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(confidence)
                    .extractorName(getName())
                    .requiresManualCompletion(confidence < 60)
                    .build();

        } catch (Exception e) {
            log.warn("SSRN extraction failed for {}: {}", url, e.getMessage());
            return ExtractionResult.failed(getName(), e.getMessage());
        }
    }

    private java.util.Map<String, String> extractMetaTags(String html) {
        java.util.Map<String, String> meta = new java.util.LinkedHashMap<>();
        Matcher m1 = META_CONTENT.matcher(html);
        while (m1.find()) meta.putIfAbsent(m1.group(1).toLowerCase(), m1.group(2));
        Matcher m2 = META_CONTENT2.matcher(html);
        while (m2.find()) meta.putIfAbsent(m2.group(2).toLowerCase(), m2.group(1));
        return meta;
    }

    private List<CitationAuthor> extractAuthors(String html) {
        List<CitationAuthor> authors = new ArrayList<>();
        // SSRN uses <meta name="citation_author" content="Lastname, Firstname">
        Pattern p = Pattern.compile("<meta[^>]+name=[\"']citation_author[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        while (m.find()) {
            String name = m.group(1).trim();
            String[] parts = name.split(",", 2);
            authors.add(CitationAuthor.builder()
                    .lastName(parts[0].trim())
                    .firstName(parts.length > 1 ? parts[1].trim() : "")
                    .role("AUTHOR")
                    .build());
        }
        return authors;
    }

    private boolean isSsrnUrl(String url) {
        return url != null && url.contains("ssrn.com");
    }
}
