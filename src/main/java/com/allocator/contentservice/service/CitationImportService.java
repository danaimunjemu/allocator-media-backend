package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.CitationDetectionResult;
import com.allocator.contentservice.dto.CitationImportRequest;
import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.CitationImportHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CitationImportService {

    private final ResourceDetectionService detectionService;
    private final MetadataExtractionPipeline pipeline;
    private final CitationFormatterService formatterService;
    private final CitationImportHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CitationDetectionResult detect(CitationImportRequest request, UUID requestingUserId) {
        String rawInput = request.getInput().trim();
        log.info("Citation detection requested for input: {}", rawInput.substring(0, Math.min(rawInput.length(), 80)));

        DetectedIdentifier identifier = detectionService.detect(rawInput);
        log.info("Detected identifier type: {} value: {}", identifier.getType(), identifier.getValue());

        ExtractionResult extraction = pipeline.run(identifier);

        // Generate citations in all styles
        Map<String, String> generatedCitations = new LinkedHashMap<>();
        ReferenceRequest suggestedRequest = null;

        if (extraction.isSuccessful() && extraction.getSource() != null) {
            suggestedRequest = toReferenceRequest(extraction.getSource(), requestingUserId);
            for (CitationStyle style : CitationStyle.values()) {
                suggestedRequest.setCitationStyle(style);
                try {
                    generatedCitations.put(style.name(), formatterService.format(suggestedRequest));
                } catch (Exception e) {
                    generatedCitations.put(style.name(), "");
                }
            }
            suggestedRequest.setCitationStyle(CitationStyle.APA_7);
        }

        // Persist history
        CitationImportHistory history = CitationImportHistory.builder()
                .rawInput(rawInput)
                .detectedIdentifierType(identifier.getType().name())
                .detectedIdentifierValue(identifier.getValue())
                .extractorUsed(extraction.getExtractorName())
                .confidence(extraction.getConfidence())
                .resourceType(extraction.getSource() != null && extraction.getSource().getResourceType() != null
                        ? extraction.getSource().getResourceType().name() : null)
                .requiresManualCompletion(extraction.isRequiresManualCompletion())
                .failureReason(extraction.getFailureReason())
                .createdByUserId(requestingUserId)
                .build();
        history = historyRepository.save(history);

        return CitationDetectionResult.builder()
                .importHistoryId(history.getId())
                .extractorUsed(extraction.getExtractorName())
                .confidence(extraction.getConfidence())
                .requiresManualCompletion(extraction.isRequiresManualCompletion())
                .failureReason(extraction.getFailureReason())
                .resourceType(history.getResourceType())
                .detectedIdentifierType(identifier.getType().name())
                .detectedIdentifierValue(identifier.getValue())
                .generatedCitations(generatedCitations)
                .suggestedRequest(suggestedRequest)
                .build();
    }

    @Transactional
    public void linkReferenceToHistory(UUID historyId, UUID referenceId) {
        historyRepository.findById(historyId).ifPresent(h -> {
            h.setCreatedReferenceId(referenceId);
            historyRepository.save(h);
        });
    }

    private ReferenceRequest toReferenceRequest(CitationSource src, UUID userId) {
        ReferenceRequest req = new ReferenceRequest();
        req.setSourceType(mapResourceType(src.getResourceType()));
        req.setCitationStyle(CitationStyle.APA_7);
        req.setTitle(src.getTitle());
        req.setSubtitle(src.getSubtitle());
        req.setJournalName(src.getContainerTitle());
        req.setVolumeStart(src.getVolume());
        req.setIssue(src.getIssue());
        req.setPageStart(src.getPageStart());
        req.setPageEnd(src.getPageEnd());
        req.setArticleNumber(src.getArticleNumber());
        req.setPubYear(src.getPubYear());
        req.setPubMonth(src.getPubMonth());
        req.setPubDay(src.getPubDay());
        req.setDoi(src.getDoi());
        req.setIsbn(src.getIsbn());
        req.setUrl(src.getUrl());
        req.setPdfUrl(src.getPdfUrl());
        req.setPublisher(src.getPublisher());
        req.setEdition(src.getEdition());
        req.setWebsiteName(src.getWebsiteName() != null ? src.getWebsiteName() : src.getPlatform());
        req.setAccessDate(src.getAccessDate());
        String annotationText = src.getAbstractText() != null ? src.getAbstractText()
                : src.getDescription();
        req.setAnnotation(annotationText != null
                ? annotationText.substring(0, Math.min(annotationText.length(), 500)) : null);
        req.setCreatedByUserId(userId);
        req.setContributorsJson(serializeAuthors(src.getAuthors()));
        return req;
    }

    private String serializeAuthors(List<CitationAuthor> authors) {
        if (authors == null || authors.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(authors.stream()
                    .map(a -> Map.of(
                            "role", a.getRole() != null ? a.getRole() : "AUTHOR",
                            "firstName", a.getFirstName() != null ? a.getFirstName() : "",
                            "lastName", a.getLastName() != null ? a.getLastName() : ""
                    )).toList());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize authors: {}", e.getMessage());
            return null;
        }
    }

    private ReferenceSourceType mapResourceType(ResourceType rt) {
        if (rt == null) return ReferenceSourceType.WEBPAGE;
        return switch (rt) {
            case JOURNAL_ARTICLE, RESEARCH_PAPER -> ReferenceSourceType.JOURNAL_ARTICLE;
            case BOOK -> ReferenceSourceType.BOOK;
            case BOOK_CHAPTER -> ReferenceSourceType.BOOK_CHAPTER;
            case NEWS_ARTICLE -> ReferenceSourceType.NEWSPAPER_ARTICLE;
            case MAGAZINE_ARTICLE -> ReferenceSourceType.MAGAZINE;
            case BLOG_POST -> ReferenceSourceType.BLOG_POST;
            case GOVERNMENT_PUBLICATION -> ReferenceSourceType.GOVERNMENT_DOCUMENT;
            case REPORT, FINANCIAL_RESEARCH -> ReferenceSourceType.REPORT;
            case DATASET -> ReferenceSourceType.DATASET;
            case THESIS, DISSERTATION -> ReferenceSourceType.DISSERTATION;
            case CONFERENCE_PAPER -> ReferenceSourceType.CONFERENCE_PAPER;
            case YOUTUBE_VIDEO, VIMEO_VIDEO -> ReferenceSourceType.ONLINE_VIDEO;
            case PODCAST_EPISODE, PODCAST_SERIES -> ReferenceSourceType.PODCAST;
            case INTERVIEW -> ReferenceSourceType.INTERVIEW;
            case PRESS_RELEASE -> ReferenceSourceType.PRESS_RELEASE;
            default -> ReferenceSourceType.WEBPAGE;
        };
    }
}
