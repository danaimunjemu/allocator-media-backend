package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.AnnotationType;
import com.allocator.contentservice.model.CommentStatus;
import com.allocator.contentservice.model.EditorialAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface EditorialAnnotationRepository extends JpaRepository<EditorialAnnotation, UUID> {

    List<EditorialAnnotation> findByContentIdOrderByCreatedAtAsc(UUID contentId);

    List<EditorialAnnotation> findByContentIdAndStatusOrderByCreatedAtAsc(UUID contentId, CommentStatus status);

    List<EditorialAnnotation> findByContentIdAndAnnotationTypeOrderByCreatedAtAsc(
            UUID contentId, AnnotationType annotationType);

    List<EditorialAnnotation> findByContentIdAndAnnotationTypeAndStatusOrderByCreatedAtAsc(
            UUID contentId, AnnotationType annotationType, CommentStatus status);

    @Query("SELECT DISTINCT ea.annotatorId FROM EditorialAnnotation ea WHERE ea.contentId = :contentId")
    List<UUID> findDistinctAnnotatorIdsByContentId(@Param("contentId") UUID contentId);

    @Modifying
    @Transactional
    @Query("UPDATE EditorialAnnotation ea SET ea.anchorStatus = com.allocator.contentservice.model.AnchorStatus.STALE " +
           "WHERE ea.contentId = :contentId AND ea.contentVersionNumber < :versionNumber AND ea.anchorStart IS NOT NULL")
    void markAnchorsStale(@Param("contentId") UUID contentId, @Param("versionNumber") int versionNumber);
}
