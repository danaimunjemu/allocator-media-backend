package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.EditorialTask;
import com.allocator.contentservice.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EditorialTaskRepository extends JpaRepository<EditorialTask, UUID> {

    List<EditorialTask> findByContentIdOrderByCreatedAtDesc(UUID contentId);

    List<EditorialTask> findByAssigneeIdAndStatusOrderByCreatedAtDesc(UUID assigneeId, TaskStatus status);

    List<EditorialTask> findByAssigneeIdOrderByCreatedAtDesc(UUID assigneeId);

    List<EditorialTask> findByBrandIdAndStatusOrderByCreatedAtDesc(UUID brandId, TaskStatus status);

    long countByAssigneeIdAndStatus(UUID assigneeId, TaskStatus status);

    @Query("SELECT DISTINCT et.assigneeId FROM EditorialTask et WHERE et.contentId = :contentId AND et.assigneeId IS NOT NULL")
    List<UUID> findDistinctAssigneeIdsByContentId(@Param("contentId") UUID contentId);
}
