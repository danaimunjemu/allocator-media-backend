package com.allocator.analyticsservice.service;

import com.allocator.analyticsservice.model.ArticleAnalytics;
import com.allocator.analyticsservice.model.AuthorAnalytics;
import com.allocator.analyticsservice.model.TopicAnalytics;
import com.allocator.analyticsservice.repository.ArticleAnalyticsRepository;
import com.allocator.analyticsservice.repository.AuthorAnalyticsRepository;
import com.allocator.analyticsservice.repository.TopicAnalyticsRepository;
import com.allocator.analyticsservice.repository.ArticleViewRepository;
import com.allocator.analyticsservice.repository.ArticleEngagementRepository;
import com.allocator.analyticsservice.repository.SearchQueryRepository;
import com.allocator.analyticsservice.dto.TrendingArticleResponse;
import com.allocator.analyticsservice.model.ArticleEngagement;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final ArticleAnalyticsRepository articleRepository;
    private final AuthorAnalyticsRepository authorRepository;
    private final TopicAnalyticsRepository topicRepository;
    private final ArticleViewRepository viewRepository;
    private final ArticleEngagementRepository engagementRepository;
    private final SearchQueryRepository searchRepository;

    @Override
    public List<ArticleAnalytics> getTopArticles(int limit) {
        return articleRepository.findMostReadArticles(PageRequest.of(0, limit));
    }

    @Override
    public List<TopicAnalytics> getTrendingTopics(int limit) {
        return topicRepository.findTrendingTopics(PageRequest.of(0, limit));
    }

    @Override
    public List<AuthorAnalytics> getAuthorPerformance() {
        return authorRepository.findAll();
    }

    @Override
    public List<TrendingArticleResponse> getTrendingArticles(int limit) {
        java.time.LocalDateTime twentyFourHoursAgo = java.time.LocalDateTime.now().minusHours(24);
        
        List<ArticleAnalytics> allArticles = articleRepository.findAll();
        
        return allArticles.stream()
                .map(article -> {
                    String articleId = article.getArticleId().toString();
                    long views24h = viewRepository.countViewsSince(articleId, twentyFourHoursAgo);
                    
                    ArticleEngagement engagement = engagementRepository.findByContentId(articleId)
                            .orElse(ArticleEngagement.builder().build());
                    
                    // Score = views in 24h + (unique views / total views * avg reading time) - simplistic but covers the requirement
                    double engagementScore = 0.0;
                    if (engagement.getTotalViews() != null && engagement.getTotalViews() > 0) {
                        engagementScore = (double) engagement.getUniqueViews() / engagement.getTotalViews() * engagement.getAverageReadingTime();
                    }
                    
                    double finalScore = views24h + engagementScore;
                    
                    return TrendingArticleResponse.builder()
                            .articleId(articleId)
                            .title(article.getTitle())
                            .viewsLast24h(views24h)
                            .engagementScore(finalScore)
                            .build();
                })
                .sorted(java.util.Comparator.comparingDouble(TrendingArticleResponse::getEngagementScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public AuthorAnalytics getAuthorStats(String authorId) {
        AuthorAnalytics author = authorRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found: " + authorId));

        // Aggregate engagement from articles if not up-to-date
        List<ArticleAnalytics> articles = articleRepository.findByAuthorId(authorId);
        double totalEngagement = 0.0;
        for (ArticleAnalytics article : articles) {
            ArticleEngagement engagement = engagementRepository.findByContentId(article.getArticleId().toString())
                    .orElse(null);
            if (engagement != null) {
                totalEngagement += (double) engagement.getUniqueViews() / (engagement.getTotalViews() > 0 ? engagement.getTotalViews() : 1) * engagement.getAverageReadingTime();
            }
        }
        
        if (!articles.isEmpty()) {
            author.setAverageEngagement(totalEngagement / articles.size());
        }
        
        return author;
    }

    @Override
    public List<String> getTrendingSearches() {
        return searchRepository.findTrendingQueries(PageRequest.of(0, 10));
    }
}
