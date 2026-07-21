package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.InAppNotification;
import com.allocator.contentservice.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications/inbox")
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationController {

    private final InAppNotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InAppNotification>>> getForUser(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getForUser(userId, unreadOnly)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<InAppNotification>> markRead(
            @PathVariable UUID notificationId,
            @RequestHeader("X-User-Id") UUID userId) {
        InAppNotification updated = notificationService.markRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success(updated, "Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
