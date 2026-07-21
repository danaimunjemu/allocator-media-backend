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

import java.util.UUID;

@Entity
@Table(name = "article_engagement", indexes = {
        @Index(name = "idx_article_engagement_content_id", columnList = "contentId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleEngagement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String contentId;
    private Long totalViews;
    private Long uniqueViews;
    private Double averageReadingTime;
}
