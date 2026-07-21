package com.allocator.notificationservice.service;

import com.allocator.notificationservice.dto.SubscriptionRequest;
import com.allocator.notificationservice.model.Subscription;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {
    Subscription subscribe(SubscriptionRequest request);

    List<Subscription> getSubscriptionsByUserId(String userId);

    void unsubscribe(UUID id);
}
