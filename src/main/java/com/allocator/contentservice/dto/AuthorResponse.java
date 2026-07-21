package com.allocator.contentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String bio;
    private String avatarUrl;
    private String email;
    private String role;
    private String contactInfo;
    private Map<String, String> socialLinks;
    private Instant createdAt;
    private Instant updatedAt;
}
