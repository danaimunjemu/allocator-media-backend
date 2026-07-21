package com.allocator.mediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileUploadResponse {

    private UUID mediaId;
    private String fileName;
    private String url;
    private Long fileSize;
}
