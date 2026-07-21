package com.allocator.contentservice.service;

import com.allocator.contentservice.extractor.YouTubeOEmbedExtractor;
import com.allocator.contentservice.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("YouTube multi-phase metadata extraction")
class YouTubeExtractionTest {

    @Mock RestTemplate restTemplate;

    YouTubeOEmbedExtractor extractor;
    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        extractor = new YouTubeOEmbedExtractor(restTemplate, mapper);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void givenOEmbed(String title, String channel, String thumb) {
        String json = String.format(
                "{\"title\":\"%s\",\"author_name\":\"%s\",\"thumbnail_url\":\"%s\"}",
                title, channel, thumb);
        // getForObject is now called with a URI object (not a String) to avoid
        // double-encoding — match any URI since we verify correctness via field assertions
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(json);
    }

    private void givenPageHtml(String html) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(html));
    }

    private void givenPageFails() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));
    }

    private DetectedIdentifier youtubeId(String videoId, String fetchedContent) {
        return DetectedIdentifier.builder()
                .type(IdentifierType.YOUTUBE_URL)
                .value(videoId)
                .normalizedInput("https://www.youtube.com/watch?v=" + videoId)
                .fetchedContent(fetchedContent)
                .build();
    }

    private DetectedIdentifier youtubeId(String videoId) {
        return youtubeId(videoId, null);
    }

    // ── Phase-specific extraction tests ───────────────────────────────────────

    @Nested
    @DisplayName("Phase 2 — JSON-LD VideoObject")
    class JsonLdTests {

        @Test
        @DisplayName("extracts uploadDate, description, and duration from JSON-LD VideoObject")
        void extractsFromJsonLd() {
            givenOEmbed("Test Video", "Test Channel", "https://i.ytimg.com/vi/abc/hqdefault.jpg");
            String html = "<html><script type=\"application/ld+json\">" +
                    "{\"@context\":\"https://schema.org\",\"@type\":\"VideoObject\"," +
                    "\"name\":\"Test Video\",\"uploadDate\":\"2024-03-15\"," +
                    "\"description\":\"A test description.\",\"duration\":\"PT15M33S\"," +
                    "\"thumbnailUrl\":\"https://i.ytimg.com/vi/abc/maxresdefault.jpg\"}" +
                    "</script></html>";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("abcdefghijk"));

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getConfidence()).isEqualTo(95);
            assertThat(result.getSource().getPubYear()).isEqualTo(2024);
            assertThat(result.getSource().getPubMonth()).isEqualTo(3);
            assertThat(result.getSource().getPubDay()).isEqualTo(15);
            assertThat(result.getSource().getDescription()).isEqualTo("A test description.");
            assertThat(result.getSource().getDuration()).isEqualTo("PT15M33S");
            assertThat(result.getSource().getMetadataSource()).isEqualTo("JSON_LD");
            assertThat(result.isRequiresManualCompletion()).isFalse();
        }

        @Test
        @DisplayName("handles JSON-LD array root containing VideoObject")
        void handlesJsonLdArray() {
            givenOEmbed("Array Video", "Channel", "https://i.ytimg.com/vi/x/hqdefault.jpg");
            String html = "<html><script type=\"application/ld+json\">" +
                    "[{\"@type\":\"WebPage\"},{\"@type\":\"VideoObject\"," +
                    "\"uploadDate\":\"2023-11-01\",\"description\":\"Desc\"}]" +
                    "</script></html>";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("xxxxxxxxxxx"));

            assertThat(result.getConfidence()).isEqualTo(95);
            assertThat(result.getSource().getPubYear()).isEqualTo(2023);
            assertThat(result.getSource().getPubMonth()).isEqualTo(11);
            assertThat(result.getSource().getMetadataSource()).isEqualTo("JSON_LD");
        }
    }

    @Nested
    @DisplayName("Phase 3 — ytInitialData")
    class YtInitialDataTests {

        @Test
        @DisplayName("extracts ISO 8601 uploadDate from ytInitialPlayerResponse")
        void extractsIsoUploadDate() {
            givenOEmbed("Podcast Episode", "Pasi Koetle", "https://i.ytimg.com/vi/SYatma2BXP8/hqdefault.jpg");
            // Simulate bot-deflection page: no JSON-LD, but ytInitialPlayerResponse embedded
            String html = "var ytInitialPlayerResponse = {\"videoDetails\":{" +
                    "\"uploadDate\":\"2026-06-28T00:00:00+00:00\"," +
                    "\"shortDescription\":\"On today's episode we have Penuel the black pen.\"" +
                    "}};";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("SYatma2BXP8"));

            assertThat(result.getConfidence()).isEqualTo(90);
            assertThat(result.getSource().getPubYear()).isEqualTo(2026);
            assertThat(result.getSource().getPubMonth()).isEqualTo(6);
            assertThat(result.getSource().getPubDay()).isEqualTo(28);
            assertThat(result.getSource().getDescription()).contains("Penuel");
            assertThat(result.getSource().getMetadataSource()).isEqualTo("YT_INITIAL_DATA");
            assertThat(result.isRequiresManualCompletion()).isFalse();
        }

        @Test
        @DisplayName("extracts localised date from ytInitialData engagement panel")
        void extractsLocalisedPublishDate() {
            givenOEmbed("Video Title", "Channel Name", "https://i.ytimg.com/vi/abc/hqdefault.jpg");
            // No JSON-LD, no uploadDate ISO, but publishDate simpleText
            String html = "var ytInitialData = {\"publishDate\":{\"simpleText\":\"28 Jun 2026\"}};";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("abcdefghijk"));

            assertThat(result.getConfidence()).isEqualTo(90);
            assertThat(result.getSource().getPubYear()).isEqualTo(2026);
            assertThat(result.getSource().getPubMonth()).isEqualTo(6);
            assertThat(result.getSource().getPubDay()).isEqualTo(28);
            assertThat(result.getSource().getMetadataSource()).isEqualTo("YT_INITIAL_DATA");
        }
    }

    @Nested
    @DisplayName("Phase 4 — meta itemprop fallback")
    class MetaItempropTests {

        @Test
        @DisplayName("extracts uploadDate and duration from meta itemprop tags")
        void extractsMetaItemprop() {
            givenOEmbed("Lecture", "University Channel", "https://i.ytimg.com/vi/lec/hqdefault.jpg");
            // Page has schema.org microdata but no JSON-LD or ytInitialData date
            String html = "<html>" +
                    "<meta itemprop=\"uploadDate\" content=\"2022-09-01\">" +
                    "<meta itemprop=\"duration\" content=\"PT1H23M45S\">" +
                    "</html>";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("lecturevideo1"));

            assertThat(result.getSource().getPubYear()).isEqualTo(2022);
            assertThat(result.getSource().getDuration()).isEqualTo("PT1H23M45S");
            // confidence stays 80 since itemprop is the weakest page source
            assertThat(result.getConfidence()).isEqualTo(80);
        }
    }

    @Nested
    @DisplayName("oEmbed-only path")
    class OEmbedOnlyTests {

        @Test
        @DisplayName("returns confidence 80 and requiresManualCompletion when page fetch fails")
        void oEmbedOnlyWhenPageUnavailable() {
            givenOEmbed("Great Video", "Great Channel", "https://i.ytimg.com/vi/gv/hqdefault.jpg");
            givenPageFails();

            ExtractionResult result = extractor.extract(youtubeId("greatvideo11"));

            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.getConfidence()).isEqualTo(80);
            assertThat(result.getSource().getTitle()).isEqualTo("Great Video");
            assertThat(result.getSource().getThumbnailUrl()).contains("hqdefault.jpg");
            assertThat(result.getSource().getMetadataSource()).isEqualTo("OEMBED");
            // upload date unknown → access year used, manual completion required
            assertThat(result.isRequiresManualCompletion()).isTrue();
        }

        @Test
        @DisplayName("uses pre-fetched HTML from DetectedIdentifier without making extra HTTP call")
        void usesPreFetchedContent() {
            givenOEmbed("Pre-cached Video", "Channel", "https://i.ytimg.com/vi/pc/hqdefault.jpg");
            String html = "\"uploadDate\":\"2025-01-15T00:00:00Z\"";

            // Identifier already has fetchedContent — no exchange() should be called
            ExtractionResult result = extractor.extract(youtubeId("preCachedVideo", html));

            assertThat(result.getSource().getPubYear()).isEqualTo(2025);
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(String.class));
        }

        @Test
        @DisplayName("fails cleanly when both oEmbed and page return nothing")
        void failsWhenBothSourcesFail() {
            when(restTemplate.getForObject(any(URI.class), eq(String.class)))
                    .thenThrow(new RuntimeException("404 Not Found"));
            givenPageFails();

            ExtractionResult result = extractor.extract(youtubeId("deletedvideo1"));

            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getConfidence()).isEqualTo(0);
            assertThat(result.getFailureReason()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Date parsing")
    class DateParsingTests {

        @Test
        @DisplayName("parses ISO 8601 with timezone offset")
        void parsesIso8601WithOffset() {
            LocalDate d = YouTubeOEmbedExtractor.parseDate("2026-06-28T00:00:00+00:00");
            assertThat(d).isEqualTo(LocalDate.of(2026, 6, 28));
        }

        @Test
        @DisplayName("parses abbreviated month format (28 Jun 2026)")
        void parsesAbbreviatedMonth() {
            LocalDate d = YouTubeOEmbedExtractor.parseDate("28 Jun 2026");
            assertThat(d).isEqualTo(LocalDate.of(2026, 6, 28));
        }

        @Test
        @DisplayName("parses full month format (June 28, 2026)")
        void parsesFullMonth() {
            LocalDate d = YouTubeOEmbedExtractor.parseDate("June 28, 2026");
            assertThat(d).isEqualTo(LocalDate.of(2026, 6, 28));
        }

        @Test
        @DisplayName("returns null for unparseable string")
        void returnsNullForGarbage() {
            assertThat(YouTubeOEmbedExtractor.parseDate("yesterday")).isNull();
            assertThat(YouTubeOEmbedExtractor.parseDate(null)).isNull();
            assertThat(YouTubeOEmbedExtractor.parseDate("  ")).isNull();
        }
    }

    @Nested
    @DisplayName("APA citation quality")
    class CitationQualityTests {

        @Test
        @DisplayName("APA citation uses actual upload date not access year")
        void apaCitationUsesUploadDate() {
            givenOEmbed("Documentary Film", "Film Channel", "https://i.ytimg.com/vi/doc/hqdefault.jpg");
            String html = "\"uploadDate\":\"2020-08-22T00:00:00Z\"";
            givenPageHtml(html);

            ExtractionResult result = extractor.extract(youtubeId("documentaryFilm"));
            CitationSource src = result.getSource();

            assertThat(src.getPubYear()).isEqualTo(2020);
            assertThat(src.getPubMonth()).isEqualTo(8);
            assertThat(src.getPubDay()).isEqualTo(22);

            // Build APA via formatter to confirm end-to-end
            com.allocator.contentservice.dto.ReferenceRequest req =
                    com.allocator.contentservice.dto.ReferenceRequest.builder()
                            .sourceType(ReferenceSourceType.ONLINE_VIDEO)
                            .title(src.getTitle())
                            .pubYear(src.getPubYear())
                            .pubMonth(src.getPubMonth())
                            .pubDay(src.getPubDay())
                            .websiteName("YouTube")
                            .url(src.getUrl())
                            .citationStyle(CitationStyle.APA_7)
                            .contributorsJson("[{\"role\":\"AUTHOR\",\"firstName\":\"\",\"lastName\":\"Film Channel\"}]")
                            .build();

            CitationFormatterService formatter = new CitationFormatterService(mapper);
            String apa = formatter.format(req);

            // Must contain the actual upload year, not the current access year
            assertThat(apa).contains("2020");
            assertThat(apa).contains("August");
            assertThat(apa).contains("22");
            assertThat(apa).contains("[Video]");
        }
    }
}
