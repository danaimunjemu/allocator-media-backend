package com.allocator.mediaservice.repository;

import com.allocator.mediaservice.model.Media;
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
public interface MediaRepository extends JpaRepository<Media, UUID>, MediaRepositoryCustom {

    Optional<Media> findByFileName(String fileName);

    Optional<Media> findByStorageUrl(String storageUrl);

    List<Media> findByBrandId(UUID brandId);

    Page<Media> findByBrandId(UUID brandId, Pageable pageable);

    List<Media> findByContentType(Media.MediaType contentType);

    Page<Media> findByBrandIdAndContentType(UUID brandId, Media.MediaType contentType, Pageable pageable);

    List<Media> findByUploadedBy(UUID uploadedBy);

    Page<Media> findByUploadedBy(UUID uploadedBy, Pageable pageable);

    @Query("SELECT m FROM Media m WHERE m.isArchived = false AND m.expiresAt IS NULL OR m.expiresAt > :now")
    Page<Media> findActiveMedia(Pageable pageable, @Param("now") Instant now);

    @Query("SELECT m FROM Media m WHERE m.brandId = :brandId AND m.isArchived = false AND (m.expiresAt IS NULL OR m.expiresAt > :now)")
    Page<Media> findActiveMediaByBrand(UUID brandId, Pageable pageable, @Param("now") Instant now);

    @Query("SELECT m FROM Media m WHERE m.contentType = :contentType AND m.isArchived = false AND (m.expiresAt IS NULL OR m.expiresAt > :now)")
    Page<Media> findActiveMediaByContentType(Media.MediaType contentType, Pageable pageable, @Param("now") Instant now);

    @Query("SELECT m FROM Media m WHERE m.processingStatus = :status")
    List<Media> findByProcessingStatus(@Param("status") Media.ProcessingStatus status);

    @Query("SELECT COUNT(m) FROM Media m WHERE m.brandId = :brandId AND m.isArchived = false")
    Long countActiveMediaByBrand(@Param("brandId") UUID brandId);

    @Query("SELECT m FROM Media m WHERE m.fileName LIKE %:keyword% OR m.originalName LIKE %:keyword%")
    Page<Media> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    void deleteByStorageUrl(String storageUrl);

    @Query("UPDATE Media m SET m.linkedContentCount = m.linkedContentCount + 1 WHERE m.id = :mediaId")
    void incrementLinkedContentCount(@Param("mediaId") UUID mediaId);

    @Query("UPDATE Media m SET m.linkedContentCount = GREATEST(m.linkedContentCount - 1, 0) WHERE m.id = :mediaId")
    void decrementLinkedContentCount(@Param("mediaId") UUID mediaId);
}
