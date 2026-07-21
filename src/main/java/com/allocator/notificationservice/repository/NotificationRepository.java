package com.allocator.notificationservice.repository;

import com.allocator.notificationservice.model.Notification;
import com.allocator.notificationservice.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    long countByEmailAndStatus(String email, NotificationStatus status);
}
