package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findByUserId(String userId);

    List<ActivityLog> findByContentId(String contentId);

    @Query("SELECT a FROM ActivityLog a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) AND " +
            "(:activityType IS NULL OR a.activityType = :activityType) " +
            "ORDER BY a.timestamp DESC")
    Page<ActivityLog> findWithFilters(
            @Param("userId") String userId,
            @Param("activityType") String activityType,
            Pageable pageable);
}
