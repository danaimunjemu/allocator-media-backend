package com.allocator.brokerservice.controller;

import com.allocator.brokerservice.dto.BrokerRequest;
import com.allocator.brokerservice.dto.BrokerResponse;
import com.allocator.brokerservice.service.BrokerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController("brokerBrokerController")
@RequestMapping("/api/v1/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    /** Public — list active brokers with optional filters. */
    @GetMapping
    public ResponseEntity<List<BrokerResponse>> list(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String licenseType,
            @RequestParam(required = false) BigDecimal minInvestment,
            @RequestParam(required = false) String tradingPlatform) {

        return ResponseEntity.ok(brokerService.findActive(brandId, country, licenseType, minInvestment, tradingPlatform));
    }

    /** Public — get a single active broker by id. */
    @GetMapping("/{id}")
    public ResponseEntity<BrokerResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(brokerService.findActiveById(id));
    }

    /** Admin — create broker. */
    @PostMapping
    public ResponseEntity<BrokerResponse> create(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles,
            @Valid @RequestBody BrokerRequest req) {

        requireAdmin(roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(brokerService.create(req));
    }

    /** Admin — update broker. */
    @PutMapping("/{id}")
    public ResponseEntity<BrokerResponse> update(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles,
            @PathVariable UUID id,
            @Valid @RequestBody BrokerRequest req) {

        requireAdmin(roles);
        return ResponseEntity.ok(brokerService.update(id, req));
    }

    /** Admin — soft delete (sets active = false). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles,
            @PathVariable UUID id) {

        requireAdmin(roles);
        brokerService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(String rolesHeader) {
        boolean isAdmin = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN") || r.equalsIgnoreCase("ADMIN")
                            || r.equalsIgnoreCase("SUPER_ADMIN") || r.equalsIgnoreCase("ROLE_SUPER_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
