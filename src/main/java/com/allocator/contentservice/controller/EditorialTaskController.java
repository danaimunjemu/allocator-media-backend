package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.EditorialTask;
import com.allocator.contentservice.model.TaskPriority;
import com.allocator.contentservice.model.TaskStatus;
import com.allocator.contentservice.service.EditorialTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EditorialTaskController {

    private final EditorialTaskService taskService;

    record CreateTaskRequest(String title, String description, UUID assigneeId,
                             Instant dueDate, TaskPriority priority,
                             UUID linkedCommentId, UUID linkedAnnotationId) {}
    record UpdateTaskRequest(String title, String description, UUID assigneeId,
                             Instant dueDate, TaskPriority priority, TaskStatus status) {}

    // ─── Content-scoped endpoints ─────────────────────────────────────────────

    @GetMapping("/api/v1/content/{contentId}/tasks")
    public ResponseEntity<ApiResponse<List<EditorialTask>>> getForContent(@PathVariable UUID contentId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getForContent(contentId)));
    }

    @PostMapping("/api/v1/content/{contentId}/tasks")
    public ResponseEntity<ApiResponse<EditorialTask>> create(
            @PathVariable UUID contentId,
            @RequestBody CreateTaskRequest req,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialTask saved = taskService.create(contentId, userId, req.title(),
                req.description(), req.assigneeId(), req.dueDate(), req.priority(),
                req.linkedCommentId(), req.linkedAnnotationId());
        return ResponseEntity.ok(ApiResponse.success(saved, "Task created"));
    }

    // ─── User-scoped endpoints ────────────────────────────────────────────────

    @GetMapping("/api/v1/tasks")
    public ResponseEntity<ApiResponse<List<EditorialTask>>> getForAssignee(
            @RequestParam Optional<TaskStatus> status,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getForAssignee(userId, status.orElse(null))));
    }

    @PutMapping("/api/v1/tasks/{taskId}")
    public ResponseEntity<ApiResponse<EditorialTask>> update(
            @PathVariable UUID taskId,
            @RequestBody UpdateTaskRequest req,
            @RequestHeader("X-User-Id") UUID userId) {
        EditorialTask updated = taskService.update(taskId, userId, req.title(), req.description(),
                req.assigneeId(), req.dueDate(), req.priority(), req.status());
        return ResponseEntity.ok(ApiResponse.success(updated, "Task updated"));
    }

    @PostMapping("/api/v1/tasks/{taskId}/complete")
    public ResponseEntity<ApiResponse<EditorialTask>> complete(
            @PathVariable UUID taskId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.complete(taskId, userId), "Task completed"));
    }

    @PostMapping("/api/v1/tasks/{taskId}/cancel")
    public ResponseEntity<ApiResponse<EditorialTask>> cancel(
            @PathVariable UUID taskId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.cancel(taskId, userId), "Task cancelled"));
    }
}
