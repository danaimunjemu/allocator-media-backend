package com.allocator.mediaservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaEventPayload {
    private UUID mediaId;
    private String fileName;
    private String mediaType;
    private Long fileSize;
    private String url;
}
