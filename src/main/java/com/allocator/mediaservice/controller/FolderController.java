package com.allocator.mediaservice.controller;

import com.allocator.mediaservice.dto.ApiResponse;
import com.allocator.mediaservice.dto.FolderCreateRequest;
import com.allocator.mediaservice.dto.FolderResponse;
import com.allocator.mediaservice.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
@Slf4j
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Creating folder: {} with parent: {} by user: {}", 
                request.getName(), request.getParentFolderId(), userId);
        
        try {
            FolderResponse response = folderService.createFolder(request, userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Folder created successfully"));
        } catch (Exception e) {
            log.error("Failed to create folder: {}", request.getName(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create folder: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFolderTree() {
        log.info("Fetching folder tree");
        
        try {
            List<FolderResponse> folderTree = folderService.getFolderTree();
            return ResponseEntity.ok(ApiResponse.success(folderTree));
        } catch (Exception e) {
            log.error("Failed to fetch folder tree", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to fetch folder tree"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FolderResponse>> getFolder(@PathVariable UUID id) {
        log.info("Fetching folder: {}", id);
        
        try {
            FolderResponse response = folderService.getFolder(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to fetch folder: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("Deleting folder: {} by user: {}", id, userId);
        
        try {
            folderService.deleteFolder(id, userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Folder deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete folder: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete folder: " + e.getMessage()));
        }
    }
}
