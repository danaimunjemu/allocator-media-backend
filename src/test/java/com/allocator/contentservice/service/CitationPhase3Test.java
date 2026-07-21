package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.CitationDetectionResult;
import com.allocator.contentservice.dto.CitationImportRequest;
import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.extractor.*;
import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.CitationImportHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 3 — Citation Acquisition & Metadata Extraction")
class CitationPhase3Test {

    // ── Mocks ──────────────────────────────────────────────────────────────────

    @Mock MetadataExtractor mockExtractor;
    @Mock CitationImportHistoryRepository historyRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    ResourceDetectionService detectionService;

    @BeforeEach
    void setUp() {
        detectionService = new ResourceDetectionService();

        CitationImportHistory savedHistory = CitationImportHistory.builder()
                .id(UUID.randomUUID())
                .rawInput("test")
                .detectedIdentifierType("DOI")
                .build();
        when(historyRepository.save(any())).thenReturn(savedHistory);
    }

    // ── Resource Detection Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("ResourceDetectionService — identifier detection")
    class DetectionTests {

        @Test
        @DisplayName("detects bare DOI")
        void detectsBareDoi() {
            DetectedIdentifier id = detectionService.detect("10.1016/j.cell.2023.01.001");
            assertThat(id.getType()).isEqualTo(IdentifierType.DOI);
            assertThat(id.getValue()).contains("10.1016");
        }

        @Test
        @DisplayName("detects DOI with https://doi.org/ prefix")
        void detectsDoiWithPrefix() {
            DetectedIdentifier id = detectionService.detect("https://doi.org/10.1038/nature12345");
            assertThat(id.getType()).isEqualTo(IdentifierType.DOI);
            assertThat(id.getValue()).isEqualTo("10.1038/nature12345");
        }

        @Test
        @DisplayName("detects ISBN-13")
        void detectsIsbn13() {
            DetectedIdentifier id = detectionService.detect("978-0-7432-7356-5");
            assertThat(id.getType()).isEqualTo(IdentifierType.ISBN);
            assertThat(id.getValue()).isEqualTo("9780743273565");
        }

        @Test
        @DisplayName("detects arXiv bare ID")
        void detectsArxivBareId() {
            DetectedIdentifier id = detectionService.detect("2306.12345");
            assertThat(id.getType()).isEqualTo(IdentifierType.ARXIV);
            assertThat(id.getValue()).startsWith("2306");
        }

        @Test
        @DisplayName("detects arXiv URL")
        void detectsArxivUrl() {
            DetectedIdentifier id = detectionService.detect("https://arxiv.org/abs/2306.12345v2");
            assertThat(id.getType()).isEqualTo(IdentifierType.ARXIV);
        }

        @Test
        @DisplayName("detects YouTube URL")
        void detectsYouTubeUrl() {
            DetectedIdentifier id = detectionService.detect("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            assertThat(id.getType()).isEqualTo(IdentifierType.YOUTUBE_URL);
            assertThat(id.getValue()).isEqualTo("dQw4w9WgXcQ");
        }

        @Test
        @DisplayName("detects youtu.be short URL")
        void detectsYoutubeShortUrl() {
            DetectedIdentifier id = detectionService.detect("https://youtu.be/dQw4w9WgXcQ");
            assertThat(id.getType()).isEqualTo(IdentifierType.YOUTUBE_URL);
        }

        @Test
        @DisplayName("detects government URL (.gov)")
        void detectsGovernmentUrl() {
            DetectedIdentifier id = detectionService.detect("https://www.cdc.gov/coronavirus/2019-ncov/index.html");
            assertThat(id.getType()).isEqualTo(IdentifierType.GOVERNMENT_URL);
        }

        @Test
        @DisplayName("detects Reuters news URL")
        void detectsNewsUrl() {
            DetectedIdentifier id = detectionService.detect("https://reuters.com/markets/stocks/2023-06-15/");
            assertThat(id.getType()).isEqualTo(IdentifierType.NEWS_URL);
        }

        @Test
        @DisplayName("detects Spotify podcast URL")
        void detectsPodcastUrl() {
            DetectedIdentifier id = detectionService.detect("https://open.spotify.com/episode/abc123");
            assertThat(id.getType()).isEqualTo(IdentifierType.PODCAST_URL);
        }

        @Test
        @DisplayName("detects generic URL as fallback")
        void detectsGenericUrl() {
            DetectedIdentifier id = detectionService.detect("https://example.com/some-article");
            assertThat(id.getType()).isEqualTo(IdentifierType.GENERIC_URL);
        }

        @Test
        @DisplayName("returns UNKNOWN for unrecognized input")
        void returnsUnknownForUnrecognized() {
            DetectedIdentifier id = detectionService.detect("hello world");
            assertThat(id.getType()).isEqualTo(IdentifierType.UNKNOWN);
        }
    }

    // ── Citation Formatter Tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("CitationFormatterService — all 6 styles")
    class FormatterTests {

        CitationFormatterService formatter;

        @BeforeEach
        void init() {
            formatter = new CitationFormatterService(objectMapper);
        }

        private ReferenceRequest journalReq() {
            return ReferenceRequest.builder()
                    .sourceType(ReferenceSourceType.JOURNAL_ARTICLE)
                    .title("The Impact of Climate Change")
                    .contributorsJson("[{\"role\":\"AUTHOR\",\"firstName\":\"Jane\",\"lastName\":\"Doe\"}]")
                    .journalName("Nature")
                    .volumeStart("12")
                    .issue("3")
                    .pageStart("100")
                    .pageEnd("120")
                    .pubYear(2023)
                    .pubMonth(6)
                    .doi("10.1038/nature12345")
                    .citationStyle(CitationStyle.APA_7)
                    .build();
        }

        @Test
        @DisplayName("APA 7 formats journal article correctly")
        void apa7JournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.APA_7);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("(2023)").contains("Nature");
        }

        @Test
        @DisplayName("MLA 9 formats journal article")
        void mla9JournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.MLA_9);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("Nature");
        }

        @Test
        @DisplayName("Harvard formats journal article")
        void harvardJournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.HARVARD);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("(2023)").contains("Nature");
        }

        @Test
        @DisplayName("Chicago 17 formats journal article")
        void chicago17JournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.CHICAGO_17);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("Nature");
        }

        @Test
        @DisplayName("IEEE formats journal article with vol/no/pp/doi")
        void ieeeJournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.IEEE);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("vol.").contains("no.").contains("pp.");
        }

        @Test
        @DisplayName("Vancouver formats journal article with semicolon notation")
        void vancouverJournalArticle() {
            ReferenceRequest req = journalReq();
            req.setCitationStyle(CitationStyle.VANCOUVER);
            String result = formatter.format(req);
            assertThat(result).contains("Doe").contains("Nature").contains("2023");
        }

        @Test
        @DisplayName("APA 7 handles book source type")
        void apa7Book() {
            ReferenceRequest req = ReferenceRequest.builder()
                    .sourceType(ReferenceSourceType.BOOK)
                    .citationStyle(CitationStyle.APA_7)
                    .title("Clean Code")
                    .contributorsJson("[{\"role\":\"AUTHOR\",\"firstName\":\"Robert\",\"lastName\":\"Martin\"}]")
                    .publisher("Prentice Hall")
                    .pubYear(2008)
                    .build();
            String result = formatter.format(req);
            assertThat(result).contains("Martin").contains("(2008)").contains("Clean Code").contains("Prentice Hall");
        }

        @Test
        @DisplayName("returns empty string for null request")
        void returnsEmptyForNull() {
            assertThat(formatter.format(null)).isEmpty();
        }
    }

    // ── Pipeline Tests ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MetadataExtractionPipeline — extractor priority and fallback")
    class PipelineTests {

        @Test
        @DisplayName("returns extractor result when above confidence threshold")
        void returnsHighConfidenceResult() {
            when(mockExtractor.supports(IdentifierType.DOI)).thenReturn(true);
            when(mockExtractor.getPriority()).thenReturn(1);
            when(mockExtractor.getName()).thenReturn("MockExtractor");

            CitationSource src = CitationSource.builder()
                    .title("Test Article")
                    .doi("10.1000/test")
                    .resourceType(ResourceType.JOURNAL_ARTICLE)
                    .build();
            ExtractionResult success = ExtractionResult.builder()
                    .source(src)
                    .confidence(95)
                    .extractorName("MockExtractor")
                    .requiresManualCompletion(false)
                    .build();
            when(mockExtractor.extract(any())).thenReturn(success);

            MetadataExtractionPipeline pipeline = new MetadataExtractionPipeline(
                    List.of(mockExtractor), null);

            DetectedIdentifier id = DetectedIdentifier.builder()
                    .type(IdentifierType.DOI)
                    .value("10.1000/test")
                    .normalizedInput("10.1000/test")
                    .build();

            ExtractionResult result = pipeline.run(id);
            assertThat(result.getConfidence()).isEqualTo(95);
            assertThat(result.getExtractorName()).isEqualTo("MockExtractor");
            assertThat(result.getSource().getTitle()).isEqualTo("Test Article");
        }

        @Test
        @DisplayName("marks result as requiring manual completion when confidence is low")
        void marksManualWhenLowConfidence() {
            when(mockExtractor.supports(IdentifierType.GENERIC_URL)).thenReturn(true);
            when(mockExtractor.getPriority()).thenReturn(99);
            when(mockExtractor.getName()).thenReturn("Heuristic");

            CitationSource src = CitationSource.builder().title("Some Page").build();
            ExtractionResult low = ExtractionResult.builder()
                    .source(src)
                    .confidence(40)
                    .extractorName("Heuristic")
                    .requiresManualCompletion(true)
                    .build();
            when(mockExtractor.extract(any())).thenReturn(low);

            MetadataExtractionPipeline pipeline = new MetadataExtractionPipeline(
                    List.of(mockExtractor), null);

            DetectedIdentifier id = DetectedIdentifier.builder()
                    .type(IdentifierType.GENERIC_URL)
                    .value("https://example.com")
                    .normalizedInput("https://example.com")
                    .build();

            ExtractionResult result = pipeline.run(id);
            assertThat(result.isRequiresManualCompletion()).isTrue();
        }

        @Test
        @DisplayName("returns failed result when no extractor supports identifier type")
        void failsWhenNoExtractorSupports() {
            when(mockExtractor.supports(any())).thenReturn(false);

            MetadataExtractionPipeline pipeline = new MetadataExtractionPipeline(
                    List.of(mockExtractor), null);

            DetectedIdentifier id = DetectedIdentifier.builder()
                    .type(IdentifierType.UNKNOWN)
                    .value("unknown")
                    .normalizedInput("unknown")
                    .build();

            ExtractionResult result = pipeline.run(id);
            assertThat(result.getConfidence()).isEqualTo(0);
            assertThat(result.isRequiresManualCompletion()).isTrue();
        }
    }

    // ── ExtractionResult Tests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("ExtractionResult — helper methods")
    class ExtractionResultTests {

        @Test
        @DisplayName("failed() builds a result with zero confidence")
        void failedBuildsWithZeroConfidence() {
            ExtractionResult result = ExtractionResult.failed("TestExtractor", "Network timeout");
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.getConfidence()).isEqualTo(0);
            assertThat(result.getFailureReason()).isEqualTo("Network timeout");
            assertThat(result.getExtractorName()).isEqualTo("TestExtractor");
        }

        @Test
        @DisplayName("isSuccessful() returns false when source is null")
        void notSuccessfulWhenSourceNull() {
            ExtractionResult result = ExtractionResult.builder().confidence(80).build();
            assertThat(result.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("isSuccessful() returns true when source and confidence > 0")
        void successfulWithSourceAndConfidence() {
            ExtractionResult result = ExtractionResult.builder()
                    .source(CitationSource.builder().title("Test").build())
                    .confidence(75)
                    .build();
            assertThat(result.isSuccessful()).isTrue();
        }
    }

    // ── ISBN Validation Tests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("ISBN detection — checksum validation")
    class IsbnTests {

        @Test
        @DisplayName("accepts valid ISBN-13")
        void validIsbn13() {
            DetectedIdentifier id = detectionService.detect("9780743273565");
            assertThat(id.getType()).isEqualTo(IdentifierType.ISBN);
        }

        @Test
        @DisplayName("accepts ISBN-13 with hyphens")
        void validIsbn13WithHyphens() {
            DetectedIdentifier id = detectionService.detect("978-0-7432-7356-5");
            assertThat(id.getType()).isEqualTo(IdentifierType.ISBN);
        }

        @Test
        @DisplayName("rejects invalid ISBN-13 checksum")
        void rejectsInvalidIsbn13() {
            // Same digits but wrong check digit (last digit changed)
            DetectedIdentifier id = detectionService.detect("9780743273566");
            assertThat(id.getType()).isNotEqualTo(IdentifierType.ISBN);
        }
    }

    // ── ResourceType Mapping Tests ─────────────────────────────────────────────

    @Nested
    @DisplayName("CitationImportService — ResourceType → ReferenceSourceType mapping")
    class MappingTests {

        @Test
        @DisplayName("all ResourceType values produce a non-null ReferenceSourceType")
        void allResourceTypesMapped() {
            CitationImportService importService = new CitationImportService(
                    detectionService,
                    new MetadataExtractionPipeline(List.of(mockExtractor), null),
                    new CitationFormatterService(objectMapper),
                    historyRepository,
                    objectMapper);

            when(mockExtractor.supports(any())).thenReturn(true);
            when(mockExtractor.getPriority()).thenReturn(1);
            when(mockExtractor.getName()).thenReturn("Mock");

            for (ResourceType rt : ResourceType.values()) {
                CitationSource src = CitationSource.builder()
                        .title("Test")
                        .resourceType(rt)
                        .build();
                ExtractionResult ex = ExtractionResult.builder()
                        .source(src).confidence(90).extractorName("Mock").build();
                when(mockExtractor.extract(any())).thenReturn(ex);

                CitationDetectionResult result = importService.detect(
                        new CitationImportRequest("10.1000/test"), null);
                assertThat(result).isNotNull();
                assertThat(result.getSuggestedRequest()).isNotNull();
                assertThat(result.getSuggestedRequest().getSourceType()).isNotNull();
            }
        }
    }
}
