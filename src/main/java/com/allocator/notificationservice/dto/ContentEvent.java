package com.allocator.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEvent {
    private String eventType;
    private String contentId;
    private String brandId;
    private String title;
    private String summary;
    private String body;
    private String contentType;
    private List<String> tags;
    private List<String> topics;
    private String sector;
    private String authorId;
    private LocalDateTime publishedAt;
}
