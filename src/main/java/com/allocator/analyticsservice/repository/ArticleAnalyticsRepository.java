package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.ArticleAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleAnalyticsRepository extends JpaRepository<ArticleAnalytics, java.util.UUID> {
    
    @Query("SELECT a FROM ArticleAnalytics a ORDER BY a.views DESC")
    List<ArticleAnalytics> findMostReadArticles(Pageable pageable);

    List<ArticleAnalytics> findByAuthorId(String authorId);

    List<ArticleAnalytics> findAllByArticleIdIn(java.util.Collection<java.util.UUID> ids);
}
