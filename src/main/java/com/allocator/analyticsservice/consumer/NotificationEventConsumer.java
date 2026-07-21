package com.allocator.analyticsservice.consumer;

import com.allocator.analyticsservice.dto.NotificationEvent;
import com.allocator.analyticsservice.service.AnalyticsUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service("analyticsNotificationEventConsumer")
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final AnalyticsUpdateService analyticsUpdateService;

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received in-memory notification event: {} for notification: {}", event.getEventType(), event.getNotificationId());

        if ("NewsletterOpened".equalsIgnoreCase(event.getEventType())) {
            analyticsUpdateService.processNewsletterOpen(event);
        } else if ("NewsletterClicked".equalsIgnoreCase(event.getEventType())) {
            analyticsUpdateService.processNewsletterClick(event);
        }
    }
}

