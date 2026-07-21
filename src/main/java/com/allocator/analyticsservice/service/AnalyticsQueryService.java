package com.allocator.analyticsservice.service;

import com.allocator.analyticsservice.model.ArticleAnalytics;
import com.allocator.analyticsservice.model.AuthorAnalytics;
import com.allocator.analyticsservice.model.TopicAnalytics;

import java.util.List;

public interface AnalyticsQueryService {
    List<ArticleAnalytics> getTopArticles(int limit);

    List<TopicAnalytics> getTrendingTopics(int limit);

    List<AuthorAnalytics> getAuthorPerformance();

    List<com.allocator.analyticsservice.dto.TrendingArticleResponse> getTrendingArticles(int limit);

    AuthorAnalytics getAuthorStats(String authorId);

    List<String> getTrendingSearches();
}
