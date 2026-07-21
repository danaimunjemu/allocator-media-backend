package com.allocator.commentservice.model;

public enum PublicCommentStatus {
    ACTIVE,
    /** Author deleted their own comment. */
    DELETED,
    /** An admin/super-admin removed it for a moderation reason — see
     *  {@link CommentRemovalReason}. Distinct from DELETED so the public UI
     *  can show a different notice and admins can restore it. */
    REMOVED
}
