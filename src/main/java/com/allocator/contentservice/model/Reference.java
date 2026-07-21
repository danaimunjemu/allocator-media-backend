package com.allocator.contentservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "content_references_lib")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "contents")
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private ReferenceSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "citation_style", nullable = false)
    private CitationStyle citationStyle = CitationStyle.APA_7;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "contributors_json", columnDefinition = "TEXT")
    private String contributorsJson;

    @Column(name = "journal_name")
    private String journalName;

    @Column(name = "volume_start")
    private String volumeStart;

    @Column(name = "volume_end")
    private String volumeEnd;

    @Column(name = "issue")
    private String issue;

    @Column(name = "article_number")
    private String articleNumber;

    @Column(name = "publication_status")
    private String publicationStatus;

    @Column(name = "pub_year")
    private Integer pubYear;

    @Column(name = "pub_month")
    private Integer pubMonth;

    @Column(name = "pub_day")
    private Integer pubDay;

    @Column(name = "page_start")
    private String pageStart;

    @Column(name = "page_end")
    private String pageEnd;

    @Column(name = "library_database")
    private String libraryDatabase;

    @Column(name = "doi")
    private String doi;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "url")
    private String url;

    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    @Column(name = "publisher")
    private String publisher;

    @Column(name = "edition")
    private String edition;

    @Column(name = "isbn")
    private String isbn;

    @Column(name = "website_name")
    private String websiteName;

    @Column(name = "access_date")
    private String accessDate;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "conference_title")
    private String conferenceTitle;

    @Column(name = "conference_location")
    private String conferenceLocation;

    @Column(name = "report_number")
    private String reportNumber;

    @Column(name = "institution")
    private String institution;

    @Column(name = "degree")
    private String degree;

    @Column(name = "channel_name")
    private String channelName;

    @Column(name = "duration")
    private String duration;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "publisher_location")
    private String publisherLocation;

    /** Unique key assigned when this reference is inserted as an inline citation in a document. */
    @Column(name = "inline_key")
    private String inlineKey;

    @Column(name = "formatted_citation", columnDefinition = "TEXT")
    private String formattedCitation;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @JsonIgnore
    @ManyToMany(mappedBy = "references", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Content> contents = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
