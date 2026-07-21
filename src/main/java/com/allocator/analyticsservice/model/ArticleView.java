package com.allocator.analyticsservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "article_views", indexes = {
        @Index(name = "idx_article_view_content_id", columnList = "contentId"),
        @Index(name = "idx_article_view_viewed_at", columnList = "viewedAt"),
        @Index(name = "idx_article_view_brand_id", columnList = "brandId"),
        @Index(name = "idx_article_view_slug", columnList = "slug")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleView {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ---- Core fields ----

    private String contentId;
    private String userId;
    private String sessionId;
    private String ipAddress;
    private String brandId;
    private LocalDateTime viewedAt;
    private Long readingTimeSeconds;

    // ---- Page-view enrichment ----

    /**
     * URL pathname of the viewed page.  Indexed to support per-slug view counts
     * without requiring a content-service lookup.
     */
    @Column(length = 500)
    private String slug;

    /**
     * {@code document.referrer} captured at page-load time.
     * Stored as TEXT to accommodate long referrer URLs.
     */
    @Column(columnDefinition = "TEXT")
    private String referrer;

    /**
     * {@code navigator.userAgent} — truncated to 512 characters before
     * persistence to prevent abnormally long strings from bloating the table.
     */
    @Column(length = 512)
    private String userAgent;
}
