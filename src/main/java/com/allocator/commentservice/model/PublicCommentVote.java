package com.allocator.commentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "public_comment_votes", indexes = {
    @Index(name = "idx_pcv_comment_id", columnList = "comment_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_pcv_comment_user", columnNames = {"comment_id", "user_id"})
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicCommentVote extends BaseEntity {

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** +1 (upvote) or -1 (downvote). */
    @Column(name = "value", nullable = false)
    private short value;
}
