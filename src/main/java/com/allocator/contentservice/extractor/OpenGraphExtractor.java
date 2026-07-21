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

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenGraphExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;

    private static final Pattern OG_TITLE = metaPattern("og:title");
    private static final Pattern OG_SITE_NAME = metaPattern("og:site_name");
    private static final Pattern OG_URL = metaPattern("og:url");
    private static final Pattern OG_DESCRIPTION = metaPattern("og:description");
    private static final Pattern OG_TYPE = metaPattern("og:type");
    private static final Pattern META_AUTHOR = Pattern.compile(
            "<meta[^>]+name=[\"']author[\"'][^>]+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_PUBLISHED = Pattern.compile(
            "<meta[^>]+(?:property=[\"']article:published_time[\"']|name=[\"']date[\"'])[^>]+content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE);

    @Override public String getName() { return "OpenGraph"; }

    @Override
    public boolean supports(IdentifierType type) {
        return type == IdentifierType.GENERIC_URL
                || type == IdentifierType.NEWS_URL
                || type == IdentifierType.GOVERNMENT_URL
                || type == IdentifierType.PODCAST_URL;
    }

    @Override public int getPriority() { return 4; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        try {
            String html = getHtml(identifier);
            if (html == null) return ExtractionResult.failed(getName(), "Could not fetch page");

            String title = extract(OG_TITLE, html);
            if (title == null) return ExtractionResult.failed(getName(), "No og:title found");

            String siteName = extract(OG_SITE_NAME, html);
            String ogUrl = extract(OG_URL, html);
            String description = extract(OG_DESCRIPTION, html);
            String author = extract(META_AUTHOR, html);
            String published = extract(META_PUBLISHED, html);
            String ogType = extract(OG_TYPE, html);

            CitationSource.CitationSourceBuilder src = CitationSource.builder()
                    .rawInput(identifier.getNormalizedInput())
                    .resourceType(mapOgType(ogType))
                    .url(ogUrl != null ? ogUrl : identifier.getNormalizedInput())
                    .title(title)
                    .websiteName(siteName)
                    .abstractText(description)
                    .accessDate(LocalDate.now().toString());

            if (author != null) {
                String[] parts = author.split(" ");
                src.authors(List.of(CitationAuthor.builder()
                        .firstName(parts.length > 1 ? author.substring(0, author.lastIndexOf(" ")).trim() : "")
                        .lastName(parts[parts.length - 1])
                        .role("AUTHOR").build()));
            }

            if (published != null && published.length() >= 4) {
                try { src.pubYear(Integer.parseInt(published.substring(0, 4))); } catch (Exception ignored) {}
                if (published.length() >= 7) {
                    try { src.pubMonth(Integer.parseInt(published.substring(5, 7))); } catch (Exception ignored) {}
                }
            }

            return ExtractionResult.builder()
                    .source(src.build())
                    .confidence(70)
                    .extractorName(getName())
                    .requiresManualCompletion(false)
                    .build();

        } catch (Exception e) {
            log.warn("OpenGraph extraction failed for {}: {}", identifier.getNormalizedInput(), e.getMessage());
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
            log.warn("Failed to fetch URL {}: {}", identifier.getNormalizedInput(), e.getMessage());
            return null;
        }
    }

    private String extract(Pattern p, String html) {
        Matcher m = p.matcher(html);
        return m.find() ? m.group(1).trim() : null;
    }

    private static Pattern metaPattern(String property) {
        return Pattern.compile(
                "<meta[^>]+property=[\"']" + Pattern.quote(property) + "[\"'][^>]+content=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE);
    }

    private ResourceType mapOgType(String ogType) {
        if (ogType == null) return ResourceType.WEBPAGE;
        return switch (ogType) {
            case "article" -> ResourceType.NEWS_ARTICLE;
            case "book" -> ResourceType.BOOK;
            case "video.other", "video.episode" -> ResourceType.YOUTUBE_VIDEO;
            case "music.song", "music.album" -> ResourceType.PODCAST_EPISODE;
            default -> ResourceType.WEBPAGE;
        };
    }
}
