package com.allocator.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_tracking", indexes = {
        @Index(name = "idx_tracking_notification_id", columnList = "notification_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTracking extends BaseEntity {

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Builder.Default
    @Column(nullable = false)
    private boolean opened = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean clicked = false;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
}
