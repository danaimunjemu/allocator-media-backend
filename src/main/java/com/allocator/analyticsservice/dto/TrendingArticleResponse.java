package com.allocator.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingArticleResponse {
    private String articleId;
    private String title;
    private long viewsLast24h;
    private double engagementScore;
}
