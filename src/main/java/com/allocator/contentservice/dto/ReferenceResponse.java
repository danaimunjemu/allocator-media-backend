package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.CitationStyle;
import com.allocator.contentservice.model.ReferenceSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceResponse {

    private UUID id;
    private ReferenceSourceType sourceType;
    private CitationStyle citationStyle;
    private String title;
    private String subtitle;
    private String contributorsJson;
    private String journalName;
    private String volumeStart;
    private String volumeEnd;
    private String issue;
    private String articleNumber;
    private String publicationStatus;
    private Integer pubYear;
    private Integer pubMonth;
    private Integer pubDay;
    private String pageStart;
    private String pageEnd;
    private String libraryDatabase;
    private String doi;
    private String pdfUrl;
    private String url;
    private String annotation;
    private String publisher;
    private String edition;
    private String isbn;
    private String websiteName;
    private String accessDate;

    // Extended metadata fields
    private String abstractText;
    private String conferenceTitle;
    private String conferenceLocation;
    private String reportNumber;
    private String institution;
    private String degree;
    private String channelName;
    private String duration;
    private String thumbnailUrl;
    private String publisherLocation;
    private String inlineKey;

    private String formattedCitation;
    private Integer metadataCompleteness;
    private UUID createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<UUID> contentIds;
}
