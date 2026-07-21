package com.allocator.paymentservice.controller;

import com.allocator.paymentservice.service.StripeWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Stripe webhook receiver.
 *
 * This endpoint MUST bypass JWT auth — it is listed in the gateway's PUBLIC_PATHS
 * and must also be excluded from any service-level security filters.
 * Authenticity is verified via Stripe-Signature HMAC, not bearer tokens.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final StripeWebhookService webhookService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) throws IOException {

        if (sigHeader == null) {
            log.warn("Stripe webhook received without Stripe-Signature header");
            return ResponseEntity.badRequest().body("Missing Stripe-Signature");
        }

        // Read raw bytes — MUST happen before any body parsing to preserve HMAC validity
        byte[] payload = request.getInputStream().readAllBytes();

        boolean valid = webhookService.handleWebhook(payload, sigHeader);
        if (!valid) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        return ResponseEntity.ok("Received");
    }
}
