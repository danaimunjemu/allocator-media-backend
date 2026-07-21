package com.allocator.analyticsservice.service;

import com.allocator.common.event.ArticlePublishedEvent;
import com.allocator.common.event.ArticleViewedEvent;
import com.allocator.common.event.MediaUploadedEvent;
import com.allocator.analyticsservice.model.ActivityLog;
import com.allocator.analyticsservice.model.ArticleAnalytics;
import com.allocator.analyticsservice.model.AuthorAnalytics;
import com.allocator.analyticsservice.model.TopicAnalytics;
import com.allocator.analyticsservice.repository.ActivityLogRepository;
import com.allocator.analyticsservice.repository.ArticleAnalyticsRepository;
import com.allocator.analyticsservice.repository.AuthorAnalyticsRepository;
import com.allocator.analyticsservice.repository.TopicAnalyticsRepository;
import com.allocator.analyticsservice.repository.ArticleViewRepository;
import com.allocator.analyticsservice.repository.SearchQueryRepository;
import com.allocator.analyticsservice.repository.NewsletterOpenRepository;
import com.allocator.analyticsservice.repository.NewsletterClickRepository;
import com.allocator.analyticsservice.dto.ViewTrackingRequest;
import com.allocator.analyticsservice.dto.SearchTrackingRequest;
import com.allocator.analyticsservice.dto.DownloadTrackingRequest;
import com.allocator.analyticsservice.model.SearchQuery;
import com.allocator.analyticsservice.model.ArticleView;
import com.allocator.analyticsservice.model.ContentDownload;
import com.allocator.analyticsservice.model.NewsletterOpen;
import com.allocator.analyticsservice.model.NewsletterClick;
import com.allocator.analyticsservice.dto.NotificationEvent;
import com.allocator.analyticsservice.repository.ContentDownloadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsUpdateServiceImpl implements AnalyticsUpdateService {

    private final ActivityLogRepository logRepository;
    private final ArticleAnalyticsRepository articleRepository;
    private final AuthorAnalyticsRepository authorRepository;
    private final TopicAnalyticsRepository topicRepository;
    private final ArticleViewRepository viewRepository;
    private final SearchQueryRepository searchRepository;
    private final NewsletterOpenRepository newsletterOpenRepository;
    private final NewsletterClickRepository newsletterClickRepository;
    private final ContentDownloadRepository downloadRepository;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public void processArticlePublished(ArticlePublishedEvent event) {
        java.util.UUID articleId = java.util.UUID.fromString(event.getArticleId());
        if (!articleRepository.existsById(articleId)) {
            ArticleAnalytics article = ArticleAnalytics.builder()
                    .articleId(articleId)
                    .title(event.getTitle())
                    .authorId(event.getAuthorId() != null ? event.getAuthorId() : "system")
                    .views(0)
                    .uniqueVisitors(0)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            articleRepository.save(article);
            
            meterRegistry.counter("analytics.events.count", "type", "ArticlePublished").increment();

            updateAuthorArticleCount(event.getAuthorId() != null ? event.getAuthorId() : "system", "Unknown");
        }
    }

    @Override
    @Transactional
    public void processArticleViewed(ArticleViewedEvent event) {
        // Log activity
        ActivityLog activity = ActivityLog.builder()
                .userId(event.getUserId())
                .contentId(event.getArticleId())
                .activityType("VIEW")
                .timestamp(LocalDateTime.now())
                .build();
        logRepository.save(activity);

        // Update Article aggregate
        java.util.UUID articleId = java.util.UUID.fromString(event.getArticleId());
        ArticleAnalytics article = articleRepository.findById(articleId)
                .orElseGet(() -> ArticleAnalytics.builder()
                        .articleId(articleId)
                        .title("Unknown")
                        .authorId("Unknown")
                        .views(0)
                        .uniqueVisitors(0)
                        .build());

        article.setViews(article.getViews() + 1);
        article.setLastUpdated(LocalDateTime.now());
        articleRepository.save(article);

        // Update Author aggregate
        if (article.getAuthorId() != null && !article.getAuthorId().equals("Unknown")) {
            updateAuthorViews(article.getAuthorId());
        }
    }

    @Override
    @Transactional
    public void processMediaUploaded(MediaUploadedEvent event) {
        ActivityLog activity = ActivityLog.builder()
                .userId(event.getUploaderId())
                .contentId(event.getFileId())
                .activityType("MEDIA_UPLOAD")
                .metadata(event.getFileName())
                .timestamp(LocalDateTime.now())
                .build();
        logRepository.save(activity);
        
        meterRegistry.counter("analytics.media.uploads.count").increment();
    }

    private void updateAuthorArticleCount(String authorId, String authorName) {
        AuthorAnalytics author = authorRepository.findById(authorId)
                .orElse(AuthorAnalytics.builder()
                        .authorId(authorId)
                        .authorName(authorName)
                        .totalViews(0)
                        .articlesCount(0)
                        .build());

        author.setArticlesCount(author.getArticlesCount() + 1);
        author.setLastUpdated(LocalDateTime.now());
        authorRepository.save(author);
    }

    private void updateAuthorViews(String authorId) {
        authorRepository.findById(authorId).ifPresent(author -> {
            author.setTotalViews(author.getTotalViews() + 1);
            author.setLastUpdated(LocalDateTime.now());
            authorRepository.save(author);
        });
    }

    private void updateTopicViews(String topicName) {
        TopicAnalytics topic = topicRepository.findById(topicName)
                .orElse(TopicAnalytics.builder()
                        .topic(topicName)
                        .views(0)
                        .growthRate(0)
                        .build());

        topic.setViews(topic.getViews() + 1);
        topic.setLastUpdated(LocalDateTime.now());
        topicRepository.save(topic);
    }

    @Override
    @Transactional
    public void processNewsletterOpen(NotificationEvent event) {
        NewsletterOpen open = NewsletterOpen.builder()
                .notificationId(java.util.UUID.fromString(event.getNotificationId()))
                .email(event.getEmail())
                .openedAt(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        newsletterOpenRepository.save(open);
        meterRegistry.counter("analytics.events.count", "type", "NewsletterOpened").increment();
        log.info("Recorded newsletter open for: {}", event.getEmail());
    }

    @Override
    @Transactional
    public void processNewsletterClick(NotificationEvent event) {
        NewsletterClick click = NewsletterClick.builder()
                .notificationId(java.util.UUID.fromString(event.getNotificationId()))
                .email(event.getEmail())
                .url(event.getUrl())
                .clickedAt(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .build();
        newsletterClickRepository.save(click);
        meterRegistry.counter("analytics.events.count", "type", "NewsletterClicked").increment();
        log.info("Recorded newsletter click for: {} on URL: {}", event.getEmail(), event.getUrl());
    }

    @Override
    @Transactional
    public void trackView(String ipAddress, ViewTrackingRequest request) {
        // Truncate userAgent to 512 chars to match column definition
        String userAgent = request.getUserAgent() != null && request.getUserAgent().length() > 512
                ? request.getUserAgent().substring(0, 512)
                : request.getUserAgent();

        ArticleView view = ArticleView.builder()
                .contentId(request.getContentId())
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .ipAddress(ipAddress)
                .brandId(request.getBrandId())
                .slug(request.getSlug())
                .referrer(request.getReferrer())
                .userAgent(userAgent)
                .viewedAt(LocalDateTime.now())
                .readingTimeSeconds(request.getReadingTimeSeconds() != null ? request.getReadingTimeSeconds() : 0L)
                .build();
        viewRepository.save(view);
        meterRegistry.counter("analytics.views.count").increment();
        log.info("Tracked page view — slug={} brandId={} ip={}",
                request.getSlug(), request.getBrandId(), ipAddress);
    }

    @Override
    @Transactional
    public void trackSearch(SearchTrackingRequest request) {
        SearchQuery query = SearchQuery.builder()
                .query(request.getQuery())
                .userId(request.getUserId())
                .searchedAt(LocalDateTime.now())
                .build();
        searchRepository.save(query);
        meterRegistry.counter("analytics.search.count").increment();
        log.info("Tracked search query: {}", request.getQuery());
    }

    @Override
    @Transactional
    public void trackDownload(String ipAddress, DownloadTrackingRequest request) {
        ContentDownload download = ContentDownload.builder()
                .contentId(request.getContentId())
                .brandId(request.getBrandId())
                .userId(request.getUserId())
                .ipAddress(ipAddress)
                .downloadedAt(LocalDateTime.now())
                .build();
        downloadRepository.save(download);
        meterRegistry.counter("analytics.downloads.count").increment();
        log.info("Tracked download — contentId={} brandId={} ip={}",
                request.getContentId(), request.getBrandId(), ipAddress);
    }
}
