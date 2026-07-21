package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.BrokerRequest;
import com.allocator.contentservice.dto.BrokerResponse;
import com.allocator.contentservice.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController("contentBrokerController")
@RequestMapping("/api/v1/content-brokers")
@RequiredArgsConstructor
@Slf4j
public class BrokerController {

    private final BrokerService brokerService;

    @GetMapping
    public ResponseEntity<List<BrokerResponse>> getAllBrokers() {
        log.info("Fetching all brokers");
        return ResponseEntity.ok(brokerService.getAllBrokers());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BrokerResponse> getBrokerBySlug(@PathVariable String slug) {
        log.info("Fetching broker by slug: {}", slug);
        return ResponseEntity.ok(brokerService.getBrokerBySlug(slug));
    }

    @PostMapping
    public ResponseEntity<BrokerResponse> createBroker(@RequestBody BrokerRequest request) {
        log.info("Creating new broker: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(brokerService.createBroker(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrokerResponse> updateBroker(@PathVariable UUID id, @RequestBody BrokerRequest request) {
        log.info("Updating broker with id: {}", id);
        return ResponseEntity.ok(brokerService.updateBroker(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBroker(@PathVariable UUID id) {
        log.info("Deleting broker with id: {}", id);
        brokerService.deleteBroker(id);
        return ResponseEntity.noContent().build();
    }
}
