package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.ReferenceRequest;
import com.allocator.contentservice.dto.ReferenceResponse;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.Reference;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.contentservice.repository.ReferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceService {

    private final ReferenceRepository referenceRepository;
    private final ContentRepository contentRepository;
    private final CitationFormatterService citationFormatterService;
    private final MetadataCompletenessScorer completenessScorer;
    private final DuplicateDetectionService duplicateDetectionService;

    @Transactional
    public ReferenceResponse create(ReferenceRequest request) {
        String formatted = citationFormatterService.format(request);

        Reference ref = buildFromRequest(request, new Reference());
        ref.setFormattedCitation(formatted);

        Reference saved = referenceRepository.save(ref);

        if (request.getContentIds() != null) {
            for (UUID contentId : request.getContentIds()) {
                linkToContent(saved.getId(), contentId);
            }
            saved = referenceRepository.findById(saved.getId()).orElse(saved);
        }

        log.info("Created reference: {} '{}'", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReferenceResponse get(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getAll() {
        return referenceRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReferenceResponse> search(String query, int page, int size) {
        return referenceRepository.search(query, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getByContentId(UUID contentId) {
        return referenceRepository.findByContentId(contentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getCitedByContentId(UUID contentId) {
        return referenceRepository.findCitedByContentId(contentId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferenceResponse> getByUserId(UUID userId) {
        return referenceRepository.findByCreatedByUserId(userId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DuplicateDetectionService.DuplicateResult checkDuplicate(ReferenceRequest request) {
        return duplicateDetectionService.detect(request);
    }

    @Transactional
    public ReferenceResponse update(UUID id, ReferenceRequest request) {
        Reference ref = findById(id);
        buildFromRequest(request, ref);
        ref.setFormattedCitation(citationFormatterService.format(request));
        return toResponse(referenceRepository.save(ref));
    }

    @Transactional
    public ReferenceResponse setInlineKey(UUID id, String inlineKey) {
        Reference ref = findById(id);
        ref.setInlineKey(inlineKey);
        return toResponse(referenceRepository.save(ref));
    }

    @Transactional
    public void delete(UUID id) {
        Reference ref = findById(id);
        for (Content c : ref.getContents()) {
            c.getReferences().remove(ref);
        }
        referenceRepository.delete(ref);
        log.info("Deleted reference: {}", id);
    }

    @Transactional
    public ReferenceResponse linkToContent(UUID referenceId, UUID contentId) {
        Reference ref = findById(referenceId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        content.getReferences().add(ref);
        ref.getContents().add(content);
        contentRepository.save(content);
        return toResponse(referenceRepository.save(ref));
    }

    @Transactional
    public void unlinkFromContent(UUID referenceId, UUID contentId) {
        Reference ref = findById(referenceId);
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId));
        content.getReferences().remove(ref);
        ref.getContents().remove(content);
        contentRepository.save(content);
        referenceRepository.save(ref);
    }

    // ── Export helpers ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String exportBibTex(List<UUID> ids) {
        List<Reference> refs = ids.isEmpty()
                ? referenceRepository.findAll()
                : ids.stream().map(this::findById).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (Reference r : refs) {
            sb.append(toBibTex(r)).append("\n\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportRis(List<UUID> ids) {
        List<Reference> refs = ids.isEmpty()
                ? referenceRepository.findAll()
                : ids.stream().map(this::findById).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (Reference r : refs) {
            sb.append(toRis(r)).append("\n");
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> exportCslJson(List<UUID> ids) {
        List<Reference> refs = ids.isEmpty()
                ? referenceRepository.findAll()
                : ids.stream().map(this::findById).collect(Collectors.toList());
        return refs.stream().map(this::toCslJson).collect(Collectors.toList());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Reference findById(UUID id) {
        return referenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reference not found: " + id));
    }

    private Reference buildFromRequest(ReferenceRequest req, Reference ref) {
        ref.setSourceType(req.getSourceType());
        ref.setCitationStyle(req.getCitationStyle());
        ref.setTitle(req.getTitle());
        ref.setSubtitle(req.getSubtitle());
        ref.setContributorsJson(req.getContributorsJson());
        ref.setJournalName(req.getJournalName());
        ref.setVolumeStart(req.getVolumeStart());
        ref.setVolumeEnd(req.getVolumeEnd());
        ref.setIssue(req.getIssue());
        ref.setArticleNumber(req.getArticleNumber());
        ref.setPublicationStatus(req.getPublicationStatus());
        ref.setPubYear(req.getPubYear());
        ref.setPubMonth(req.getPubMonth());
        ref.setPubDay(req.getPubDay());
        ref.setPageStart(req.getPageStart());
        ref.setPageEnd(req.getPageEnd());
        ref.setLibraryDatabase(req.getLibraryDatabase());
        ref.setDoi(req.getDoi());
        ref.setPdfUrl(req.getPdfUrl());
        ref.setUrl(req.getUrl());
        ref.setAnnotation(req.getAnnotation());
        ref.setPublisher(req.getPublisher());
        ref.setEdition(req.getEdition());
        ref.setIsbn(req.getIsbn());
        ref.setWebsiteName(req.getWebsiteName());
        ref.setAccessDate(req.getAccessDate());
        // Extended fields
        ref.setAbstractText(req.getAbstractText());
        ref.setConferenceTitle(req.getConferenceTitle());
        ref.setConferenceLocation(req.getConferenceLocation());
        ref.setReportNumber(req.getReportNumber());
        ref.setInstitution(req.getInstitution());
        ref.setDegree(req.getDegree());
        ref.setChannelName(req.getChannelName());
        ref.setDuration(req.getDuration());
        ref.setThumbnailUrl(req.getThumbnailUrl());
        ref.setPublisherLocation(req.getPublisherLocation());
        if (req.getInlineKey() != null) ref.setInlineKey(req.getInlineKey());
        if (req.getCreatedByUserId() != null) ref.setCreatedByUserId(req.getCreatedByUserId());
        return ref;
    }

    private ReferenceResponse toResponse(Reference ref) {
        MetadataCompletenessScorer.CompletenessResult completeness = completenessScorer.score(ref);
        return ReferenceResponse.builder()
                .id(ref.getId())
                .sourceType(ref.getSourceType())
                .citationStyle(ref.getCitationStyle())
                .title(ref.getTitle())
                .subtitle(ref.getSubtitle())
                .contributorsJson(ref.getContributorsJson())
                .journalName(ref.getJournalName())
                .volumeStart(ref.getVolumeStart())
                .volumeEnd(ref.getVolumeEnd())
                .issue(ref.getIssue())
                .articleNumber(ref.getArticleNumber())
                .publicationStatus(ref.getPublicationStatus())
                .pubYear(ref.getPubYear())
                .pubMonth(ref.getPubMonth())
                .pubDay(ref.getPubDay())
                .pageStart(ref.getPageStart())
                .pageEnd(ref.getPageEnd())
                .libraryDatabase(ref.getLibraryDatabase())
                .doi(ref.getDoi())
                .pdfUrl(ref.getPdfUrl())
                .url(ref.getUrl())
                .annotation(ref.getAnnotation())
                .publisher(ref.getPublisher())
                .edition(ref.getEdition())
                .isbn(ref.getIsbn())
                .websiteName(ref.getWebsiteName())
                .accessDate(ref.getAccessDate())
                .abstractText(ref.getAbstractText())
                .conferenceTitle(ref.getConferenceTitle())
                .conferenceLocation(ref.getConferenceLocation())
                .reportNumber(ref.getReportNumber())
                .institution(ref.getInstitution())
                .degree(ref.getDegree())
                .channelName(ref.getChannelName())
                .duration(ref.getDuration())
                .thumbnailUrl(ref.getThumbnailUrl())
                .publisherLocation(ref.getPublisherLocation())
                .inlineKey(ref.getInlineKey())
                .formattedCitation(ref.getFormattedCitation())
                .metadataCompleteness(completeness.score())
                .createdByUserId(ref.getCreatedByUserId())
                .createdAt(ref.getCreatedAt())
                .updatedAt(ref.getUpdatedAt())
                .contentIds(ref.getContents() != null
                        ? ref.getContents().stream().map(c -> c.getId()).collect(Collectors.toList())
                        : List.of())
                .build();
    }

    private String toBibTex(Reference r) {
        String key = (r.getTitle() != null ? r.getTitle().replaceAll("[^a-zA-Z0-9]", "").substring(0, Math.min(20, r.getTitle().length())) : "ref")
                + (r.getPubYear() != null ? r.getPubYear() : "");
        String type = switch (r.getSourceType()) {
            case JOURNAL_ARTICLE -> "@article";
            case BOOK            -> "@book";
            case BOOK_CHAPTER    -> "@incollection";
            case CONFERENCE_PAPER -> "@inproceedings";
            case DISSERTATION    -> "@phdthesis";
            case SOFTWARE        -> "@misc";
            default              -> "@misc";
        };
        StringBuilder sb = new StringBuilder(type).append("{").append(key).append(",\n");
        appendBib(sb, "title",   r.getTitle());
        appendBib(sb, "journal", r.getJournalName());
        appendBib(sb, "year",    r.getPubYear() != null ? String.valueOf(r.getPubYear()) : null);
        appendBib(sb, "volume",  r.getVolumeStart());
        appendBib(sb, "number",  r.getIssue());
        appendBib(sb, "pages",   pages(r));
        appendBib(sb, "doi",     r.getDoi());
        appendBib(sb, "isbn",    r.getIsbn());
        appendBib(sb, "url",     r.getUrl());
        appendBib(sb, "publisher", r.getPublisher());
        appendBib(sb, "address", r.getPublisherLocation());
        appendBib(sb, "school",  r.getInstitution());
        sb.append("}");
        return sb.toString();
    }

    private void appendBib(StringBuilder sb, String key, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("  ").append(key).append(" = {").append(value).append("},\n");
        }
    }

    private String toRis(Reference r) {
        StringBuilder sb = new StringBuilder();
        String ty = switch (r.getSourceType()) {
            case JOURNAL_ARTICLE  -> "JOUR";
            case BOOK             -> "BOOK";
            case BOOK_CHAPTER     -> "CHAP";
            case CONFERENCE_PAPER -> "CONF";
            case DISSERTATION     -> "THES";
            case REPORT           -> "RPRT";
            case WEBPAGE          -> "ELEC";
            case ONLINE_VIDEO     -> "ADVS";
            default               -> "GEN";
        };
        sb.append("TY  - ").append(ty).append("\n");
        appendRis(sb, "TI", r.getTitle());
        appendRis(sb, "JO", r.getJournalName());
        if (r.getPubYear() != null) appendRis(sb, "PY", String.valueOf(r.getPubYear()));
        appendRis(sb, "VL", r.getVolumeStart());
        appendRis(sb, "IS", r.getIssue());
        appendRis(sb, "SP", r.getPageStart());
        appendRis(sb, "EP", r.getPageEnd());
        appendRis(sb, "DO", r.getDoi());
        appendRis(sb, "SN", r.getIsbn());
        appendRis(sb, "UR", r.getUrl());
        appendRis(sb, "PB", r.getPublisher());
        appendRis(sb, "AB", r.getAbstractText());
        sb.append("ER  - \n");
        return sb.toString();
    }

    private void appendRis(StringBuilder sb, String tag, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(tag).append("  - ").append(value).append("\n");
        }
    }

    private java.util.Map<String, Object> toCslJson(Reference r) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", r.getId().toString());
        m.put("type", cslType(r));
        if (r.getTitle() != null) m.put("title", r.getTitle());
        if (r.getJournalName() != null) m.put("container-title", r.getJournalName());
        if (r.getPubYear() != null) {
            m.put("issued", java.util.Map.of("date-parts", new int[][]{{r.getPubYear()}}));
        }
        if (r.getDoi() != null) m.put("DOI", r.getDoi());
        if (r.getIsbn() != null) m.put("ISBN", r.getIsbn());
        if (r.getUrl() != null) m.put("URL", r.getUrl());
        if (r.getPublisher() != null) m.put("publisher", r.getPublisher());
        if (r.getVolumeStart() != null) m.put("volume", r.getVolumeStart());
        if (r.getIssue() != null) m.put("issue", r.getIssue());
        if (r.getPageStart() != null) m.put("page", pages(r));
        return m;
    }

    private String cslType(Reference r) {
        if (r.getSourceType() == null) return "article";
        return switch (r.getSourceType()) {
            case JOURNAL_ARTICLE  -> "article-journal";
            case BOOK             -> "book";
            case BOOK_CHAPTER     -> "chapter";
            case CONFERENCE_PAPER -> "paper-conference";
            case DISSERTATION     -> "thesis";
            case REPORT           -> "report";
            case WEBPAGE          -> "webpage";
            case ONLINE_VIDEO     -> "motion_picture";
            case NEWSPAPER_ARTICLE -> "article-newspaper";
            case BLOG_POST        -> "post-weblog";
            case PODCAST          -> "broadcast";
            default               -> "article";
        };
    }

    private String pages(Reference r) {
        if (r.getPageStart() == null) return null;
        return r.getPageEnd() != null ? r.getPageStart() + "–" + r.getPageEnd() : r.getPageStart();
    }
}
