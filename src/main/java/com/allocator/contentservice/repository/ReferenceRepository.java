package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.Reference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReferenceRepository extends JpaRepository<Reference, UUID> {

    List<Reference> findByCreatedByUserId(UUID userId);

    @Query("SELECT r FROM Reference r JOIN r.contents c WHERE c.id = :contentId")
    List<Reference> findByContentId(@Param("contentId") UUID contentId);

    // ─── Duplicate detection ──────────────────────────────────────────────────

    Optional<Reference> findFirstByDoi(String doi);

    Optional<Reference> findFirstByIsbn(String isbn);

    Optional<Reference> findFirstByUrl(String url);

    @Query("""
            SELECT r FROM Reference r
            WHERE LOWER(r.title) = LOWER(:title)
            ORDER BY r.createdAt DESC
            """)
    List<Reference> findByNormalizedTitle(@Param("title") String title);

    // ─── Full-text search ─────────────────────────────────────────────────────

    @Query("""
            SELECT r FROM Reference r
            WHERE (:q IS NULL OR :q = ''
                OR LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.journalName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.doi) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.isbn) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.url) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(r.formattedCitation) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            ORDER BY r.createdAt DESC
            """)
    Page<Reference> search(@Param("q") String query, Pageable pageable);

    // ─── Inline citation tracking ─────────────────────────────────────────────

    Optional<Reference> findByInlineKey(String inlineKey);

    @Query("""
            SELECT r FROM Reference r JOIN r.contents c
            WHERE c.id = :contentId AND r.inlineKey IS NOT NULL
            ORDER BY r.createdAt ASC
            """)
    List<Reference> findCitedByContentId(@Param("contentId") UUID contentId);
}
