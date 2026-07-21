package com.allocator.mediaservice.service;

import com.allocator.common.event.MediaUploadedEvent;
import com.allocator.mediaservice.model.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.media:media-events}")
    private String mediaTopic;

    public void publishMediaUploadedEvent(Media media) {
        try {
            MediaUploadedEvent event = MediaUploadedEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .eventType("MediaUploaded")
                    .timestamp(LocalDateTime.now())
                    .sourceService("media-service")
                    .fileId(media.getId().toString())
                    .fileName(media.getOriginalName())
                    .contentType(media.getContentType().name())
                    .sizeBytes(media.getFileSize())
                    .uploaderId(media.getUploadedBy().toString())
                    .url(media.getStorageUrl())
                    .build();

            kafkaTemplate.send(mediaTopic, media.getId().toString(), event);
            log.info("Published MediaUploadedEvent for fileId: {}", media.getId());
        } catch (Exception e) {
            log.error("Failed to publish MediaUploadedEvent for fileId: {}", media.getId(), e);
        }
    }
}
