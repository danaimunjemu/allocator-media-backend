package com.allocator.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType; // NewsletterOpened, NewsletterClicked
    private String notificationId;
    private String email;
    private String url;
    private LocalDateTime timestamp;
}
