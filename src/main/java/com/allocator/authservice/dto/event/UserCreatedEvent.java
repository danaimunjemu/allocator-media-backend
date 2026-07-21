package com.allocator.authservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent {
    private UUID eventId;
    private Instant timestamp;
    private UUID userId;
    private String email;
    private List<String> roles;
    private UUID brandId;
}
