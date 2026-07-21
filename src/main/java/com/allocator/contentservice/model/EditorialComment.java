package com.allocator.contentservice.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "editorial_comments", indexes = {
    @Index(name = "idx_ec_content_id",       columnList = "content_id"),
    @Index(name = "idx_ec_brand_id",          columnList = "brand_id"),
    @Index(name = "idx_ec_parent_comment_id", columnList = "parent_comment_id"),
    @Index(name = "idx_ec_author_id",         columnList = "author_id"),
    @Index(name = "idx_ec_content_status",    columnList = "content_id, status")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EditorialComment extends BaseEntity {

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Column(name = "brand_id", nullable = false)
    private UUID brandId;

    @Column(name = "parent_comment_id")
    private UUID parentCommentId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    @Column(name = "author_role", length = 50)
    private String authorRole;

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CommentStatus status = CommentStatus.OPEN;

    @Column(name = "anchor_start")
    private Integer anchorStart;

    @Column(name = "anchor_end")
    private Integer anchorEnd;

    @Column(name = "anchor_text", length = 1000)
    private String anchorText;

    @Column(name = "content_version_number")
    private Integer contentVersionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "anchor_status", length = 20)
    @Builder.Default
    private AnchorStatus anchorStatus = AnchorStatus.VALID;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
