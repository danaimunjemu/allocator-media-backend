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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-phase YouTube metadata extractor.
 *
 * Phase 1 — oEmbed API     : title, channel, thumbnail         (confidence 80)
 * Phase 2 — JSON-LD        : uploadDate, description, duration (confidence 95)
 * Phase 3 — ytInitialData  : uploadDate, shortDescription      (confidence 90)
 * Phase 4 — meta itemprop  : uploadDate, duration              (confidence 80, kept same)
 *
 * The highest-quality source that provides an uploadDate determines the
 * final confidence score.  oEmbed is always attempted first and its fields
 * are used as the baseline; page-sourced fields enrich the result.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class YouTubeOEmbedExtractor implements MetadataExtractor {

    @Qualifier("citationRestTemplate")
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OEMBED_BASE = "https://www.youtube.com/oembed";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})");

    // JSON-LD <script> blocks
    private static final Pattern JSONLD_SCRIPT = Pattern.compile(
            "<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>([\\s\\S]*?)</script>",
            Pattern.CASE_INSENSITIVE);

    // ISO 8601 date from ytInitialPlayerResponse ("uploadDate":"2026-06-28T...")
    private static final Pattern YT_UPLOAD_DATE_ISO = Pattern.compile(
            "\"uploadDate\":\"(\\d{4}-\\d{2}-\\d{2}(?:T[^\"]{0,30})?)\"");

    // Localised date from ytInitialData engagement panel ("publishDate":{"simpleText":"28 Jun 2026"})
    private static final Pattern YT_PUBLISH_DATE_SIMPLE = Pattern.compile(
            "\"publishDate\":\\{\"simpleText\":\"([^\"]+)\"\\}");

    // Short description from ytInitialPlayerResponse
    private static final Pattern YT_SHORT_DESCRIPTION = Pattern.compile(
            "\"shortDescription\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Override public String getName()  { return "YouTube"; }
    @Override public boolean supports(IdentifierType type) { return type == IdentifierType.YOUTUBE_URL; }
    @Override public int getPriority() { return 1; }

    @Override
    public ExtractionResult extract(DetectedIdentifier identifier) {
        String url = identifier.getNormalizedInput();

        // ── Phase 1: oEmbed ────────────────────────────────────────────────────
        String title = null, channelName = null, thumbnailUrl = null;
        try {
            // Build the oEmbed URI via UriComponentsBuilder so Spring handles
            // percent-encoding of the url param value exactly once.
            // Passing a URI object to getForObject bypasses RestTemplate's own
            // URI template expansion, preventing double-encoding of the param.
            URI oembedUri = UriComponentsBuilder.fromHttpUrl(OEMBED_BASE)
                    .queryParam("url", url)
                    .queryParam("format", "json")
                    .encode()
                    .build()
                    .toUri();
            String json = restTemplate.getForObject(oembedUri, String.class);
            JsonNode root = objectMapper.readTree(json);
            title        = root.path("title").asText(null);
            channelName  = root.path("author_name").asText(null);
            thumbnailUrl = root.path("thumbnail_url").asText(null);
        } catch (Exception e) {
            log.warn("YouTube oEmbed failed for {}: {}", url, e.getMessage());
        }

        // ── Phase 2-4: Watch page HTML ─────────────────────────────────────────
        String html = identifier.getFetchedContent();
        if (html == null) {
            html = fetchPageHtml(url);
            if (html != null) identifier.setFetchedContent(html);
        }

        String uploadDate = null, description = null, duration = null;
        String metadataSource = "OEMBED";
        int confidence = 80;

        if (html != null) {
            // ── Phase 2: JSON-LD VideoObject ──────────────────────────────────
            Matcher jsMatcher = JSONLD_SCRIPT.matcher(html);
            while (jsMatcher.find() && uploadDate == null) {
                try {
                    JsonNode ld = findVideoObject(objectMapper.readTree(jsMatcher.group(1).trim()));
                    if (ld == null) continue;

                    String ldDate = ld.path("uploadDate").asText(null);
                    String ldDesc = ld.path("description").asText(null);
                    String ldDur  = ld.path("duration").asText(null);

                    if (thumbnailUrl == null) {
                        JsonNode tn = ld.path("thumbnailUrl");
                        thumbnailUrl = tn.isTextual() ? tn.asText()
                                : (tn.isArray() && tn.size() > 0 ? tn.get(0).asText() : null);
                    }
                    if (title == null && !ld.path("name").asText("").isBlank()) {
                        title = ld.path("name").asText();
                    }
                    if (ldDate != null && !ldDate.isBlank()) uploadDate = ldDate;
                    if (ldDesc != null && !ldDesc.isBlank()) description = ldDesc;
                    if (ldDur  != null && !ldDur.isBlank())  duration   = ldDur;

                    if (uploadDate != null) {
                        metadataSource = "JSON_LD";
                        confidence = 95;
                    }
                } catch (Exception ignored) {}
            }

            // ── Phase 3: ytInitialData ─────────────────────────────────────────
            if (uploadDate == null) {
                // Prefer ISO 8601 (most precise, from ytInitialPlayerResponse)
                Matcher isoM = YT_UPLOAD_DATE_ISO.matcher(html);
                if (isoM.find()) {
                    uploadDate = isoM.group(1).trim();
                    metadataSource = "YT_INITIAL_DATA";
                    confidence = 90;
                }
                // Fall back to localised "28 Jun 2026" format (from engagement panel)
                if (uploadDate == null) {
                    Matcher simpleM = YT_PUBLISH_DATE_SIMPLE.matcher(html);
                    if (simpleM.find()) {
                        uploadDate = simpleM.group(1).trim();
                        metadataSource = "YT_INITIAL_DATA";
                        confidence = 90;
                    }
                }
            }
            if (description == null) {
                Matcher descM = YT_SHORT_DESCRIPTION.matcher(html);
                if (descM.find()) {
                    description = unescapeJson(descM.group(1));
                }
            }

            // ── Phase 4: meta itemprop fallback ───────────────────────────────
            if (uploadDate == null) {
                String metaDate = metaItemprop(html, "uploadDate");
                if (metaDate != null) {
                    uploadDate = metaDate;
                    // date found but from weakest page source — confidence stays at 80
                }
            }
            if (duration == null) {
                duration = metaItemprop(html, "duration");
            }
        }

        // ── Hard fail: no data at all ──────────────────────────────────────────
        if (title == null && channelName == null) {
            return ExtractionResult.failed(getName(), "oEmbed failed and no metadata found in page");
        }

        // ── Parse date ────────────────────────────────────────────────────────
        LocalDate parsed  = parseDate(uploadDate);
        Integer pubYear   = parsed != null ? parsed.getYear()        : LocalDate.now().getYear();
        Integer pubMonth  = parsed != null ? parsed.getMonthValue()  : null;
        Integer pubDay    = parsed != null ? parsed.getDayOfMonth()  : null;

        // ── Video ID ──────────────────────────────────────────────────────────
        Matcher m = VIDEO_ID_PATTERN.matcher(url);
        String videoId = m.find() ? m.group(1) : null;

        CitationAuthor channel = CitationAuthor.builder()
                .firstName("").lastName(channelName != null ? channelName : "")
                .role("AUTHOR").build();

        CitationSource src = CitationSource.builder()
                .rawInput(url)
                .resourceType(ResourceType.YOUTUBE_VIDEO)
                .url(url)
                .title(title)
                .channelName(channelName)
                .platform("YouTube")
                .videoId(videoId)
                .authors(List.of(channel))
                .pubYear(pubYear)
                .pubMonth(pubMonth)
                .pubDay(pubDay)
                .websiteName("YouTube")
                .accessDate(LocalDate.now().toString())
                .thumbnailUrl(thumbnailUrl)
                .description(description)
                .abstractText(description)
                .duration(duration)
                .metadataSource(metadataSource)
                .build();

        boolean hasAll = title != null && channelName != null && parsed != null;

        return ExtractionResult.builder()
                .source(src)
                .confidence(confidence)
                .extractorName(getName())
                .requiresManualCompletion(!hasAll)
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String fetchPageHtml(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("YouTube page fetch failed for {}: {}", url, e.getMessage());
            return null;
        }
    }

    /** Returns the first VideoObject node from a JSON-LD root (object or array). */
    private static JsonNode findVideoObject(JsonNode root) {
        if (root == null) return null;
        if (root.isArray()) {
            for (int i = 0; i < root.size(); i++) {
                if ("VideoObject".equalsIgnoreCase(root.get(i).path("@type").asText(""))) {
                    return root.get(i);
                }
            }
            return null;
        }
        return "VideoObject".equalsIgnoreCase(root.path("@type").asText("")) ? root : null;
    }

    /** Parses ISO 8601, "28 Jun 2026", and "June 28, 2026" date strings. */
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        raw = raw.trim();
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            try { return LocalDate.parse(raw.substring(0, 10)); } catch (Exception ignored) {}
        }
        try { return LocalDate.parse(raw, DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)); }
        catch (Exception ignored) {}
        try { return LocalDate.parse(raw, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)); }
        catch (Exception ignored) {}
        log.debug("Could not parse YouTube date string: {}", raw);
        return null;
    }

    /** Reads a named meta itemprop value, handling both attribute orderings. */
    private static String metaItemprop(String html, String prop) {
        String q = Pattern.quote(prop);
        // itemprop before content
        Matcher m = Pattern.compile(
                "<meta[^>]+itemprop=[\"']" + q + "[\"'][^>]*content=[\"']([^\"']+)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) return m.group(1);
        // content before itemprop
        m = Pattern.compile(
                "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*itemprop=[\"']" + q + "[\"']",
                Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) return m.group(1);
        return null;
    }

    /** Unescapes JSON string escape sequences for the shortDescription field. */
    private static String unescapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/");
    }
}
