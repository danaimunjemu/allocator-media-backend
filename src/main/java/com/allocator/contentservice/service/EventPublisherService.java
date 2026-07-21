package com.allocator.contentservice.service;

import com.allocator.common.event.ArticlePublishedEvent;
import com.allocator.common.event.ArticleViewedEvent;
import com.allocator.common.event.ContentMediaLinkedEvent;
import com.allocator.contentservice.model.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service("contentEventPublisherService")
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    @Value("${app.kafka.topics.article:article-events}")
    private String articleEventsTopic;

    @Value("${app.kafka.topics.analytics:analytics-events}")
    private String analyticsEventsTopic;

    public CompletableFuture<Void> publishArticlePublishedEvent(Content content) {
        if (content == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("content must not be null"));
        }
        if (content.getId() == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("content.id must not be null"));
        }

        ArticlePublishedEvent event = ArticlePublishedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ARTICLE_PUBLISHED")
                .timestamp(LocalDateTime.now(clock))
                .sourceService("content-service")
                .articleId(content.getId().toString())
                .title(content.getTitle())
                .authorId(content.getAuthors() != null && !content.getAuthors().isEmpty() ? content.getAuthors().get(0).getId().toString() : null)
                .build();

        log.info("Publishing ArticlePublishedEvent articleId={} topic={}", content.getId(), articleEventsTopic);

        return kafkaTemplate.send(articleEventsTopic, content.getId().toString(), event)
                .thenRun(() -> log.info("Successfully published ArticlePublishedEvent articleId={}", content.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish ArticlePublishedEvent articleId={}", content.getId(), throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> publishArticleViewedEvent(Content content, UUID viewerId) {
        if (content == null || content.getId() == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("content and content.id must not be null"));
        }

        ArticleViewedEvent event = ArticleViewedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("ARTICLE_VIEWED")
                .timestamp(LocalDateTime.now(clock))
                .sourceService("content-service")
                .articleId(content.getId().toString())
                .userId(viewerId != null ? viewerId.toString() : null)
                .build();

        log.debug("Publishing ArticleViewedEvent articleId={} topic={}", content.getId(), analyticsEventsTopic);

        return kafkaTemplate.send(analyticsEventsTopic, content.getId().toString(), event)
                .thenRun(() -> log.debug("Successfully published ArticleViewedEvent articleId={}", content.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish ArticleViewedEvent articleId={}", content.getId(), throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> publishContentMediaLinkedEvent(Content content) {
        if (content == null || content.getId() == null || content.getMediaIds() == null || content.getMediaIds().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        ContentMediaLinkedEvent event = ContentMediaLinkedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CONTENT_MEDIA_LINKED")
                .timestamp(LocalDateTime.now(clock))
                .sourceService("content-service")
                .contentId(content.getId().toString())
                .mediaIds(content.getMediaIds())
                .build();

        log.info("Publishing ContentMediaLinkedEvent for contentId={} with {} media items", content.getId(), content.getMediaIds().size());

        return kafkaTemplate.send(articleEventsTopic, content.getId().toString(), event)
                .thenRun(() -> log.info("Successfully published ContentMediaLinkedEvent for contentId={}", content.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish ContentMediaLinkedEvent for contentId={}", content.getId(), throwable);
                    return null;
                });
    }
}


