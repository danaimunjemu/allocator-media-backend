package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.ContentDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ContentDownloadRepository extends JpaRepository<ContentDownload, UUID> {

    /** Total downloads for a specific content item. */
    long countByContentId(String contentId);

    /**
     * Total downloads within a time window, optionally scoped to a brand.
     * Mirrors the {@code ArticleViewRepository#countByPeriodAndBrandId} pattern
     * for consistent aggregation in the summary service.
     */
    @Query("SELECT COUNT(d) FROM ContentDownload d " +
           "WHERE d.downloadedAt >= :since AND (:brandId IS NULL OR d.brandId = :brandId)")
    long countByPeriodAndBrandId(LocalDateTime since, String brandId);

    // ─── Per-subscriber engagement queries ─────────────────────────────────────

    long countByUserId(String userId);

    long countByUserIdAndDownloadedAtAfter(String userId, LocalDateTime since);

    java.util.List<ContentDownload> findByUserIdAndDownloadedAtBetween(String userId, LocalDateTime from, LocalDateTime to);

    java.util.List<ContentDownload> findTop5ByUserIdOrderByDownloadedAtDesc(String userId);
}
