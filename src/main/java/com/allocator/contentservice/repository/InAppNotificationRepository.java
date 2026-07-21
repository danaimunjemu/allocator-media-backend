package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    List<InAppNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<InAppNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE InAppNotification n SET n.read = true, n.readAt = :now WHERE n.userId = :userId AND n.read = false")
    void markAllReadForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
