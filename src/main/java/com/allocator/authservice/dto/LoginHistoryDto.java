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
public class LoginHistoryDto {
    private UUID id;
    private String email;
    private String ipAddress;
    private String deviceInfo;
    private String location;
    private boolean success;
    private String failureReason;
    private LocalDateTime loginAt;
}
