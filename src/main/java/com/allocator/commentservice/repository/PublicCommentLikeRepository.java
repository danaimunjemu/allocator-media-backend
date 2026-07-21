package com.allocator.commentservice.repository;

import com.allocator.commentservice.model.PublicCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicCommentLikeRepository extends JpaRepository<PublicCommentLike, UUID> {

    Optional<PublicCommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    long countByCommentId(UUID commentId);

    @Query("SELECT l.commentId AS commentId, COUNT(l) AS total FROM PublicCommentLike l " +
           "WHERE l.commentId IN :commentIds GROUP BY l.commentId")
    List<CommentCount> countGroupedByCommentIds(@Param("commentIds") List<UUID> commentIds);

    @Query("SELECT l.commentId FROM PublicCommentLike l WHERE l.commentId IN :commentIds AND l.userId = :userId")
    List<UUID> findLikedCommentIds(@Param("commentIds") List<UUID> commentIds, @Param("userId") UUID userId);

    interface CommentCount {
        UUID getCommentId();
        long getTotal();
    }
}
