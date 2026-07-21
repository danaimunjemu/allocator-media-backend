package com.allocator.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Minimal, privacy-conscious projection used by @mention autocomplete —
 *  deliberately excludes email, roles, and every other field on UserDto. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionUserDto {
    private UUID id;
    private String name;
    private String avatarUrl;
}
