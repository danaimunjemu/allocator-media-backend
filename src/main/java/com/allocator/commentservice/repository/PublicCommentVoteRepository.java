package com.allocator.commentservice.repository;

import com.allocator.commentservice.model.PublicCommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicCommentVoteRepository extends JpaRepository<PublicCommentVote, UUID> {

    Optional<PublicCommentVote> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("SELECT v.commentId AS commentId, COALESCE(SUM(v.value), 0) AS total FROM PublicCommentVote v " +
           "WHERE v.commentId IN :commentIds GROUP BY v.commentId")
    List<CommentScore> sumScoresGroupedByCommentIds(@Param("commentIds") List<UUID> commentIds);

    List<PublicCommentVote> findByCommentIdInAndUserId(List<UUID> commentIds, UUID userId);

    interface CommentScore {
        UUID getCommentId();
        long getTotal();
    }
}
