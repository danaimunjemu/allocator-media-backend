package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.model.MediaTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaTagRepository extends JpaRepository<MediaTag, UUID> {

    List<MediaTag> findByMediaFileId(UUID mediaFileId);

    List<MediaTag> findByTag(String tag);

    @Query("SELECT DISTINCT t.tag FROM MediaTag t")
    List<String> findDistinctTags();

    @Query("SELECT t FROM MediaTag t WHERE t.mediaFile.id = :mediaFileId")
    List<MediaTag> findByMediaFile(@Param("mediaFileId") UUID mediaFileId);

    void deleteByMediaFileId(UUID mediaFileId);
}
