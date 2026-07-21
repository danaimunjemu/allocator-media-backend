package com.allocator.contentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationSource {

    // Input context
    private String rawInput;
    private ResourceType resourceType;

    // Identifiers
    private String doi;
    private String isbn;
    private String pmid;
    private String arxivId;
    private String url;
    private String pdfUrl;

    // Core bibliographic fields
    private String title;
    private String subtitle;
    private List<CitationAuthor> authors;
    private List<CitationAuthor> editors;
    private Integer pubYear;
    private Integer pubMonth;
    private Integer pubDay;

    // Journal / periodical
    private String containerTitle;
    private String volume;
    private String issue;
    private String pageStart;
    private String pageEnd;
    private String articleNumber;

    // Book
    private String publisher;
    private String publisherLocation;
    private String edition;

    // Web / media
    private String websiteName;
    private String accessDate;
    private String platform;
    private String channelName;
    private String videoId;

    // Extended media metadata
    private String thumbnailUrl;
    private String description;
    private String duration;
    private List<String> tags;
    private String viewCount;
    private String metadataSource;

    // Podcast
    private String episodeNumber;
    private String seriesTitle;

    // Academic
    private String institution;
    private String degree;
    private String reportNumber;
    private String conferenceTitle;
    private String conferenceLocation;

    // Misc
    private String language;
    private String abstractText;
    private List<String> keywords;
    private String license;
}
