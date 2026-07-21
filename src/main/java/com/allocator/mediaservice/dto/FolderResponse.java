package com.allocator.mediaservice.dto;

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
public class FolderResponse {

    private UUID id;
    private String name;
    private UUID parentFolderId;
    private List<FolderResponse> childFolders;
    private Integer fileCount;
    private Instant createdAt;
    private Instant updatedAt;
}
