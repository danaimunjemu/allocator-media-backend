package com.allocator.notificationservice.controller;

import com.allocator.notificationservice.dto.SubscriptionRequest;
import com.allocator.notificationservice.model.Subscription;
import com.allocator.notificationservice.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subscription subscribe(@Valid @RequestBody SubscriptionRequest request) {
        return service.subscribe(request);
    }

    @GetMapping("/{userId}")
    public List<Subscription> getSubscriptions(@PathVariable String userId) {
        return service.getSubscriptionsByUserId(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable UUID id) {
        service.unsubscribe(id);
    }
}
