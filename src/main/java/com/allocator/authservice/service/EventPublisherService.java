package com.allocator.authservice.service;

import com.allocator.authservice.dto.event.RoleAssignedEvent;
import com.allocator.authservice.dto.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service("authEventPublisherService")
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_CREATED_TOPIC = "user.created";
    private static final String ROLE_ASSIGNED_TOPIC = "user.role.assigned";

    public CompletableFuture<Void> publishUserCreatedEvent(UUID userId, String email, java.util.List<String> roles, UUID brandId) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.Instant.now())
                .userId(userId)
                .email(email)
                .roles(roles)
                .brandId(brandId)
                .build();

        log.info("Publishing UserCreated event for userId: {}, email: {}", userId, email);
        
        return kafkaTemplate.send(USER_CREATED_TOPIC, userId.toString(), event)
                .thenRun(() -> log.info("Successfully published UserCreated event for userId: {}", userId))
                .exceptionally(throwable -> {
                    log.error("Failed to publish UserCreated event for userId: {}", userId, throwable);
                    return null;
                });
    }

    public CompletableFuture<Void> publishRoleAssignedEvent(UUID userId, String email, java.util.List<String> roles, UUID brandId) {
        RoleAssignedEvent event = RoleAssignedEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(java.time.Instant.now())
                .userId(userId)
                .email(email)
                .roles(roles)
                .brandId(brandId)
                .build();

        log.info("Publishing RoleAssigned event for userId: {}, email: {}", userId, email);
        
        return kafkaTemplate.send(ROLE_ASSIGNED_TOPIC, userId.toString(), event)
                .thenRun(() -> log.info("Successfully published RoleAssigned event for userId: {}", userId))
                .exceptionally(throwable -> {
                    log.error("Failed to publish RoleAssigned event for userId: {}", userId, throwable);
                    return null;
                });
    }
}

