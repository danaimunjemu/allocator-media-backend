package com.allocator.mediaservice.kafka;

import com.allocator.mediaservice.dto.event.MediaEvent;
import com.allocator.mediaservice.dto.event.MediaEventPayload;
import com.allocator.mediaservice.model.MediaFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String MEDIA_EVENTS_TOPIC = "media.events";

    public CompletableFuture<Void> publishMediaUploaded(MediaFile mediaFile) {
        MediaEventPayload payload = createEventPayload(mediaFile);
        
        MediaEvent event = MediaEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("MediaUploaded")
                .aggregateId(mediaFile.getId())
                .timestamp(java.time.Instant.now())
                .payload(payload)
                .build();

        log.info("Publishing MediaUploaded event for media: {}", mediaFile.getId());
        
        return kafkaTemplate.send(MEDIA_EVENTS_TOPIC, mediaFile.getId().toString(), event)
                .thenRun(() -> log.info("Successfully published MediaUploaded event for media: {}", mediaFile.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish MediaUploaded event for media: {}", mediaFile.getId(), throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> publishMediaDeleted(MediaFile mediaFile) {
        MediaEventPayload payload = createEventPayload(mediaFile);
        
        MediaEvent event = MediaEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("MediaDeleted")
                .aggregateId(mediaFile.getId())
                .timestamp(java.time.Instant.now())
                .payload(payload)
                .build();

        log.info("Publishing MediaDeleted event for media: {}", mediaFile.getId());
        
        return kafkaTemplate.send(MEDIA_EVENTS_TOPIC, mediaFile.getId().toString(), event)
                .thenRun(() -> log.info("Successfully published MediaDeleted event for media: {}", mediaFile.getId()))
                .exceptionally(throwable -> {
                    log.error("Failed to publish MediaDeleted event for media: {}", mediaFile.getId(), throwable);
                    return null;
                });
    }

    private MediaEventPayload createEventPayload(MediaFile mediaFile) {
        return MediaEventPayload.builder()
                .mediaId(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .mediaType(mediaFile.getMediaType().toString())
                .fileSize(mediaFile.getFileSize())
                .url(mediaFile.getUrl())
                .build();
    }
}
