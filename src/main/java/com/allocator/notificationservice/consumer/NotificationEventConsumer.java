package com.allocator.notificationservice.consumer;

import com.allocator.common.event.ArticlePublishedEvent;
import com.allocator.common.event.EmailVerificationRequestedEvent;
import com.allocator.common.event.PasswordResetRequestedEvent;
import com.allocator.common.event.PaymentSucceededEvent;
import com.allocator.common.event.UserRegisteredEvent;
import com.allocator.common.event.UserSubscriptionCreatedEvent;
import com.allocator.notificationservice.model.Subscription;
import com.allocator.notificationservice.provider.EmailProvider;
import com.allocator.notificationservice.repository.SubscriptionRepository;
import com.allocator.notificationservice.service.EmailTemplates;
import com.allocator.notificationservice.service.TemplateService;
import com.allocator.notificationservice.repository.EmailTemplateRepository;
import com.allocator.notificationservice.repository.NotificationRepository;
import com.allocator.notificationservice.model.Notification;
import com.allocator.notificationservice.model.NotificationStatus;
import com.allocator.notificationservice.model.EmailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("notificationEventConsumer")
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final SubscriptionRepository subscriptionRepository;
    private final EmailProvider emailProvider;
    private final TemplateService templateService;
    private final EmailTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.public-api-url:http://localhost:8080}")
    private String publicApiUrl;

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleArticleEvents(ArticlePublishedEvent event) {
        log.info("Received ArticlePublishedEvent for article: {}", event.getArticleId());
        processArticlePublished(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleUserEvents(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent for user: {}", event.getUserId());
        processUserRegistered(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Received EmailVerificationRequestedEvent for user: {}", event.getUserId());
        processEmailVerificationRequested(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Received PasswordResetRequestedEvent for user: {}", event.getUserId());
        processPasswordResetRequested(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handleSubscriptionCreated(UserSubscriptionCreatedEvent event) {
        log.info("Received UserSubscriptionCreatedEvent for user: {}", event.getUserId());
        processSubscriptionCreated(event);
    }

    @org.springframework.context.event.EventListener
    @org.springframework.scheduling.annotation.Async
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received PaymentSucceededEvent for user: {}", event.getUserId());
        processPaymentSucceeded(event);
    }

    private void processArticlePublished(ArticlePublishedEvent event) {
        if (event.getTopics() == null || event.getTopics().isEmpty()) {
            log.warn("Article {} has no topics, skipping notifications", event.getArticleId());
            return;
        }

        for (String topic : event.getTopics()) {
            List<String> recipients = subscriptionRepository.findByTopicAndEnabledTrue(topic)
                    .stream()
                    .map(Subscription::getEmail)
                    .toList();

            if (!recipients.isEmpty()) {
                log.info("Notifying {} subscribers for topic: {}", recipients.size(), topic);
                sendNotifications(recipients, event, topic);
            }
        }
    }

    private void sendNotifications(List<String> recipients, ArticlePublishedEvent event, String topic) {
        String templateName = "article-published";
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", event.getTitle());
        variables.put("summary", event.getSummary());
        variables.put("topic", topic);
        variables.put("link", "https://african-allocator.com/articles/" + event.getArticleId());

        String subject = "New Article: " + event.getTitle();
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.articlePublished());

            for (String recipient : recipients) {
                // Create individual notification for tracking
                Notification notification = Notification.builder()
                        .email(recipient)
                        .subject(subject)
                        .status(NotificationStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

                notification = notificationRepository.save(notification);

                org.slf4j.MDC.put("notificationId", notification.getId().toString());
                org.slf4j.MDC.put("userId", event.getAuthorId() != null ? event.getAuthorId() : "system");

                try {
                    // Add tracking pixel to body
                    String body = templateService.render(bodyTemplate, variables);
                    body += "<img src='" + publicApiUrl + "/tracking/open/" + notification.getId()
                            + "' style='display:none;' />";

                    emailProvider.sendEmail(recipient, subject, body);

                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                } finally {
                    org.slf4j.MDC.clear();
                }
            }
        } catch (Exception e) {
            log.error("Failed to process notifications for topic {}: {}", topic, e.getMessage());
        }
    }

    private void processUserRegistered(UserRegisteredEvent event) {
        String templateName = "welcome-email";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getUsername());
        variables.put("link", frontendUrl);

        String subject = "Welcome to Allocator Media!";
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.welcome());

            Notification notification = Notification.builder()
                    .email(event.getEmail())
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            notification = notificationRepository.save(notification);

            org.slf4j.MDC.put("notificationId", notification.getId().toString());
            org.slf4j.MDC.put("userId", event.getUserId());

            try {
                String body = templateService.render(bodyTemplate, variables);
                body += "<img src='" + publicApiUrl + "/tracking/open/" + notification.getId()
                        + "' style='display:none;' />";

                emailProvider.sendEmail(event.getEmail(), subject, body);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
                
                log.info("Sent welcome email to User {}", event.getUserId());
            } finally {
                org.slf4j.MDC.clear();
            }
            
        } catch (Exception e) {
            log.error("Failed to process welcome email for user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private void processEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        String templateName = "verification-email";
        String link = frontendUrl + "/verify-email?token=" + event.getToken();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getFirstName() != null ? event.getFirstName() : "there");
        variables.put("link", link);

        String subject = "Verify your email address";
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.verification());

            Notification notification = Notification.builder()
                    .email(event.getEmail())
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            notification = notificationRepository.save(notification);

            org.slf4j.MDC.put("notificationId", notification.getId().toString());
            org.slf4j.MDC.put("userId", event.getUserId());

            try {
                String body = templateService.render(bodyTemplate, variables);
                emailProvider.sendEmail(event.getEmail(), subject, body);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("Sent verification email to User {}", event.getUserId());
            } finally {
                org.slf4j.MDC.clear();
            }

        } catch (Exception e) {
            log.error("Failed to process verification email for user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private void processSubscriptionCreated(UserSubscriptionCreatedEvent event) {
        String templateName = "subscription-confirmed";
        String planLabel = event.getPlanName() != null ? event.getPlanName() : event.getPlanTier();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getName() != null ? event.getName() : "there");
        variables.put("plan", planLabel);
        variables.put("renewalDate", event.getRenewalDate() != null && !event.getRenewalDate().isBlank() ? event.getRenewalDate() : "—");
        variables.put("manageUrl", frontendUrl + "/account/subscription");

        String subject = "You're subscribed — " + planLabel + " plan confirmed";
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.subscriptionConfirmed());

            Notification notification = Notification.builder()
                    .email(event.getEmail())
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            notification = notificationRepository.save(notification);

            org.slf4j.MDC.put("notificationId", notification.getId().toString());
            org.slf4j.MDC.put("userId", event.getUserId());

            try {
                String body = templateService.render(bodyTemplate, variables);
                emailProvider.sendEmail(event.getEmail(), subject, body);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("Sent subscription confirmation email to User {}", event.getUserId());
            } finally {
                org.slf4j.MDC.clear();
            }

        } catch (Exception e) {
            log.error("Failed to process subscription confirmation email for user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private void processPasswordResetRequested(PasswordResetRequestedEvent event) {
        String templateName = "password-reset-email";
        String link = frontendUrl + "/reset-password?token=" + event.getToken();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getFirstName() != null ? event.getFirstName() : "there");
        variables.put("link", link);

        String subject = "Reset your password";
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.passwordReset());

            Notification notification = Notification.builder()
                    .email(event.getEmail())
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            notification = notificationRepository.save(notification);

            org.slf4j.MDC.put("notificationId", notification.getId().toString());
            org.slf4j.MDC.put("userId", event.getUserId());

            try {
                String body = templateService.render(bodyTemplate, variables);
                emailProvider.sendEmail(event.getEmail(), subject, body);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("Sent password reset email to User {}", event.getUserId());
            } finally {
                org.slf4j.MDC.clear();
            }

        } catch (Exception e) {
            log.error("Failed to process password reset email for user {}: {}", event.getUserId(), e.getMessage());
        }
    }

    private void processPaymentSucceeded(PaymentSucceededEvent event) {
        String templateName = "payment-receipt";
        String planLabel = event.getPlanName() != null ? event.getPlanName() : event.getPlanTier();
        String amountLabel = event.getAmount() != null
                ? event.getAmount() + " " + (event.getCurrency() != null ? event.getCurrency().toUpperCase() : "")
                : "";

        Map<String, Object> variables = new HashMap<>();
        variables.put("name", event.getName() != null ? event.getName() : "there");
        variables.put("plan", planLabel);
        variables.put("invoiceNumber", event.getInvoiceNumber() != null ? event.getInvoiceNumber() : "—");
        variables.put("paymentDate", event.getPaidAt() != null && !event.getPaidAt().isBlank() ? event.getPaidAt() : "—");
        variables.put("paymentMethod", event.getPaymentMethodSummary() != null ? event.getPaymentMethodSummary() : "Card on file");
        variables.put("amount", amountLabel.trim());
        variables.put("invoiceUrl", event.getInvoiceUrl() != null ? event.getInvoiceUrl() : frontendUrl + "/account/subscription");

        String subject = "Your payment receipt — " + planLabel;
        String bodyTemplate;

        try {
            bodyTemplate = templateRepository.findByName(templateName)
                    .map(EmailTemplate::getBodyTemplate)
                    .orElse(EmailTemplates.paymentReceipt());

            Notification notification = Notification.builder()
                    .email(event.getEmail())
                    .subject(subject)
                    .status(NotificationStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            notification = notificationRepository.save(notification);

            org.slf4j.MDC.put("notificationId", notification.getId().toString());
            org.slf4j.MDC.put("userId", event.getUserId());

            try {
                String body = templateService.render(bodyTemplate, variables);
                emailProvider.sendEmail(event.getEmail(), subject, body);

                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("Sent payment receipt email to User {}", event.getUserId());
            } finally {
                org.slf4j.MDC.clear();
            }

        } catch (Exception e) {
            log.error("Failed to process payment receipt email for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}

