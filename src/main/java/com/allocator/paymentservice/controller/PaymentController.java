package com.allocator.paymentservice.controller;

import com.allocator.paymentservice.dto.*;
import com.allocator.paymentservice.service.InvoiceService;
import com.allocator.paymentservice.service.PlanService;
import com.allocator.paymentservice.service.SubscriptionService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final SubscriptionService subscriptionService;
    private final PlanService planService;
    private final InvoiceService invoiceService;

    /** Public — no auth required. */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> getPlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }

    /** Requires authenticated user (X-User-Id header injected by gateway). */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody CheckoutRequest req) throws StripeException {

        CheckoutResponse response = subscriptionService.createCheckoutSession(
                UUID.fromString(userId), email, req);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/portal")
    public ResponseEntity<PortalResponse> createPortal(
            @RequestHeader("X-User-Id") String userId) throws StripeException {

        PortalResponse response = subscriptionService.createPortalSession(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionDto> getSubscription(
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(subscriptionService.getSubscription(UUID.fromString(userId)));
    }

    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionDto> cancelSubscription(
            @RequestHeader("X-User-Id") String userId) throws StripeException {

        return ResponseEntity.ok(subscriptionService.cancelSubscription(UUID.fromString(userId)));
    }

    /** Returns the last 24 Stripe invoices for the authenticated user. */
    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceDto>> getInvoices(
            @RequestHeader("X-User-Id") String userId) throws StripeException {

        return ResponseEntity.ok(invoiceService.getInvoicesForUser(UUID.fromString(userId)));
    }
}
