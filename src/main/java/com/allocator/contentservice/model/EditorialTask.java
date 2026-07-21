package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "editorial_tasks", indexes = {
    @Index(name = "idx_et_content_id",     columnList = "content_id"),
    @Index(name = "idx_et_assignee_status", columnList = "assignee_id, status"),
    @Index(name = "idx_et_brand_status",   columnList = "brand_id, status")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EditorialTask extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "linked_comment_id")
    private UUID linkedCommentId;

    @Column(name = "linked_annotation_id")
    private UUID linkedAnnotationId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private TaskPriority priority = TaskPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "completed_by")
    private UUID completedBy;

    @Column(name = "completed_at")
    private Instant completedAt;
}
