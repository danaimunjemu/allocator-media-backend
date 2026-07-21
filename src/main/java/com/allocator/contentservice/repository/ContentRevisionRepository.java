package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.ContentRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRevisionRepository extends JpaRepository<ContentRevision, UUID> {

    List<ContentRevision> findByContentIdOrderByRevisionNumberDesc(UUID contentId);

    Optional<ContentRevision> findByContentIdAndRevisionNumber(UUID contentId, Integer revisionNumber);

    @Query("SELECT cr FROM ContentRevision cr WHERE cr.content.id = :contentId ORDER BY cr.revisionNumber DESC")
    Optional<ContentRevision> findLatestRevision(@Param("contentId") UUID contentId);

    @Query("SELECT MAX(cr.revisionNumber) FROM ContentRevision cr WHERE cr.content.id = :contentId")
    Integer findMaxRevisionNumber(@Param("contentId") UUID contentId);

    void deleteByContentId(UUID contentId);
}
