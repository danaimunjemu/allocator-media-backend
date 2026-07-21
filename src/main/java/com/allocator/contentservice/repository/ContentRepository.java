package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, JpaSpecificationExecutor<Content> {

    Optional<Content> findBySlug(String slug);

    Page<Content> findByBrandId(UUID brandId, Pageable pageable);

    Page<Content> findByStatus(ContentStatus status, Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.brandId = :brandId AND c.status = :status AND c.featured = true ORDER BY c.publishedAt DESC")
    List<Content> findFeaturedContent(@Param("brandId") UUID brandId, @Param("status") ContentStatus status);

    @Query("SELECT c FROM Content c WHERE c.brandId = :brandId AND c.status = :status AND c.highlighted = true ORDER BY c.publishedAt DESC")
    List<Content> findHighlightedContent(@Param("brandId") UUID brandId, @Param("status") ContentStatus status);

    // Finds APPROVED or COMPLIANCE_APPROVED content with a past scheduledAt.
    // COMPLIANCE_APPROVED is included so compliance-gated content can be auto-published on schedule.
    @Query("SELECT c FROM Content c WHERE c.scheduledAt <= :now AND c.scheduledAt IS NOT NULL AND c.status IN ('APPROVED', 'COMPLIANCE_APPROVED')")
    List<Content> findScheduledContentToPublish(@Param("now") Instant now);

    // Used by search-service's lightweight keyword-only suggestion lookup —
    // ContentService.listContent composes keyword search into its own
    // Specification instead, so this stays a separate, simpler query.
    @Query("SELECT c FROM Content c WHERE c.title ILIKE %:keyword% OR c.summary ILIKE %:keyword%")
    Page<Content> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ─── Reporting queries ────────────────────────────────────────────────────

    long countByBrandIdAndStatus(UUID brandId, ContentStatus status);

    long countByCategoryId(UUID categoryId);

    @Query("SELECT c FROM Content c WHERE c.brandId = :brandId AND c.status IN ('APPROVED', 'COMPLIANCE_APPROVED') AND c.scheduledAt IS NOT NULL AND c.scheduledAt > :now ORDER BY c.scheduledAt ASC")
    List<Content> findApprovedAndScheduledContent(@Param("brandId") UUID brandId, @Param("now") Instant now);

    @Query("SELECT c FROM Content c WHERE c.brandId = :brandId AND c.status = 'APPROVED' AND (c.scheduledAt IS NULL OR c.scheduledAt <= :now) ORDER BY c.approvedAt ASC")
    List<Content> findApprovedAndReadyToPublish(@Param("brandId") UUID brandId, @Param("now") Instant now);

    @Query("SELECT c FROM Content c WHERE c.brandId = :brandId AND c.publishedAt BETWEEN :from AND :to ORDER BY c.publishedAt DESC")
    List<Content> findPublishedBetween(@Param("brandId") UUID brandId, @Param("from") Instant from, @Param("to") Instant to);

    // ─── Versioning queries ───────────────────────────────────────────────────

    // Returns all versions in a group: root record + all revisions that point to the same rootId.
    @Query("SELECT c FROM Content c WHERE c.id = :rootId OR c.parentContentId = :rootId ORDER BY c.versionNumber ASC")
    List<Content> findVersionGroup(@Param("rootId") UUID rootId);

    @Query("SELECT MAX(c.versionNumber) FROM Content c WHERE c.id = :rootId OR c.parentContentId = :rootId")
    java.util.Optional<Integer> findMaxVersionInGroup(@Param("rootId") UUID rootId);

    // Returns the currently-published version within a version group (for public APIs).
    @Query("SELECT c FROM Content c WHERE (c.id = :rootId OR c.parentContentId = :rootId) AND c.status = 'PUBLISHED' ORDER BY c.versionNumber DESC")
    java.util.Optional<Content> findLatestPublishedInGroup(@Param("rootId") UUID rootId);
}
