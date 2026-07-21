package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.ReviewRequest;
import com.allocator.contentservice.model.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, UUID> {

    List<ReviewRequest> findByContentIdOrderByRequestedAtDesc(UUID contentId);

    List<ReviewRequest> findByAssigneeIdAndStatusOrderByRequestedAtDesc(UUID assigneeId, ReviewStatus status);

    List<ReviewRequest> findByBrandIdAndStatusOrderByRequestedAtDesc(UUID brandId, ReviewStatus status);

    List<ReviewRequest> findByAssigneeRoleAndBrandIdAndStatusOrderByRequestedAtDesc(
            String assigneeRole, UUID brandId, ReviewStatus status);

    long countByAssigneeIdAndStatus(UUID assigneeId, ReviewStatus status);

    @Query("SELECT DISTINCT rr.assigneeId FROM ReviewRequest rr WHERE rr.contentId = :contentId AND rr.assigneeId IS NOT NULL")
    List<UUID> findDistinctAssigneeIdsByContentId(@Param("contentId") UUID contentId);

    @Query("SELECT DISTINCT rr.requestedBy FROM ReviewRequest rr WHERE rr.contentId = :contentId")
    List<UUID> findDistinctRequesterIdsByContentId(@Param("contentId") UUID contentId);
}
