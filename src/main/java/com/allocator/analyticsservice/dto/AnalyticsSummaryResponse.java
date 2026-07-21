package com.allocator.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsSummaryResponse {

    // Analytics-service data
    private long totalPageViews;
    private long totalArticleViews;
    private long totalDownloads;
    private List<TopArticleSummary> topArticles;
    private List<TrendingTopicSummary> trendingTopics;

    // Subscription data (from payment-service; null when payment-service unavailable)
    private long totalSubscribers;
    private long newSubscribersThisPeriod;
    private BigDecimal mrr;
    private BigDecimal arr;
    private long paidSubscribers;
    private long freeSubscribers;
}
