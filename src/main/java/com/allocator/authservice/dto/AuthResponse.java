package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String role;
    private List<String> roles;
    private boolean mustChangePassword;
    @Builder.Default
    private boolean mfaRequired = false;
    private String mfaPendingToken;
    private String mfaMethod;
    @Builder.Default
    private boolean emailVerified = true;
}
