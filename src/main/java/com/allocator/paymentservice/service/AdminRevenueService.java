package com.allocator.paymentservice.service;

import com.allocator.authservice.repository.UserRepository;
import com.allocator.paymentservice.dto.RevenueReportDto;
import com.allocator.paymentservice.dto.SubscriptionDto;
import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.enums.AccountStatus;
import com.allocator.paymentservice.enums.PlanTier;
import com.allocator.paymentservice.enums.SubscriptionStatus;
import com.allocator.paymentservice.repository.PaymentRepository;
import com.allocator.paymentservice.repository.PlanRepository;
import com.allocator.paymentservice.repository.SubscriberRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRevenueService {

    private final SubscriberRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionDto> getAllSubscribers() {
        return subscriptionRepository.findAll()
                .stream()
                .map(SubscriptionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getSubscriberById(UUID id) {
        return subscriptionRepository.findById(id)
                .map(SubscriptionDto::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Subscriber not found: " + id));
    }

    @Transactional
    public void cancelSubscriptionByCustomerId(String stripeCustomerId, String reason) throws StripeException {
        Subscriber sub = findByCustomerIdOrThrow(stripeCustomerId);

        if (sub.getStripeSubscriptionId() != null) {
            Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
            stripeSub.update(SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build());
            sub.setCancelAtPeriodEnd(true);
            log.info("Cancellation scheduled at period end (admin) for customer: {}", stripeCustomerId);
        } else {
            // FREE tier — nothing to cancel in Stripe.
            sub.setStatus(SubscriptionStatus.CANCELLED);
            log.info("FREE-tier subscription cancelled immediately (admin) for customer: {}", stripeCustomerId);
        }

        sub.setCancellationReason(reason);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void reactivateSubscriptionByCustomerId(String stripeCustomerId) throws StripeException {
        Subscriber sub = findByCustomerIdOrThrow(stripeCustomerId);

        if (!Boolean.TRUE.equals(sub.getCancelAtPeriodEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Subscription for customer " + stripeCustomerId + " is not pending cancellation");
        }

        if (sub.getStripeSubscriptionId() != null) {
            Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
            stripeSub.update(SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .build());
        }

        sub.setCancelAtPeriodEnd(false);
        sub.setCancellationReason(null);
        subscriptionRepository.save(sub);
        log.info("Subscription reactivated (admin) for customer: {}", stripeCustomerId);
    }

    /**
     * Moves a subscriber onto a different plan. Paid→paid swaps the Stripe subscription item with
     * proration; paid→FREE cancels the Stripe subscription immediately; FREE→paid is rejected since
     * it requires a real checkout session (no payment method on file to charge).
     */
    @Transactional
    public SubscriptionDto changeSubscriberPlan(UUID subscriptionId, UUID newPlanId) throws StripeException {
        Subscriber sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscriber not found: " + subscriptionId));
        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plan not found: " + newPlanId));

        boolean currentIsPaid = sub.getStripeSubscriptionId() != null;
        boolean newIsPaid = newPlan.getStripePriceId() != null;

        if (!currentIsPaid && newIsPaid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot move a free-tier subscriber directly onto a paid plan without a checkout " +
                            "session — have them upgrade via the billing portal.");
        }

        if (currentIsPaid && !newIsPaid) {
            Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
            stripeSub.cancel();
            sub.setStripeSubscriptionId(null);
            sub.setStatus(SubscriptionStatus.ACTIVE);
            sub.setCancelAtPeriodEnd(false);
            sub.setCancellationReason(null);
        } else if (currentIsPaid) {
            Subscription stripeSub = Subscription.retrieve(sub.getStripeSubscriptionId());
            String itemId = stripeSub.getItems().getData().get(0).getId();
            stripeSub.update(SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(itemId)
                            .setPrice(newPlan.getStripePriceId())
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .build());
        }

        sub.setPlan(newPlan);
        Subscriber saved = subscriptionRepository.save(sub);
        log.info("Subscriber {} moved to plan {}", subscriptionId, newPlanId);
        return SubscriptionDto.from(saved);
    }

    private Subscriber findByCustomerIdOrThrow(String stripeCustomerId) {
        return subscriptionRepository.findByStripeCustomerId(stripeCustomerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscription not found for customer: " + stripeCustomerId));
    }

    /**
     * Toggles a subscriber's account-level access (independent of their billing
     * status). Also flips the underlying {@code users.enabled} flag so a
     * deactivated subscriber can no longer log in.
     */
    @Transactional
    public SubscriptionDto setSubscriberAccountStatus(UUID subscriberId, boolean active) {
        Subscriber sub = subscriptionRepository.findById(subscriberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Subscriber not found: " + subscriberId));

        sub.setAccountStatus(active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
        Subscriber saved = subscriptionRepository.save(sub);

        userRepository.findById(sub.getUserId()).ifPresent(user -> {
            user.setEnabled(active);
            userRepository.save(user);
        });

        log.info("Subscriber {} account {} by admin", subscriberId, active ? "activated" : "deactivated");
        return SubscriptionDto.from(saved);
    }

    @Transactional(readOnly = true)
    public RevenueReportDto getRevenueReport() {
        long activeCount = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        // MRR = sum of monthly plan amounts + (annual plan amounts / 12)
        BigDecimal monthlyRevenue = subscriptionRepository.sumAmountByInterval("month");
        BigDecimal annualRevenue = subscriptionRepository.sumAmountByInterval("year");
        BigDecimal mrr = monthlyRevenue.add(
                annualRevenue.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
        );
        BigDecimal arr = mrr.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);

        // Churn: subscriptions cancelled in last 30 days / active count at period start
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long cancelledLast30 = subscriptionRepository.countByStatusAndCreatedAtAfter(
                SubscriptionStatus.CANCELLED, thirtyDaysAgo);
        double churnRate = activeCount > 0
                ? (double) cancelledLast30 / (activeCount + cancelledLast30)
                : 0.0;

        // Subscribers by tier
        Map<PlanTier, Long> byTier = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(s -> s.getPlan().getTier(), Collectors.counting()));

        BigDecimal revenueLast30Days = paymentRepository.sumSucceededAmountSince(thirtyDaysAgo);

        return RevenueReportDto.builder()
                .totalActiveSubscribers(activeCount)
                .monthlyRecurringRevenue(mrr)
                .annualRecurringRevenue(arr)
                .churnRate(churnRate)
                .subscribersByTier(byTier)
                .totalRevenueLast30Days(revenueLast30Days)
                .build();
    }
}
