package com.allocator.mediaservice.dto;

import com.allocator.mediaservice.model.MediaFile;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadRequest {

    @NotNull
    private MediaFile.MediaType mediaType;

    private UUID folderId;

    private String folder;
    private UUID brandId;
    private Boolean isPublic;
    private Long expiresInSeconds;
    private Map<String, String> metadata;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<String> tags;
}
