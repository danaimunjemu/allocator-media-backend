package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.model.MediaFile;
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

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {

    Optional<MediaFile> findByFileName(String fileName);

    Optional<MediaFile> findByUrl(String url);

    List<MediaFile> findByMediaType(MediaFile.MediaType mediaType);

    Page<MediaFile> findByMediaType(MediaFile.MediaType mediaType, Pageable pageable);

    List<MediaFile> findByUploadedBy(UUID uploadedBy);

    Page<MediaFile> findByUploadedBy(UUID uploadedBy, Pageable pageable);

    List<MediaFile> findByFolder_Id(UUID folderId);

    Page<MediaFile> findByFolder_Id(UUID folderId, Pageable pageable);

    @Query("SELECT m FROM MediaFile m JOIN m.tags t WHERE t.tag = :tag")
    List<MediaFile> findByTag(@Param("tag") String tag);

    @Query("SELECT m FROM MediaFile m JOIN m.tags t WHERE t.tag IN :tags")
    Page<MediaFile> findByTags(@Param("tags") List<String> tags, Pageable pageable);

    @Query("SELECT m FROM MediaFile m WHERE m.mediaType = :mediaType AND m.uploadedBy = :uploadedBy")
    Page<MediaFile> findByMediaTypeAndUploadedBy(
            @Param("mediaType") MediaFile.MediaType mediaType,
            @Param("uploadedBy") UUID uploadedBy,
            Pageable pageable);

    @Query("SELECT m FROM MediaFile m WHERE m.uploadedAt BETWEEN :startDate AND :endDate")
    Page<MediaFile> findByUploadedAtBetween(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT COUNT(m) FROM MediaFile m WHERE m.folder.id = :folderId")
    Long countByFolderId(@Param("folderId") UUID folderId);

    @Query("SELECT m FROM MediaFile m WHERE m.fileName LIKE %:keyword% OR m.originalFileName LIKE %:keyword%")
    Page<MediaFile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT m.mediaType FROM MediaFile m")
    List<MediaFile.MediaType> findDistinctMediaTypes();
}
