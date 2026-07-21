package com.allocator.commentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank
    @Size(max = 5000)
    private String body;

    // Denormalized at write time from the caller's own authenticated profile —
    // cosmetic display fields only, never used for authorization (authorId,
    // taken from X-User-Id, is the only field that matters for ownership).
    private String authorName;
    private String authorAvatarUrl;
}
