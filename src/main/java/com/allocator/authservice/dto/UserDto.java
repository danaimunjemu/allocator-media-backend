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
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private java.util.List<String> roles;
    private String status;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private String contactInfo;
    private LocalDateTime createdAt;
    private String lastLogin;

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserBrandRoleDto {
        private String brandCode;
        private String brandName;
        private java.util.List<String> roles;
    }

    private java.util.List<UserBrandRoleDto> brandRoleMappings;
}
