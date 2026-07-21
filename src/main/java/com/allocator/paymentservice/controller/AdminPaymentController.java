package com.allocator.paymentservice.controller;

import com.allocator.paymentservice.dto.AdminPlanDto;
import com.allocator.paymentservice.dto.ChangePlanRequest;
import com.allocator.paymentservice.dto.CreatePlanRequest;
import com.allocator.paymentservice.dto.RevenueReportDto;
import com.allocator.paymentservice.dto.SubscriptionDto;
import com.allocator.paymentservice.dto.UpdatePlanRequest;
import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.repository.PlanRepository;
import com.allocator.paymentservice.service.AdminRevenueService;
import com.allocator.paymentservice.service.PlanService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/admin")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final AdminRevenueService revenueService;
    private final PlanRepository planRepository;
    private final PlanService planService;

    @GetMapping("/subscribers")
    public ResponseEntity<List<SubscriptionDto>> getAllSubscribers(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        return ResponseEntity.ok(revenueService.getAllSubscribers());
    }

    @GetMapping("/subscribers/{id}")
    public ResponseEntity<SubscriptionDto> getSubscriber(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        return ResponseEntity.ok(revenueService.getSubscriberById(id));
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueReportDto> getRevenueReport(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        return ResponseEntity.ok(revenueService.getRevenueReport());
    }

    @GetMapping("/plans")
    public ResponseEntity<List<AdminPlanDto>> getAllPlans(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return ResponseEntity.ok(planService.getAllPlansForAdmin());
    }

    @PostMapping("/plans")
    public ResponseEntity<AdminPlanDto> createPlan(
            @RequestBody CreatePlanRequest request,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) throws StripeException {
        requireAdmin(roles);
        AdminPlanDto created = planService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<AdminPlanDto> updatePlan(
            @PathVariable UUID id,
            @RequestBody UpdatePlanRequest request,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) throws StripeException {
        requireAdmin(roles);
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }

    @PatchMapping("/plans/{id}")
    public ResponseEntity<Void> togglePlan(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        Boolean active = body.get("active");
        if (active == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing 'active' field in request body");
        }
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Plan not found: " + id));
        plan.setActive(active);
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSubscription(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) throws StripeException {

        requireAdmin(roles);
        String customerId = body.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing 'customerId' in request body");
        }
        revenueService.cancelSubscriptionByCustomerId(customerId, body.get("reason"));
    }

    @PostMapping("/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateSubscription(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) throws StripeException {

        requireAdmin(roles);
        String customerId = body.get("customerId");
        if (customerId == null || customerId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing 'customerId' in request body");
        }
        revenueService.reactivateSubscriptionByCustomerId(customerId);
    }

    @PostMapping("/subscribers/{id}/change-plan")
    public ResponseEntity<SubscriptionDto> changeSubscriberPlan(
            @PathVariable UUID id,
            @RequestBody ChangePlanRequest request,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) throws StripeException {

        requireAdmin(roles);
        if (request.getNewPlanId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing 'newPlanId' in request body");
        }
        return ResponseEntity.ok(revenueService.changeSubscriberPlan(id, request.getNewPlanId()));
    }

    @PatchMapping("/subscribers/{id}/status")
    public ResponseEntity<SubscriptionDto> setSubscriberStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        Boolean active = body.get("active");
        if (active == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Missing 'active' field in request body");
        }
        return ResponseEntity.ok(revenueService.setSubscriberAccountStatus(id, active));
    }

    private void requireAdmin(String rolesHeader) {
        boolean isAdmin = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN") || r.equalsIgnoreCase("ADMIN")
                        || r.equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.equalsIgnoreCase("SUPER_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
