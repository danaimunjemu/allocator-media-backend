package com.allocator.mediaservice.controller;

import com.allocator.mediaservice.dto.ApiResponse;
import com.allocator.mediaservice.dto.MediaFilter;
import com.allocator.mediaservice.dto.MediaResponse;
import com.allocator.mediaservice.dto.MediaUploadRequest;
import com.allocator.mediaservice.dto.PresignedUrlRequest;
import com.allocator.mediaservice.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<MediaResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute MediaUploadRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Brand-Ids", required = false) String brandIds) {
        
        log.info("Uploading file: {} for user: {} in brand: {}", 
                file.getOriginalFilename(), userId, brandIds);
        
        try {
            MediaResponse response = mediaService.uploadFile(file, request, userId, brandIds);
            return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully"));
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload file"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaResponse>> getMedia(@PathVariable UUID id) {
        log.info("Fetching media: {}", id);
        
        try {
            MediaResponse response = mediaService.getMedia(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch media: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Media not found"));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        log.info("Downloading media: {}", id);
        
        try {
            var downloadData = mediaService.downloadFile(id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + URLEncoder.encode(
                                    downloadData.getMedia().getOriginalName(), 
                                    StandardCharsets.UTF_8) + "\"")
                    .body(downloadData.getResource());
        } catch (Exception e) {
            log.error("Failed to download media: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<ApiResponse<String>> generatePresignedUrl(
            @PathVariable UUID id,
            @Valid @RequestBody PresignedUrlRequest request) {
        
        log.info("Generating presigned URL for media: {} with expiry: {} minutes", id, request.getExpiryMinutes());
        
        try {
            String presignedUrl = mediaService.generatePresignedUrl(id, request.getExpiryMinutes());
            return ResponseEntity.ok(ApiResponse.success(presignedUrl, "Presigned URL generated"));
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate presigned URL"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ApiResponse.PageResponse<MediaResponse>>> listMedia(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) UUID uploadedBy,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) String processingStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("Listing media with filters - brandId: {}, contentType: {}, keyword: {}", 
                brandId, contentType, keyword);
        
        MediaFilter filter = MediaFilter.builder()
                .brandId(brandId)
                .contentType(contentType != null ? 
                        com.allocator.mediaservice.model.Media.MediaType.valueOf(contentType.toUpperCase()) : null)
                .uploadedBy(uploadedBy)
                .isPublic(isPublic)
                .isArchived(isArchived)
                .processingStatus(processingStatus != null ? 
                        com.allocator.mediaservice.model.Media.ProcessingStatus.valueOf(processingStatus.toUpperCase()) : null)
                .keyword(keyword)
                .build();
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<MediaResponse> mediaPage = mediaService.listMedia(filter, pageable);
        ApiResponse.PageResponse<MediaResponse> pageResponse = ApiResponse.PageResponse.from(mediaPage);
        
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaResponse>> updateMedia(
            @PathVariable UUID id,
            @Valid @RequestBody MediaResponse request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Updating media: {} by user: {}", id, userId);
        
        try {
            MediaResponse response = mediaService.updateMedia(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Media updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update media"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Deleting media: {} by user: {}", id, userId);
        
        try {
            mediaService.deleteMedia(id, userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Media deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete media"));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<MediaResponse>> archiveMedia(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Archiving media: {} by user: {}", id, userId);
        
        try {
            MediaResponse response = mediaService.archiveMedia(id, userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Media archived successfully"));
        } catch (Exception e) {
            log.error("Failed to archive media: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to archive media"));
        }
    }

    @GetMapping("/stats/brand/{brandId}")
    public ResponseEntity<ApiResponse<Object>> getMediaStats(@PathVariable UUID brandId) {
        log.info("Getting media stats for brand: {}", brandId);
        
        try {
            Object stats = mediaService.getMediaStats(brandId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get media stats for brand: {}", brandId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get media stats"));
        }
    }
}
