package com.allocator.analyticsservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "newsletter_clicks", indexes = {
        @Index(name = "idx_newsletter_click_notification_id", columnList = "notificationId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterClick {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID notificationId;
    private String email;
    private String url;
    private LocalDateTime clickedAt;
}
