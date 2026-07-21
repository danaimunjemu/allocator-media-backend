package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.WorkflowAuditLog;
import com.allocator.contentservice.model.WorkflowEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowAuditLogRepository extends JpaRepository<WorkflowAuditLog, UUID> {

    List<WorkflowAuditLog> findByContentIdOrderByTimestampAsc(UUID contentId);

    Page<WorkflowAuditLog> findByBrandId(UUID brandId, Pageable pageable);

    List<WorkflowAuditLog> findByActorIdOrderByTimestampDesc(UUID actorId);

    List<WorkflowAuditLog> findByEventTypeAndBrandId(WorkflowEventType eventType, UUID brandId);

    @Query("SELECT w FROM WorkflowAuditLog w WHERE w.brandId = :brandId AND w.timestamp BETWEEN :from AND :to ORDER BY w.timestamp DESC")
    List<WorkflowAuditLog> findByBrandIdAndTimestampBetween(
            @Param("brandId") UUID brandId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("SELECT COUNT(w) FROM WorkflowAuditLog w WHERE w.brandId = :brandId AND w.eventType = :eventType AND w.timestamp >= :since")
    long countByBrandIdAndEventTypeSince(
            @Param("brandId") UUID brandId,
            @Param("eventType") WorkflowEventType eventType,
            @Param("since") Instant since
    );

    /**
     * Filtered paginated audit query. All parameters are optional (null = no filter).
     * Supports: contentId, brandId, actorId, eventType, timestamp range.
     */
    @Query("""
            SELECT w FROM WorkflowAuditLog w
            WHERE (:contentId IS NULL OR w.contentId = :contentId)
              AND (:brandId   IS NULL OR w.brandId   = :brandId)
              AND (:actorId   IS NULL OR w.actorId   = :actorId)
              AND (:eventType IS NULL OR w.eventType = :eventType)
              AND (:from      IS NULL OR w.timestamp >= :from)
              AND (:to        IS NULL OR w.timestamp <= :to)
            ORDER BY w.timestamp DESC
            """)
    Page<WorkflowAuditLog> findFiltered(
            @Param("contentId")  UUID contentId,
            @Param("brandId")    UUID brandId,
            @Param("actorId")    UUID actorId,
            @Param("eventType")  WorkflowEventType eventType,
            @Param("from")       Instant from,
            @Param("to")         Instant to,
            Pageable pageable
    );
}
