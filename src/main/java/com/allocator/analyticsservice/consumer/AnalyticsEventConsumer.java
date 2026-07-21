package com.allocator.analyticsservice.consumer;

import com.allocator.common.event.ArticlePublishedEvent;
import com.allocator.common.event.ArticleViewedEvent;
import com.allocator.common.event.MediaUploadedEvent;
import com.allocator.analyticsservice.service.AnalyticsUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventConsumer {

    private final AnalyticsUpdateService analyticsUpdateService;

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleArticlePublished(ArticlePublishedEvent event) {
        log.info("Received ArticlePublishedEvent for articleId: {}", event.getArticleId());
        analyticsUpdateService.processArticlePublished(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleArticleViewed(ArticleViewedEvent event) {
        log.info("Received ArticleViewedEvent for articleId: {}", event.getArticleId());
        analyticsUpdateService.processArticleViewed(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleMediaUploaded(MediaUploadedEvent event) {
        log.info("Received MediaUploadedEvent for fileId: {}", event.getFileId());
        analyticsUpdateService.processMediaUploaded(event);
    }
}
