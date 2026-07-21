package com.allocator.commentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private UUID contentId;
    private UUID rootCommentId;
    private UUID parentCommentId;

    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;

    private String body;
    private boolean deleted;

    private boolean removed;
    /** Human-readable label (e.g. "Hateful conduct") — null unless removed. */
    private String removalReason;
    private Instant removedAt;

    private Instant createdAt;
    /** Null unless the body was actually edited after creation. */
    private Instant editedAt;

    private long likeCount;
    private boolean likedByMe;

    private long score;
    /** +1, -1, or null if the viewer hasn't voted (or isn't authenticated). */
    private Short myVote;

    private long replyCount;
}
