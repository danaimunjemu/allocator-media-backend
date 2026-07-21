package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.ArticleView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ArticleViewRepository extends JpaRepository<ArticleView, UUID> {

    @Query("SELECT DISTINCT v.contentId FROM ArticleView v")
    List<String> findAllContentIds();

    long countByContentId(String contentId);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM ArticleView v WHERE v.contentId = :contentId")
    long countUniqueViewsByContentId(String contentId);

    @Query("SELECT AVG(v.readingTimeSeconds) FROM ArticleView v WHERE v.contentId = :contentId")
    Double averageReadingTimeByContentId(String contentId);

    @Query("SELECT COUNT(v) FROM ArticleView v WHERE v.contentId = :contentId AND v.viewedAt > :since")
    long countViewsSince(String contentId, LocalDateTime since);

    /** Total views in period, optionally filtered by brandId (null = all brands). */
    @Query("SELECT COUNT(v) FROM ArticleView v " +
           "WHERE v.viewedAt >= :since AND (:brandId IS NULL OR v.brandId = :brandId)")
    long countByPeriodAndBrandId(LocalDateTime since, String brandId);

    /**
     * Top content IDs by view count within a period, optionally scoped to a brand.
     * Returns Object[]{contentId (String), viewCount (Long)}.
     * Single aggregated query — no N+1.
     */
    @Query("SELECT v.contentId, COUNT(v) AS cnt FROM ArticleView v " +
           "WHERE v.viewedAt >= :since AND (:brandId IS NULL OR v.brandId = :brandId) " +
           "GROUP BY v.contentId ORDER BY cnt DESC")
    List<Object[]> findTopContentsByPeriod(LocalDateTime since, String brandId, Pageable pageable);

    // ─── Per-subscriber engagement queries ─────────────────────────────────────

    List<ArticleView> findByUserIdAndViewedAtBetween(String userId, LocalDateTime from, LocalDateTime to);

    long countByUserId(String userId);

    long countByUserIdAndViewedAtAfter(String userId, LocalDateTime since);

    long countByUserIdAndReadingTimeSecondsGreaterThanEqual(String userId, Long minSeconds);

    @Query("SELECT COUNT(DISTINCT v.contentId) FROM ArticleView v WHERE v.userId = :userId")
    long countDistinctContentByUserId(String userId);

    @Query("SELECT COALESCE(SUM(v.readingTimeSeconds), 0) FROM ArticleView v WHERE v.userId = :userId")
    long sumReadingTimeByUserId(String userId);

    /** Top content IDs by view count for a single subscriber. Object[]{contentId, viewCount}. */
    @Query("SELECT v.contentId, COUNT(v) AS cnt FROM ArticleView v WHERE v.userId = :userId " +
           "GROUP BY v.contentId ORDER BY cnt DESC")
    List<Object[]> findTopContentsByUserId(String userId, Pageable pageable);

    List<ArticleView> findTop5ByUserIdOrderByViewedAtDesc(String userId);

    @Query("SELECT COUNT(DISTINCT CAST(v.viewedAt AS date)) FROM ArticleView v WHERE v.userId = :userId")
    long countDistinctActiveDaysByUserId(String userId);
}
