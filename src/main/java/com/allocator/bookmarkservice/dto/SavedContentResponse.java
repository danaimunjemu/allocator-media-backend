package com.allocator.bookmarkservice.dto;

import com.allocator.contentservice.dto.ContentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedContentResponse {
    private ContentResponse content;
    private Instant savedAt;
}
