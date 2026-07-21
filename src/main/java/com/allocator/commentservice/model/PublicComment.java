package com.allocator.commentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "public_comments", indexes = {
    @Index(name = "idx_pc_content_id",       columnList = "content_id"),
    @Index(name = "idx_pc_brand_id",         columnList = "brand_id"),
    @Index(name = "idx_pc_root_comment_id",  columnList = "root_comment_id"),
    @Index(name = "idx_pc_author_id",        columnList = "author_id"),
    @Index(name = "idx_pc_content_status",   columnList = "content_id, status")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicComment extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    /** Null for top-level comments; for replies, the top-level ancestor's id
     *  (a reply-to-a-reply still points here, flattening the thread to one
     *  visual level — {@link #parentCommentId} keeps the immediate parent). */
    @Column(name = "root_comment_id")
    private UUID rootCommentId;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    @Column(name = "author_avatar_url", length = 1000)
    private String authorAvatarUrl;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PublicCommentStatus status = PublicCommentStatus.ACTIVE;

    @Column(name = "removed_by")
    private UUID removedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "removal_reason", length = 50)
    private CommentRemovalReason removalReason;

    /** Set only by updateComment() — the sole source of truth for "was this
     *  edited", since BaseEntity.updatedAt also changes on remove/restore. */
    @Column(name = "edited_at")
    private Instant editedAt;
}
