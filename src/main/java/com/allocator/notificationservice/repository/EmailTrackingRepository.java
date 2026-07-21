package com.allocator.notificationservice.repository;

import com.allocator.notificationservice.model.EmailTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTrackingRepository extends JpaRepository<EmailTracking, UUID> {
    Optional<EmailTracking> findByNotificationId(UUID notificationId);

    long countByOpenedTrue();

    long countByClickedTrue();
}
