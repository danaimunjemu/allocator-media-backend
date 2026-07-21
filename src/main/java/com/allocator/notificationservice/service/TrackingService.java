package com.allocator.notificationservice.service;

import java.util.UUID;

public interface TrackingService {
    void recordOpen(UUID notificationId);

    void recordClick(UUID notificationId);
}
