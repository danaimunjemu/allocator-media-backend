package com.allocator.mediaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {

    @NotBlank
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    private String name;

    private UUID parentFolderId;
}
