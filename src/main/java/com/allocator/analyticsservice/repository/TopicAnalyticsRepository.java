package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.TopicAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicAnalyticsRepository extends JpaRepository<TopicAnalytics, String> {

    @Query("SELECT t FROM TopicAnalytics t ORDER BY t.views DESC")
    List<TopicAnalytics> findTrendingTopics(Pageable pageable);
}
