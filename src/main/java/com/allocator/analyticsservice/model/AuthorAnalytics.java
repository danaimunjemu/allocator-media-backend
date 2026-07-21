package com.allocator.analyticsservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "author_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorAnalytics {

    @Id
    private String authorId;

    private String authorName;
    private long totalViews;
    private long articlesCount;
    private double averageEngagement;
    private LocalDateTime lastUpdated;
}
