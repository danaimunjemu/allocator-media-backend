package com.allocator.commentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "public_comment_likes", indexes = {
    @Index(name = "idx_pcl_comment_id", columnList = "comment_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_pcl_comment_user", columnNames = {"comment_id", "user_id"})
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PublicCommentLike extends BaseEntity {

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
