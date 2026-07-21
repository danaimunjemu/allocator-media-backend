package com.allocator.contentservice.service;

import com.allocator.contentservice.model.*;
import com.allocator.contentservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditorialTaskService {

    private final EditorialTaskRepository taskRepository;
    private final InAppNotificationRepository notificationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;
    private final ContentRepository contentRepository;

    public List<EditorialTask> getForContent(UUID contentId) {
        return taskRepository.findByContentIdOrderByCreatedAtDesc(contentId);
    }

    public List<EditorialTask> getForAssignee(UUID assigneeId, TaskStatus statusFilter) {
        if (statusFilter == null) {
            return taskRepository.findByAssigneeIdOrderByCreatedAtDesc(assigneeId);
        }
        return taskRepository.findByAssigneeIdAndStatusOrderByCreatedAtDesc(assigneeId, statusFilter);
    }

    @Transactional
    public EditorialTask create(UUID contentId, UUID createdBy, String title,
                                String description, UUID assigneeId, Instant dueDate,
                                TaskPriority priority, UUID linkedCommentId, UUID linkedAnnotationId) {
        UUID brandId = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + contentId))
                .getBrandId();
        EditorialTask task = EditorialTask.builder()
                .contentId(contentId)
                .brandId(brandId)
                .title(title)
                .description(description)
                .assigneeId(assigneeId)
                .dueDate(dueDate)
                .priority(priority != null ? priority : TaskPriority.NORMAL)
                .linkedCommentId(linkedCommentId)
                .linkedAnnotationId(linkedAnnotationId)
                .createdBy(createdBy)
                .build();

        EditorialTask saved = taskRepository.save(task);
        recordAudit(contentId, brandId, WorkflowEventType.TASK_CREATED, createdBy, null, title);

        if (assigneeId != null) {
            notifyAssignee(assigneeId, brandId, contentId, createdBy,
                    InAppNotificationType.TASK_ASSIGNED,
                    "You have been assigned a task: " + title,
                    "/content/" + contentId + "/tasks");
        }
        return saved;
    }

    @Transactional
    public EditorialTask update(UUID taskId, UUID updatingUserId, String title, String description,
                                UUID assigneeId, Instant dueDate, TaskPriority priority, TaskStatus status) {
        EditorialTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        UUID previousAssignee = task.getAssigneeId();
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (dueDate != null) task.setDueDate(dueDate);
        if (priority != null) task.setPriority(priority);
        if (status != null) task.setStatus(status);
        if (assigneeId != null) task.setAssigneeId(assigneeId);

        EditorialTask saved = taskRepository.save(task);

        boolean assigneeChanged = assigneeId != null && !assigneeId.equals(previousAssignee);
        if (assigneeChanged) {
            recordAudit(task.getContentId(), task.getBrandId(), WorkflowEventType.TASK_ASSIGNED,
                    updatingUserId, null, "Assigned to " + assigneeId);
            notifyAssignee(assigneeId, task.getBrandId(), task.getContentId(), updatingUserId,
                    InAppNotificationType.TASK_ASSIGNED,
                    "You have been assigned a task: " + task.getTitle(),
                    "/content/" + task.getContentId() + "/tasks");
        }
        return saved;
    }

    @Transactional
    public EditorialTask complete(UUID taskId, UUID completingUserId) {
        EditorialTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedBy(completingUserId);
        task.setCompletedAt(Instant.now());
        EditorialTask saved = taskRepository.save(task);

        recordAudit(task.getContentId(), task.getBrandId(), WorkflowEventType.TASK_COMPLETED,
                completingUserId, null, task.getTitle());

        UUID creator = task.getCreatedBy();
        if (creator != null && !creator.equals(completingUserId)) {
            notificationRepository.save(InAppNotification.builder()
                    .userId(creator).brandId(task.getBrandId())
                    .notificationType(InAppNotificationType.TASK_COMPLETED)
                    .contentId(task.getContentId()).actorId(completingUserId)
                    .message("Task completed: " + task.getTitle())
                    .linkPath("/content/" + task.getContentId() + "/tasks")
                    .build());
        }
        return saved;
    }

    @Transactional
    public EditorialTask cancel(UUID taskId, UUID cancellingUserId) {
        EditorialTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        task.setStatus(TaskStatus.CANCELLED);
        recordAudit(task.getContentId(), task.getBrandId(), WorkflowEventType.TASK_CANCELLED,
                cancellingUserId, null, task.getTitle());
        return taskRepository.save(task);
    }

    private void notifyAssignee(UUID assigneeId, UUID brandId, UUID contentId, UUID actorId,
                                 InAppNotificationType type, String message, String linkPath) {
        notificationRepository.save(InAppNotification.builder()
                .userId(assigneeId).brandId(brandId).notificationType(type)
                .contentId(contentId).actorId(actorId).message(message).linkPath(linkPath)
                .build());
    }

    private void recordAudit(UUID contentId, UUID brandId, WorkflowEventType eventType,
                              UUID actorId, String actorRole, String reason) {
        auditLogRepository.save(WorkflowAuditLog.builder()
                .contentId(contentId).brandId(brandId).eventType(eventType)
                .actorId(actorId).actorRole(actorRole).reason(reason)
                .timestamp(Instant.now()).build());
    }
}
