package com.allocator.bookmarkservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_content", indexes = {
    @Index(name = "idx_saved_content_user_id",    columnList = "user_id"),
    @Index(name = "idx_saved_content_content_id", columnList = "content_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
