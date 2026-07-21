package com.allocator.mediaservice.dto;

import com.allocator.mediaservice.model.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileResponse {

    private UUID id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private MediaFile.MediaType mediaType;
    private Long fileSize;
    private String storagePath;
    private String url;
    private UUID uploadedBy;
    private Instant uploadedAt;
    private String checksum;
    private UUID folderId;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
