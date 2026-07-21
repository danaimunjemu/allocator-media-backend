package com.allocator.contentservice.mapper;

import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentMedia;
import com.allocator.contentservice.model.Author;
import com.allocator.contentservice.model.Reference;
import com.allocator.contentservice.dto.AuthorResponse;
import com.allocator.contentservice.dto.ContentRequest;
import com.allocator.contentservice.dto.ContentResponse;
import com.allocator.contentservice.dto.ReferenceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    ContentMapper INSTANCE = Mappers.getMapper(ContentMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "editorId", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "publishedBy", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "highlighted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "revisions", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(source = "heroImage", target = "heroImageUrl")
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "anonymous", source = "anonymous", defaultExpression = "java(false)")
    @Mapping(source = "accessTier", target = "accessTier")
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "parentContentId", ignore = true)
    @Mapping(target = "latestVersion", ignore = true)
    @Mapping(target = "complianceApproved", ignore = true)
    @Mapping(target = "complianceApprovedBy", ignore = true)
    @Mapping(target = "complianceApprovedAt", ignore = true)
    @Mapping(target = "complianceRequired", ignore = true)
    @Mapping(target = "scheduledBy", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    Content toEntity(ContentRequest request);

    @Mapping(source = "heroImageUrl", target = "heroImage")
    @Mapping(target = "topic", ignore = true)
    @Mapping(target = "abstractText", ignore = true)
    @Mapping(target = "pages", ignore = true)
    @Mapping(target = "downloadUrl", ignore = true)
    @Mapping(target = "audioUrl", ignore = true)
    @Mapping(target = "spotifyUrl", ignore = true)
    @Mapping(target = "applePodcastsUrl", ignore = true)
    @Mapping(target = "duration", ignore = true)
    @Mapping(target = "youtubeId", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(source = "anonymous", target = "anonymous")
    @Mapping(target = "interviewSubject", ignore = true)
    @Mapping(target = "interviewMediaUrl", ignore = true)
    @Mapping(target = "citation", ignore = true)
    @Mapping(target = "samplePages", ignore = true)
    ContentResponse toResponse(Content content);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "editorId", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "publishedBy", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "featured", ignore = true)
    @Mapping(target = "highlighted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "revisions", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(source = "heroImage", target = "heroImageUrl")
    @Mapping(target = "anonymous", source = "anonymous", defaultExpression = "java(false)")
    @Mapping(source = "accessTier", target = "accessTier")
    @Mapping(target = "versionNumber", ignore = true)
    @Mapping(target = "parentContentId", ignore = true)
    @Mapping(target = "latestVersion", ignore = true)
    @Mapping(target = "complianceApproved", ignore = true)
    @Mapping(target = "complianceApprovedBy", ignore = true)
    @Mapping(target = "complianceApprovedAt", ignore = true)
    @Mapping(target = "complianceRequired", ignore = true)
    @Mapping(target = "scheduledBy", ignore = true)
    @Mapping(target = "archivedAt", ignore = true)
    void updateEntity(ContentRequest request, @MappingTarget Content content);

    AuthorResponse toAuthorResponse(Author author);

    default List<ContentResponse.MediaResponse> mapMedia(List<ContentMedia> media) {
        if (media == null) return null;
        return media.stream()
                .map(m -> ContentResponse.MediaResponse.builder()
                        .id(m.getMediaId() != null ? m.getMediaId().toString() : null)
                        .type(m.getType() != null ? m.getType().toString() : null)
                        .url(m.getUrl())
                        .build())
                .collect(Collectors.toList());
    }

    default ContentResponse toResponseEnriched(Content content) {
        ContentResponse response = toResponse(content);
        enrich(content, response);
        return response;
    }

    default void enrich(Content content, ContentResponse response) {
        response.setAuthors(content.getAuthors() != null
                ? content.getAuthors().stream().map(this::toAuthorResponse).collect(Collectors.toList())
                : java.util.Collections.emptyList());

        if (content.getReferences() != null && !content.getReferences().isEmpty()) {
            response.setReferences(content.getReferences().stream()
                    .map(this::toReferenceResponse)
                    .collect(Collectors.toList()));
        }

        if (content.getMetadata() == null) return;
        Map<String, Object> metadata = content.getMetadata();

        // ARTICLE
        response.setTopic((String) metadata.get("topic"));
        Object rawContent = metadata.get("content");
        if (rawContent instanceof String) {
            response.setContent((String) rawContent);
        } else if (rawContent instanceof Map) {
            try {
                response.setContent(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rawContent));
            } catch (Exception ignored) {}
        }

        // RESEARCH
        response.setAbstractText((String) metadata.get("abstract"));
        if (metadata.containsKey("pages")) {
            Object pages = metadata.get("pages");
            if (pages instanceof Number) {
                response.setPages(((Number) pages).intValue());
            }
        }
        response.setDownloadUrl((String) metadata.get("downloadUrl"));
        response.setCitation((String) metadata.get("citation"));
        Object samplePages = metadata.get("samplePages");
        if (samplePages instanceof List) {
            response.setSamplePages(((List<?>) samplePages).stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }

        // PODCAST
        response.setAudioUrl((String) metadata.get("audioUrl"));
        response.setSpotifyUrl((String) metadata.get("spotifyUrl"));
        response.setApplePodcastsUrl((String) metadata.get("applePodcastsUrl"));
        response.setDuration((String) metadata.get("duration"));

        // VIDEO
        response.setYoutubeId((String) metadata.get("youtubeId"));

        // INTERVIEW — interviewSubject and mediaUrl surfaced at top level for frontend convenience
        if (metadata.containsKey("interviewSubject")) {
            response.setInterviewSubject((String) metadata.get("interviewSubject"));
        }
        if (metadata.containsKey("mediaUrl")) {
            response.setInterviewMediaUrl((String) metadata.get("mediaUrl"));
        }
    }

    default ReferenceResponse toReferenceResponse(Reference ref) {
        if (ref == null) return null;
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
                .formattedCitation(ref.getFormattedCitation())
                .createdByUserId(ref.getCreatedByUserId())
                .createdAt(ref.getCreatedAt())
                .updatedAt(ref.getUpdatedAt())
                .contentIds(ref.getContents() != null
                        ? ref.getContents().stream().map(c -> c.getId()).collect(Collectors.toList())
                        : java.util.List.of())
                .build();
    }
}
