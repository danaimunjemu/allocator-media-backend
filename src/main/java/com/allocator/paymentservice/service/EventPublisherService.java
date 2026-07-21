package com.allocator.paymentservice.service;

import com.allocator.common.event.PaymentFailedEvent;
import com.allocator.common.event.PaymentSucceededEvent;
import com.allocator.common.event.UserSubscriptionCancelledEvent;
import com.allocator.common.event.UserSubscriptionCreatedEvent;
import com.allocator.paymentservice.entity.Payment;
import com.allocator.paymentservice.entity.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service("paymentEventPublisherService")
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private static final String PAYMENT_TOPIC = "payment-events";
    private static final String SOURCE = "payment-service";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    // Emails (verification, password reset, welcome) run through Spring's
    // in-process event bus, not Kafka — Kafka isn't part of the local dev
    // stack and nothing consumes this topic, so subscription notifications
    // need to go through the same in-process path to actually be delivered.
    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishSubscriptionCreated(Subscriber sub) {
        String renewalDate = sub.getCurrentPeriodEnd() != null
                ? DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH).format(sub.getCurrentPeriodEnd().atZone(ZoneOffset.UTC))
                : "";

        UserSubscriptionCreatedEvent event = UserSubscriptionCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("USER_SUBSCRIPTION_CREATED")
                .timestamp(LocalDateTime.now())
                .sourceService(SOURCE)
                .userId(sub.getUserId().toString())
                .email(sub.getEmail())
                .name(sub.getName())
                .planTier(sub.getPlan().getTier().name())
                .planName(sub.getPlan().getName())
                .renewalDate(renewalDate)
                .subscriptionId(sub.getId().toString())
                .stripeSubscriptionId(sub.getStripeSubscriptionId())
                .build();

        applicationEventPublisher.publishEvent(event);

        kafkaTemplate.send(PAYMENT_TOPIC, sub.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SubscriberCreated for userId: {}", sub.getUserId(), ex);
                    } else {
                        log.info("Published SubscriberCreated for userId: {}", sub.getUserId());
                    }
                });
    }

    public void publishSubscriptionCancelled(Subscriber sub) {
        UserSubscriptionCancelledEvent event = UserSubscriptionCancelledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("USER_SUBSCRIPTION_CANCELLED")
                .timestamp(LocalDateTime.now())
                .sourceService(SOURCE)
                .userId(sub.getUserId().toString())
                .email(sub.getEmail())
                .planTier(sub.getPlan().getTier().name())
                .subscriptionId(sub.getId().toString())
                .stripeSubscriptionId(sub.getStripeSubscriptionId())
                .build();

        kafkaTemplate.send(PAYMENT_TOPIC, sub.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish SubscriberCancelled for userId: {}", sub.getUserId(), ex);
                    } else {
                        log.info("Published SubscriberCancelled for userId: {}", sub.getUserId());
                    }
                });
    }

    public void publishPaymentFailed(Subscriber sub, String stripeInvoiceId) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PAYMENT_FAILED")
                .timestamp(LocalDateTime.now())
                .sourceService(SOURCE)
                .userId(sub.getUserId().toString())
                .email(sub.getEmail())
                .subscriptionId(sub.getId().toString())
                .stripeInvoiceId(stripeInvoiceId)
                .planTier(sub.getPlan().getTier().name())
                .build();

        kafkaTemplate.send(PAYMENT_TOPIC, sub.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentFailed for userId: {}", sub.getUserId(), ex);
                    } else {
                        log.info("Published PaymentFailed for userId: {}", sub.getUserId());
                    }
                });
    }

    /** Fires the payment receipt email — same in-process path as the other transactional emails. */
    public void publishPaymentSucceeded(Subscriber sub, Payment payment, String invoiceNumber, String invoiceUrl,
                                         String paymentMethodSummary) {
        String paidAt = payment.getPaidAt() != null
                ? DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH).format(payment.getPaidAt().atZone(ZoneOffset.UTC))
                : "";

        PaymentSucceededEvent event = PaymentSucceededEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PAYMENT_SUCCEEDED")
                .timestamp(LocalDateTime.now())
                .sourceService(SOURCE)
                .userId(sub.getUserId().toString())
                .email(sub.getEmail())
                .name(sub.getName())
                .planTier(sub.getPlan().getTier().name())
                .planName(sub.getPlan().getName())
                .subscriptionId(sub.getId().toString())
                .stripeInvoiceId(payment.getStripeInvoiceId())
                .invoiceNumber(invoiceNumber)
                .amount(payment.getAmount().toPlainString())
                .currency(payment.getCurrency())
                .paidAt(paidAt)
                .invoiceUrl(invoiceUrl)
                .paymentMethodSummary(paymentMethodSummary)
                .build();

        applicationEventPublisher.publishEvent(event);

        kafkaTemplate.send(PAYMENT_TOPIC, sub.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish PaymentSucceeded for userId: {}", sub.getUserId(), ex);
                    } else {
                        log.info("Published PaymentSucceeded for userId: {}", sub.getUserId());
                    }
                });
    }
}

