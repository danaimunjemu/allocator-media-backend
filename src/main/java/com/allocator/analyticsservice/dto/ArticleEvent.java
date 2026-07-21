package com.allocator.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleEvent {
    private String articleId;
    private String title;
    private String authorId;
    private String authorName;
    private List<String> topics;
    private String eventType; // PUBLISHED, VIEW, READ_COMPLETE
    private String userId; // Optional, present for user-driven events
}
