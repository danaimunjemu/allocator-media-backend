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
@Table(name = "article_analytics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAnalytics {

    @Id
    private java.util.UUID articleId;

    private String title;
    private String authorId;
    private long views;
    private long uniqueVisitors;
    private java.time.LocalDateTime lastUpdated;
}
