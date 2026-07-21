package com.allocator.contentservice.service;

import com.allocator.contentservice.dto.event.ContentEvent;
import com.allocator.contentservice.dto.event.ContentEventPayload;
import com.allocator.contentservice.dto.event.ContentEventType;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CONTENT_EVENTS_TOPIC = "content.events";

    // ─── Canonical event publishers ──────────────────────────────────────────

    public CompletableFuture<Void> publishContentCreated(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_CREATED, content, null, ContentStatus.DRAFT, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentUpdated(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_UPDATED, content, content.getStatus(), content.getStatus(), actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentSubmitted(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_SUBMITTED, content, ContentStatus.DRAFT, ContentStatus.REVIEW, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentRejected(Content content, UUID actorId, String actorRole, String reason) {
        return publish(ContentEventType.CONTENT_REJECTED, content, ContentStatus.REVIEW, ContentStatus.DRAFT, actorId, actorRole, reason);
    }

    public CompletableFuture<Void> publishContentApproved(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_APPROVED, content, ContentStatus.REVIEW, ContentStatus.APPROVED, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentScheduled(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_SCHEDULED, content, ContentStatus.APPROVED, ContentStatus.APPROVED, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentPublished(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_PUBLISHED, content, ContentStatus.APPROVED, ContentStatus.PUBLISHED, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentArchived(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_ARCHIVED, content, content.getStatus(), ContentStatus.ARCHIVED, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentRestored(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_RESTORED, content, ContentStatus.ARCHIVED, ContentStatus.DRAFT, actorId, actorRole, null);
    }

    public CompletableFuture<Void> publishContentUnpublished(Content content, UUID actorId, String actorRole) {
        return publish(ContentEventType.CONTENT_UNPUBLISHED, content, ContentStatus.PUBLISHED, ContentStatus.DRAFT, actorId, actorRole, null);
    }

    // ─── Legacy shim — kept for backward-compatible consumers ────────────────

    public CompletableFuture<Void> publishArticleCreated(Content content) {
        return publishLegacy(ContentEventType.ARTICLE_CREATED, content);
    }

    public CompletableFuture<Void> publishArticleUpdated(Content content) {
        return publishLegacy(ContentEventType.ARTICLE_UPDATED, content);
    }

    public CompletableFuture<Void> publishArticlePublished(Content content) {
        return publishLegacy(ContentEventType.ARTICLE_PUBLISHED, content);
    }

    public CompletableFuture<Void> publishArticleDeleted(Content content) {
        return publishLegacy(ContentEventType.ARTICLE_DELETED, content);
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private CompletableFuture<Void> publish(
            ContentEventType eventType,
            Content content,
            ContentStatus fromStatus,
            ContentStatus toStatus,
            UUID actorId,
            String actorRole,
            String reason) {

        ContentEventPayload payload = buildPayload(content, fromStatus, toStatus, actorId, actorRole, reason);

        ContentEvent event = ContentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType.name())
                .aggregateType("Content")
                .aggregateId(content.getId())
                .brandId(content.getBrandId())
                .timestamp(java.time.Instant.now())
                .payload(payload)
                .build();

        log.info("Publishing {} event for content: {}", eventType, content.getId());

        return kafkaTemplate.send(CONTENT_EVENTS_TOPIC, content.getId().toString(), event)
                .thenRun(() -> log.debug("Published {} event for content: {}", eventType, content.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish {} event for content: {}", eventType, content.getId(), throwable);
                    return null;
                });
    }

    private CompletableFuture<Void> publishLegacy(ContentEventType eventType, Content content) {
        ContentEventPayload payload = buildPayload(content, null, null, null, null, null);

        ContentEvent event = ContentEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType.name())
                .aggregateType("Content")
                .aggregateId(content.getId())
                .brandId(content.getBrandId())
                .timestamp(java.time.Instant.now())
                .payload(payload)
                .build();

        return kafkaTemplate.send(CONTENT_EVENTS_TOPIC, content.getId().toString(), event)
                .thenRun(() -> log.debug("Published legacy {} event for content: {}", eventType, content.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish legacy {} event for content: {}", eventType, content.getId(), throwable);
                    return null;
                });
    }

    private ContentEventPayload buildPayload(Content content, ContentStatus fromStatus, ContentStatus toStatus,
                                              UUID actorId, String actorRole, String reason) {
        List<String> tags = content.getTags() != null ? content.getTags() : List.of();
        Map<String, Object> metadata = content.getMetadata() != null ? content.getMetadata() : Map.of();

        return ContentEventPayload.builder()
                .contentId(content.getId())
                .brandId(content.getBrandId())
                .title(content.getTitle())
                .slug(content.getSlug())
                .contentType(content.getContentType())
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus != null ? toStatus.name() : null)
                .actorId(actorId)
                .actorRole(actorRole)
                .reason(reason)
                .publishedAt(content.getPublishedAt())
                .scheduledAt(content.getScheduledAt())
                .tags(tags)
                .metadata(metadata)
                .build();
    }
}
