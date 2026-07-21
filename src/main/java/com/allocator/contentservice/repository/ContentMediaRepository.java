package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.ContentMedia;
import com.allocator.contentservice.model.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentMediaRepository extends JpaRepository<ContentMedia, UUID> {

    List<ContentMedia> findByContentId(UUID contentId);

    List<ContentMedia> findByContentIdOrderBySortOrder(UUID contentId);

    List<ContentMedia> findByMediaId(UUID mediaId);

    List<ContentMedia> findByType(MediaType type);

    @Query("SELECT cm FROM ContentMedia cm WHERE cm.content.id = :contentId AND cm.type = :type ORDER BY cm.sortOrder")
    List<ContentMedia> findByContentIdAndTypeOrderBySortOrder(@Param("contentId") UUID contentId, @Param("type") MediaType type);

    void deleteByContentId(UUID contentId);
}
