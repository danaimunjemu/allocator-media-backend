package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private UUID id;
    private String deviceName;
    private String deviceType;
    private String os;
    private String browser;
    private String ipAddress;
    private String location;
    private LocalDateTime lastActive;
    private LocalDateTime createdAt;
    private boolean active;
    private boolean current;
}
