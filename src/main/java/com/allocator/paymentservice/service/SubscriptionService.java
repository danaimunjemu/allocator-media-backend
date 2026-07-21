package com.allocator.paymentservice.service;

import com.allocator.paymentservice.dto.CheckoutRequest;
import com.allocator.paymentservice.dto.CheckoutResponse;
import com.allocator.paymentservice.dto.PortalResponse;
import com.allocator.paymentservice.dto.SubscriptionDto;
import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.PlanTier;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import com.allocator.paymentservice.repository.PlanRepository;
import com.allocator.paymentservice.repository.SubscriberRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriberRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final EventPublisherService eventPublisher;

    @Value("${stripe.urls.success:http://localhost:3001/account/subscription/success}")
    private String defaultSuccessUrl;

    @Value("${stripe.urls.cancel:http://localhost:3001/pricing}")
    private String defaultCancelUrl;

    @Value("${stripe.urls.portal-return:http://localhost:3001/account}")
    private String portalReturnUrl;

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscription(UUID userId) {
        Subscriber sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));
        return SubscriptionDto.from(sub);
    }

    @Transactional
    public CheckoutResponse createCheckoutSession(UUID userId, String email, CheckoutRequest req) throws StripeException {
        Plan plan = planRepository.findById(UUID.fromString(req.getPlanId()))
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + req.getPlanId()));

        if (plan.getStripePriceId() == null) {
            throw new IllegalArgumentException("Free-tier plans do not require a checkout session");
        }

        String successUrl = req.getSuccessUrl() != null ? req.getSuccessUrl() : defaultSuccessUrl;
        String cancelUrl = req.getCancelUrl() != null ? req.getCancelUrl() : defaultCancelUrl;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(email)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .putMetadata("userId", userId.toString())
                .putMetadata("planId", plan.getId().toString())
                .build();

        Session session = Session.create(params);
        log.info("Created Stripe Checkout session {} for userId: {}", session.getId(), userId);
        return new CheckoutResponse(session.getUrl(), session.getId());
    }

    @Transactional
    public PortalResponse createPortalSession(UUID userId) throws StripeException {
        Subscriber sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));

        if (sub.getStripeCustomerId() == null) {
            throw new IllegalArgumentException("Free-tier users do not have a billing portal");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(sub.getStripeCustomerId())
                        .setReturnUrl(portalReturnUrl)
                        .build();

        com.stripe.model.billingportal.Session portalSession =
                com.stripe.model.billingportal.Session.create(params);

        return new PortalResponse(portalSession.getUrl());
    }

    @Transactional
    public SubscriptionDto cancelSubscription(UUID userId) throws StripeException {
        Subscriber sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));

        if (sub.getStripeSubscriptionId() == null) {
            throw new IllegalArgumentException("Free-tier subscriptions cannot be cancelled via this endpoint");
        }

        com.stripe.model.Subscription stripeSubscription =
                com.stripe.model.Subscription.retrieve(sub.getStripeSubscriptionId());

        stripeSubscription.update(com.stripe.param.SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build());

        sub.setCancelAtPeriodEnd(true);
        subscriptionRepository.save(sub);

        log.info("Cancellation scheduled at period end for userId: {}", userId);
        return SubscriptionDto.from(sub);
    }

    /** Creates a FREE-tier subscription for a new user without any Stripe interaction. */
    @Transactional
    public Subscriber createFreeSubscription(UUID userId, String email, String firstName, String lastName) {
        Plan freePlan = planRepository.findByTierAndActiveTrue(PlanTier.FREE)
                .orElseThrow(() -> new IllegalStateException("FREE plan not configured in database"));

        Subscriber sub = new Subscriber();
        sub.setUserId(userId);
        sub.setEmail(email);
        sub.setFirstName(firstName);
        sub.setLastName(lastName);
        sub.setPlan(freePlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setAccountStatus(com.allocator.paymentservice.enums.AccountStatus.ACTIVE);
        sub.setCancelAtPeriodEnd(false);
        sub.setLastActiveAt(java.time.Instant.now());
        sub.setLocation("Unknown");

        Subscriber saved = subscriptionRepository.save(sub);
        eventPublisher.publishSubscriptionCreated(saved);
        return saved;
    }
}
