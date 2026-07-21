package com.allocator.contentservice.dto;

import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.model.ContentStatus;
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
public class ContentFilter {

    private UUID brandId;
    private ContentStatus status;
    private ContentType contentType;
    private UUID categoryId;
    private String topic;
    private String sector;
    private Integer year;
    private List<String> tags;
    private String keyword;
    private Boolean featured;
    private Boolean highlighted;
    private Instant createdFrom;
    private Instant createdTo;
}
