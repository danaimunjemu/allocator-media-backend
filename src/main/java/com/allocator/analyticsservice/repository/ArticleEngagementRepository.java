package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.ArticleEngagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface ArticleEngagementRepository extends JpaRepository<ArticleEngagement, UUID> {
    Optional<ArticleEngagement> findByContentId(String contentId);
}
