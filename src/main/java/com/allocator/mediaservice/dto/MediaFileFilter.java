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
public class MediaFileFilter {

    private MediaFile.MediaType mediaType;
    private UUID folderId;
    private UUID uploadedBy;
    private String tag;
    private List<String> tags;
    private String keyword;
    private Instant uploadedAfter;
    private Instant uploadedBefore;
}
