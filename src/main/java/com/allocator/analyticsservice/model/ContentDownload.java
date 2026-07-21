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

/**
 * Persistent record of a single authorised PDF download event.
 *
 * <p>A row is inserted every time a subscriber with sufficient access initiates
 * a download on the public site.  Unauthorised (gated/blocked) attempts are
 * redirected to /pricing before reaching this layer and are never recorded.
 */
@Entity
@Table(name = "content_downloads", indexes = {
        @Index(name = "idx_download_content_id",   columnList = "contentId"),
        @Index(name = "idx_download_downloaded_at", columnList = "downloadedAt"),
        @Index(name = "idx_download_brand_id",      columnList = "brandId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** UUID of the content item that was downloaded. */
    @Column(nullable = false)
    private String contentId;

    /** Brand identifier — which public site triggered the download. */
    private String brandId;

    /** Authenticated user ID when the downloader is logged in. */
    private String userId;

    /** Originating IP address, captured server-side. */
    private String ipAddress;

    /** Timestamp of the download event. */
    @Column(nullable = false)
    private LocalDateTime downloadedAt;
}
