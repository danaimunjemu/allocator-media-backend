package com.allocator.analyticsservice.service;

import com.allocator.analyticsservice.client.PaymentServiceClient;
import com.allocator.analyticsservice.dto.AnalyticsSummaryResponse;
import com.allocator.analyticsservice.dto.PaymentSummaryDto;
import com.allocator.analyticsservice.dto.TopArticleSummary;
import com.allocator.analyticsservice.dto.TrendingTopicSummary;
import com.allocator.analyticsservice.model.ArticleAnalytics;
import com.allocator.analyticsservice.model.TopicAnalytics;
import com.allocator.analyticsservice.repository.ArticleAnalyticsRepository;
import com.allocator.analyticsservice.repository.ArticleViewRepository;
import com.allocator.analyticsservice.repository.ContentDownloadRepository;
import com.allocator.analyticsservice.repository.TopicAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsSummaryService {

    private static final int TOP_ARTICLES_LIMIT = 10;
    private static final int TRENDING_TOPICS_LIMIT = 5;

    private final ArticleViewRepository articleViewRepository;
    private final ArticleAnalyticsRepository articleAnalyticsRepository;
    private final TopicAnalyticsRepository topicAnalyticsRepository;
    private final ContentDownloadRepository contentDownloadRepository;
    private final PaymentServiceClient paymentServiceClient;

    /**
     * Returns a full dashboard summary for the given brand and time window.
     * Results are cached per brandId+period; cache is keyed as "brandId-period".
     * Cache TTL is managed by the cache provider (simple = no TTL by default).
     */
    @Cacheable(value = "analytics-summary", key = "#brandId + '-' + #period")
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(String brandId, String period) {
        LocalDateTime since = parsePeriod(period);

        // --- Analytics DB queries (three aggregated queries, no N+1) ---
        long totalArticleViews = articleViewRepository.countByPeriodAndBrandId(since, brandId);
        long totalDownloads    = contentDownloadRepository.countByPeriodAndBrandId(since, brandId);

        List<TopArticleSummary> topArticles = buildTopArticles(since, brandId);

        List<TrendingTopicSummary> trendingTopics = topicAnalyticsRepository
                .findTrendingTopics(PageRequest.of(0, TRENDING_TOPICS_LIMIT))
                .stream()
                .map(t -> TrendingTopicSummary.builder()
                        .topic(t.getTopic())
                        .views(t.getViews())
                        .growthRate(t.getGrowthRate())
                        .build())
                .collect(Collectors.toList());

        // --- Financial data from payment-service (best-effort) ---
        Optional<PaymentSummaryDto> paymentData = paymentServiceClient.fetchRevenueSummary();

        long totalSubscribers = 0;
        long paidSubscribers = 0;
        long freeSubscribers = 0;
        BigDecimal mrr = null;
        BigDecimal arr = null;

        if (paymentData.isPresent()) {
            PaymentSummaryDto p = paymentData.get();
            totalSubscribers = p.getTotalActiveSubscribers();
            mrr = p.getMonthlyRecurringRevenue();
            arr = p.getAnnualRecurringRevenue();

            if (p.getSubscribersByTier() != null) {
                Map<String, Long> byTier = p.getSubscribersByTier();
                freeSubscribers = byTier.getOrDefault("FREE", 0L);
                paidSubscribers = byTier.getOrDefault("STARTER", 0L)
                        + byTier.getOrDefault("PRO", 0L);
            }
        } else {
            log.warn("Payment-service data unavailable — financial fields will be null in summary response");
        }

        return AnalyticsSummaryResponse.builder()
                .totalPageViews(totalArticleViews)
                .totalArticleViews(totalArticleViews)
                .totalDownloads(totalDownloads)
                .newSubscribersThisPeriod(0L)       // Requires payment-service period endpoint
                .topArticles(topArticles)
                .trendingTopics(trendingTopics)
                .totalSubscribers(totalSubscribers)
                .paidSubscribers(paidSubscribers)
                .freeSubscribers(freeSubscribers)
                .mrr(mrr)
                .arr(arr)
                .build();
    }

    /**
     * Builds top articles by period in two queries:
     * 1. Aggregate (contentId, viewCount) from article_views
     * 2. Batch fetch titles from article_analytics by the returned IDs
     * No N+1.
     */
    private List<TopArticleSummary> buildTopArticles(LocalDateTime since, String brandId) {
        List<Object[]> rawRows = articleViewRepository.findTopContentsByPeriod(
                since, brandId, PageRequest.of(0, TOP_ARTICLES_LIMIT));

        if (rawRows.isEmpty()) {
            return List.of();
        }

        // Collect UUIDs for batch title lookup
        List<UUID> articleIds = rawRows.stream()
                .map(row -> {
                    try {
                        return UUID.fromString((String) row[0]);
                    } catch (IllegalArgumentException e) {
                        log.debug("contentId '{}' is not a valid UUID — skipping title enrichment", row[0]);
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // Batch fetch — single IN query
        Map<UUID, String> titleById = articleAnalyticsRepository
                .findAllByArticleIdIn(articleIds)
                .stream()
                .collect(Collectors.toMap(ArticleAnalytics::getArticleId, ArticleAnalytics::getTitle));

        return rawRows.stream()
                .map(row -> {
                    String contentId = (String) row[0];
                    long viewCount = ((Number) row[1]).longValue();
                    String title = null;
                    try {
                        title = titleById.get(UUID.fromString(contentId));
                    } catch (IllegalArgumentException ignored) { }

                    return TopArticleSummary.builder()
                            .articleId(contentId)
                            .title(title != null ? title : contentId)
                            .views(viewCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Parses period strings like "7d", "30d", "90d" into a cutoff LocalDateTime.
     * Falls back to 30 days for unrecognised formats.
     */
    private static LocalDateTime parsePeriod(String period) {
        if (period == null || period.isBlank()) {
            return LocalDateTime.now().minusDays(30);
        }
        String lower = period.trim().toLowerCase();
        try {
            if (lower.endsWith("d")) {
                long days = Long.parseLong(lower.substring(0, lower.length() - 1));
                return LocalDateTime.now().minusDays(days);
            }
            if (lower.endsWith("h")) {
                long hours = Long.parseLong(lower.substring(0, lower.length() - 1));
                return LocalDateTime.now().minusHours(hours);
            }
        } catch (NumberFormatException e) {
            log.warn("Unrecognised period format '{}' — defaulting to 30d", period);
        }
        return LocalDateTime.now().minusDays(30);
    }
}
