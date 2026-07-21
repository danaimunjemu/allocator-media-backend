package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, UUID> {
    
    @org.springframework.data.jpa.repository.Query("SELECT s.query FROM SearchQuery s GROUP BY s.query ORDER BY COUNT(s) DESC")
    java.util.List<String> findTrendingQueries(org.springframework.data.domain.Pageable pageable);
}
