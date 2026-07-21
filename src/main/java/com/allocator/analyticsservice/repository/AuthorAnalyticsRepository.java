package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.AuthorAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorAnalyticsRepository extends JpaRepository<AuthorAnalytics, String> {
}
