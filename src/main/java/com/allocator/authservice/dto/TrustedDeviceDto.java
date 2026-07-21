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
public class TrustedDeviceDto {
    private UUID id;
    private String deviceName;
    private String deviceType;
    private String os;
    private String deviceFingerprint;
    private LocalDateTime trustedAt;
    private LocalDateTime lastUsedAt;
}
