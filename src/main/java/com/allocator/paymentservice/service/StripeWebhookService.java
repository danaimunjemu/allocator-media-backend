package com.allocator.paymentservice.service;

import com.allocator.paymentservice.entity.Payment;
import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.PaymentStatus;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import com.allocator.paymentservice.repository.PaymentRepository;
import com.allocator.paymentservice.repository.PlanRepository;
import com.allocator.paymentservice.repository.SubscriberRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentRetrieveParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final SubscriberRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final EventPublisherService eventPublisher;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Validates the Stripe signature and dispatches to the correct handler.
     * Returns false if the signature is invalid (caller should respond 400).
     */
    public boolean handleWebhook(byte[] payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(new String(payload), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return false;
        }

        log.info("Processing Stripe event: {} [{}]", event.getType(), event.getId());

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
                case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error processing Stripe event {} [{}]", event.getType(), event.getId(), e);
            // Return true — we've acknowledged receipt; retrying won't fix a code bug
        }

        return true;
    }

    @Transactional
    protected void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElseThrow(() -> new IllegalStateException("Could not deserialize checkout.session"));

        String stripeSubscriptionId = session.getSubscription();
        if (stripeSubscriptionId == null) return; // one-time payment, skip

        // Idempotency: skip if already recorded
        if (subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).isPresent()) {
            log.info("Subscription {} already exists — skipping duplicate checkout.session.completed", stripeSubscriptionId);
            return;
        }

        String userIdStr = session.getMetadata().get("userId");
        String planIdStr = session.getMetadata().get("planId");

        UUID userId = UUID.fromString(userIdStr);
        Plan plan = planRepository.findById(UUID.fromString(planIdStr))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planIdStr));

        // Retrieve full subscription for period dates
        com.stripe.model.Subscription stripeSub;
        try {
            stripeSub = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve Stripe subscription: " + stripeSubscriptionId, e);
        }

        // Upsert: user may already have a FREE subscription row
        Subscriber sub = subscriptionRepository.findByUserId(userId)
                .orElse(new Subscriber());

        sub.setUserId(userId);
        sub.setEmail(session.getCustomerEmail());
        sub.setPlan(plan);
        sub.setStripeCustomerId(session.getCustomer());
        sub.setStripeSubscriptionId(stripeSubscriptionId);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        SubscriptionItem item = stripeSub.getItems().getData().get(0);
        sub.setCurrentPeriodStart(Instant.ofEpochSecond(item.getCurrentPeriodStart()));
        sub.setCurrentPeriodEnd(Instant.ofEpochSecond(item.getCurrentPeriodEnd()));
        sub.setCancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd());

        Subscriber saved = subscriptionRepository.save(sub);
        eventPublisher.publishSubscriptionCreated(saved);

        log.info("Subscription created for userId: {}, plan: {}", userId, plan.getTier());
    }

    @Transactional
    protected void handleSubscriptionUpdated(Event event) {
        com.stripe.model.Subscription stripeSub = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer().getObject()
                        .orElseThrow(() -> new IllegalStateException("Could not deserialize customer.subscription"));

        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).ifPresentOrElse(sub -> {
            SubscriptionItem item = stripeSub.getItems().getData().get(0);
            sub.setCurrentPeriodStart(Instant.ofEpochSecond(item.getCurrentPeriodStart()));
            sub.setCurrentPeriodEnd(Instant.ofEpochSecond(item.getCurrentPeriodEnd()));
            sub.setCancelAtPeriodEnd(stripeSub.getCancelAtPeriodEnd());

            // Reflect any status change (e.g. trialing → active)
            SubscriptionStatus status = mapStripeStatus(stripeSub.getStatus());
            sub.setStatus(status);

            subscriptionRepository.save(sub);
            log.info("Subscription {} updated", stripeSub.getId());
        }, () -> log.warn("Received subscription.updated for unknown stripe subscription: {}", stripeSub.getId()));
    }

    @Transactional
    protected void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSub = (com.stripe.model.Subscription)
                event.getDataObjectDeserializer().getObject()
                        .orElseThrow(() -> new IllegalStateException("Could not deserialize customer.subscription"));

        subscriptionRepository.findByStripeSubscriptionId(stripeSub.getId()).ifPresentOrElse(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            Subscriber saved = subscriptionRepository.save(sub);
            eventPublisher.publishSubscriptionCancelled(saved);
            log.info("Subscription {} cancelled for userId: {}", stripeSub.getId(), sub.getUserId());
        }, () -> log.warn("Received subscription.deleted for unknown stripe subscription: {}", stripeSub.getId()));
    }

    @Transactional
    protected void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow(() -> new IllegalStateException("Could not deserialize invoice"));

        // Idempotency: skip if already recorded
        if (paymentRepository.findByStripeInvoiceId(invoice.getId()).isPresent()) {
            log.info("Payment for invoice {} already recorded — skipping", invoice.getId());
            return;
        }

        Subscriber sub = subscriptionRepository
                .findByStripeSubscriptionId(invoiceSubscriptionId(invoice))
                .orElse(null);

        if (sub == null) {
            log.warn("Invoice {} has no matching subscription in DB", invoice.getId());
            return;
        }

        Payment payment = new Payment();
        payment.setSubscriptionId(sub.getId());
        payment.setStripeInvoiceId(invoice.getId());
        payment.setStripePaymentIntentId(invoicePaymentIntentId(invoice));
        payment.setAmount(BigDecimal.valueOf(invoice.getAmountPaid()).movePointLeft(2)); // cents → major
        payment.setCurrency(invoice.getCurrency().toUpperCase());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAt(Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt()));

        Payment saved = paymentRepository.save(payment);

        String invoiceNumber = invoice.getNumber() != null ? invoice.getNumber() : saved.getId().toString();
        String invoiceUrl = invoice.getHostedInvoiceUrl();
        String paymentMethodSummary = resolvePaymentMethodSummary(invoice);

        eventPublisher.publishPaymentSucceeded(sub, saved, invoiceNumber, invoiceUrl, paymentMethodSummary);

        log.info("Recorded successful payment for invoice: {}", invoice.getId());
    }

    /**
     * Best-effort card brand/last4 for the receipt email — an extra Stripe round trip via
     * the invoice's payment intent, so any failure here must never block recording the
     * payment itself. Falls back to null (row omitted from the email) rather than a
     * fabricated placeholder, same "real data or nothing" rule the newsletter renderer
     * follows for its weather snippet.
     */
    private String resolvePaymentMethodSummary(Invoice invoice) {
        try {
            String paymentIntentId = invoicePaymentIntentId(invoice);
            if (paymentIntentId == null) return null;

            PaymentIntentRetrieveParams params = PaymentIntentRetrieveParams.builder()
                    .addExpand("payment_method")
                    .build();
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, params, null);
            PaymentMethod pm = intent.getPaymentMethodObject();
            if (pm == null || pm.getCard() == null) return null;

            String brand = pm.getCard().getBrand();
            String last4 = pm.getCard().getLast4();
            if (brand == null || last4 == null) return null;

            return Character.toUpperCase(brand.charAt(0)) + brand.substring(1) + " &bull;&bull;&bull;&bull; " + last4;
        } catch (Exception e) {
            log.debug("Could not resolve payment method for invoice {}: {}", invoice.getId(), e.getMessage());
            return null;
        }
    }

    @Transactional
    protected void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow(() -> new IllegalStateException("Could not deserialize invoice"));

        String stripeSubscriptionId = invoiceSubscriptionId(invoice);
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresentOrElse(sub -> {
                    sub.setStatus(SubscriptionStatus.PAST_DUE);
                    Subscriber saved = subscriptionRepository.save(sub);
                    eventPublisher.publishPaymentFailed(saved, invoice.getId());
                    log.warn("Payment failed for subscription: {}, userId: {}", stripeSubscriptionId, sub.getUserId());
                }, () -> log.warn("invoice.payment_failed for unknown subscription: {}", stripeSubscriptionId));
    }

    /** Invoice's subscription link moved under parent.subscription_details in newer API versions. */
    private String invoiceSubscriptionId(Invoice invoice) {
        Invoice.Parent parent = invoice.getParent();
        if (parent == null || parent.getSubscriptionDetails() == null) return null;
        return parent.getSubscriptionDetails().getSubscription();
    }

    /** Invoice's payment_intent field was replaced by the payments list (an invoice can have multiple payment attempts). */
    private String invoicePaymentIntentId(Invoice invoice) {
        if (invoice.getPayments() == null || invoice.getPayments().getData().isEmpty()) return null;
        return invoice.getPayments().getData().get(0).getPayment().getPaymentIntent();
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "trialing" -> SubscriptionStatus.TRIALING;
            default -> SubscriptionStatus.ACTIVE;
        };
    }
}
