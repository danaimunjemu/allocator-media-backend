package com.allocator.mediaservice.service;

import com.allocator.mediaservice.dto.MediaFilter;
import com.allocator.mediaservice.dto.MediaResponse;
import com.allocator.mediaservice.dto.MediaUploadRequest;
import com.allocator.mediaservice.mapper.MediaMapper;
import com.allocator.mediaservice.model.Media;
import com.allocator.mediaservice.repository.MediaRepository;
import com.allocator.mediaservice.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    private final MediaMapper mediaMapper;
    private final MediaEventPublisherService mediaEventPublisherService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MediaResponse uploadFile(MultipartFile file, MediaUploadRequest request, UUID userId, String brandIds) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            // Generate unique filename
            String folder = request.getFolder() != null ? request.getFolder() : "uploads";
            String storageUrl = fileStorageService.uploadFile(file, folder);

            // Create media entity
            Media media = Media.builder()
                    .fileName(storageUrl.substring(storageUrl.lastIndexOf('/') + 1))
                    .originalName(file.getOriginalFilename())
                    .contentType(determineContentType(file.getContentType()))
                    .fileSize(file.getSize())
                    .filePath(storageUrl)
                    .storageUrl(storageUrl)
                    .brandId(request.getBrandId())
                    .uploadedBy(userId)
                    .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                    .isArchived(false)
                    .processingStatus(Media.ProcessingStatus.COMPLETED)
                    .expiresAt(request.getExpiresInSeconds() != null ? 
                            Instant.now().plusSeconds(request.getExpiresInSeconds()) : null)
                    .metadata(serializeMetadata(request.getMetadata()))
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("Successfully uploaded and saved media: {} for user: {}", savedMedia.getId(), userId);

            // Publish event
            mediaEventPublisherService.publishMediaUploadedEvent(savedMedia);

            return mediaMapper.toResponse(savedMedia);
        } catch (Exception e) {
            log.error("Failed to process uploaded file", e);
            throw new RuntimeException("Failed to process uploaded file", e);
        }
    }

    @Transactional(readOnly = true)
    public MediaResponse getMedia(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));
        return mediaMapper.toResponse(media);
    }

    @Transactional(readOnly = true)
    public DownloadData downloadFile(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));

        Resource resource = new InputStreamResource(fileStorageService.downloadFile(media.getFilePath()));
        return new DownloadData(media, resource);
    }

    @Transactional(readOnly = true)
    public String generatePresignedUrl(UUID id, int expiryMinutes) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));

        return fileStorageService.generatePresignedUrl(media.getFilePath(), expiryMinutes);
    }

    @Transactional(readOnly = true)
    public Page<MediaResponse> listMedia(MediaFilter filter, Pageable pageable) {
        Page<Media> mediaPage = mediaRepository.findAll(filter, pageable);
        return mediaPage.map(mediaMapper::toResponse);
    }

    @Transactional
    public MediaResponse updateMedia(UUID id, MediaResponse request, UUID userId) {
        Media existingMedia = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));

        // Update fields
        existingMedia.setIsPublic(request.getIsPublic());
        existingMedia.setMetadata(request.getMetadata());
        existingMedia.setUpdatedBy(userId);

        Media savedMedia = mediaRepository.save(existingMedia);
        log.info("Successfully updated media: {} by user: {}", savedMedia.getId(), userId);

        return mediaMapper.toResponse(savedMedia);
    }

    @Transactional
    public void deleteMedia(UUID id, UUID userId) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));

        // Delete from storage
        fileStorageService.deleteFile(media.getFilePath());

        // Delete from database
        mediaRepository.deleteById(id);

        log.info("Successfully deleted media: {} by user: {}", id, userId);
    }

    @Transactional
    public MediaResponse archiveMedia(UUID id, UUID userId) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));

        media.setIsArchived(true);
        media.setUpdatedBy(userId);

        Media savedMedia = mediaRepository.save(media);
        log.info("Successfully archived media: {} by user: {}", id, userId);

        return mediaMapper.toResponse(savedMedia);
    }

    @Transactional(readOnly = true)
    public Object getMediaStats(UUID brandId) {
        Long totalMedia = mediaRepository.countActiveMediaByBrand(brandId);
        
        return MediaStats.builder()
                .totalMedia(totalMedia)
                .build();
    }

    private Media.MediaType determineContentType(String contentType) {
        if (contentType == null) {
            return Media.MediaType.OTHER;
        }

        if (contentType.startsWith("image/")) {
            return Media.MediaType.IMAGE;
        } else if (contentType.startsWith("audio/")) {
            return Media.MediaType.AUDIO;
        } else if (contentType.startsWith("video/")) {
            return Media.MediaType.VIDEO;
        } else if (contentType.startsWith("application/pdf") || 
                   contentType.startsWith("application/msword") ||
                   contentType.startsWith("application/vnd.ms-") ||
                   contentType.startsWith("application/vnd.openxmlformats-")) {
            return Media.MediaType.DOCUMENT;
        } else {
            return Media.MediaType.OTHER;
        }
    }

    private String serializeMetadata(java.util.Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return null;
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class DownloadData {
        private final Media media;
        private final Resource resource;
    }

    @lombok.Data
    @lombok.Builder
    public static class MediaStats {
        private final Long totalMedia;
    }
}
