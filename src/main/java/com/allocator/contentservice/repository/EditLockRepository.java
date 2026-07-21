package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.EditLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EditLockRepository extends JpaRepository<EditLock, UUID> {

    Optional<EditLock> findByContentId(UUID contentId);

    @Transactional
    void deleteByContentId(UUID contentId);

    // Atomic upsert — a plain "find, then insert-if-absent" is a check-then-act
    // race: two concurrent acquire calls for the same content can both see no
    // row under READ COMMITTED and both attempt an insert, tripping the unique
    // constraint on content_id. ON CONFLICT lets Postgres serialize this at the
    // row level instead. The WHERE clause makes it a no-op (0 rows affected)
    // when a *different* user still holds a live lock, which the caller reads
    // as a conflict.
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO edit_locks (id, content_id, user_id, user_name, user_role, acquired_at, heartbeat_at, expires_at)
        VALUES (:id, :contentId, :userId, :userName, :userRole, :now, :now, :expiresAt)
        ON CONFLICT (content_id) DO UPDATE SET
            user_id = EXCLUDED.user_id,
            user_name = EXCLUDED.user_name,
            user_role = EXCLUDED.user_role,
            acquired_at = EXCLUDED.acquired_at,
            heartbeat_at = EXCLUDED.heartbeat_at,
            expires_at = EXCLUDED.expires_at
        WHERE edit_locks.expires_at < :now OR edit_locks.user_id = :userId
        """, nativeQuery = true)
    int upsertLock(@Param("id") UUID id, @Param("contentId") UUID contentId, @Param("userId") UUID userId,
                    @Param("userName") String userName, @Param("userRole") String userRole,
                    @Param("now") Instant now, @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Transactional
    @Query("DELETE FROM EditLock l WHERE l.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") Instant now);
}
