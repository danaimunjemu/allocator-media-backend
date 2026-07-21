package com.allocator.contentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    private String bio;

    private String avatarUrl;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 100)
    private String role;

    private String contactInfo;

    private Map<String, String> socialLinks;

    // Optional: links the author to a platform user account (UUID as string)
    private String userId;
}
