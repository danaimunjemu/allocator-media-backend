package com.allocator.searchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentIndexDto {
    private String contentId;
    private String id; // Alias for compatibility
    private String brandId;
    private String title;
    private String summary;
    private String body;
    private String contentType;
    private String slug;
    private List<String> tags;
    private List<String> topics;
    private String sector;
    private Integer year;
    private Instant publishedAt;
    private String authorId;
}
