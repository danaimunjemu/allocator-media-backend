package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.CommentStatus;
import com.allocator.contentservice.model.EditorialComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface EditorialCommentRepository extends JpaRepository<EditorialComment, UUID> {

    List<EditorialComment> findByContentIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID contentId);

    List<EditorialComment> findByContentIdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID contentId, CommentStatus status);

    List<EditorialComment> findByParentCommentIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID parentCommentId);

    long countByContentIdAndStatusAndDeletedAtIsNull(UUID contentId, CommentStatus status);

    @Query("SELECT DISTINCT ec.authorId FROM EditorialComment ec WHERE ec.contentId = :contentId AND ec.deletedAt IS NULL")
    List<UUID> findDistinctAuthorIdsByContentId(@Param("contentId") UUID contentId);

    @Modifying
    @Transactional
    @Query("UPDATE EditorialComment ec SET ec.anchorStatus = com.allocator.contentservice.model.AnchorStatus.STALE " +
           "WHERE ec.contentId = :contentId AND ec.contentVersionNumber < :versionNumber AND ec.anchorStart IS NOT NULL AND ec.deletedAt IS NULL")
    void markAnchorsStale(@Param("contentId") UUID contentId, @Param("versionNumber") int versionNumber);
}
