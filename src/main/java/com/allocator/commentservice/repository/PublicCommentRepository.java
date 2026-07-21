package com.allocator.commentservice.repository;

import com.allocator.commentservice.model.PublicComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

// Deliberately no status filtering in the listing queries below — a
// self-deleted or moderator-removed comment must stay visible in its thread
// slot (rendered as a placeholder by the frontend) rather than vanish, so
// replies don't end up orphaned with no visible parent context.
public interface PublicCommentRepository extends JpaRepository<PublicComment, UUID> {

    Page<PublicComment> findByContentIdAndRootCommentIdIsNullOrderByCreatedAtDesc(
            UUID contentId, Pageable pageable);

    Page<PublicComment> findByContentIdAndRootCommentIdIsNullOrderByCreatedAtAsc(
            UUID contentId, Pageable pageable);

    @Query("""
        SELECT c FROM PublicComment c
        WHERE c.contentId = :contentId AND c.rootCommentId IS NULL
        ORDER BY (SELECT COALESCE(SUM(v.value), 0) FROM PublicCommentVote v WHERE v.commentId = c.id) DESC,
                 c.createdAt DESC
        """)
    Page<PublicComment> findTopLevelOrderByScore(@Param("contentId") UUID contentId, Pageable pageable);

    Page<PublicComment> findByRootCommentIdOrderByCreatedAtAsc(UUID rootCommentId, Pageable pageable);

    long countByContentId(UUID contentId);

    long countByRootCommentId(UUID rootCommentId);
}
