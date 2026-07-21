package com.allocator.notificationservice.service;

import com.allocator.notificationservice.dto.SubscriptionRequest;
import com.allocator.notificationservice.mapper.SubscriptionMapper;
import com.allocator.notificationservice.model.Subscription;
import com.allocator.notificationservice.provider.MailchimpAudienceProvider;
import com.allocator.notificationservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository repository;
    private final SubscriptionMapper mapper;
    private final MailchimpAudienceProvider mailchimpAudienceProvider;

    @Override
    @Transactional
    public Subscription subscribe(SubscriptionRequest request) {
        log.info("Subscribing user {} to topic {}", request.getUserId(), request.getTopic());

        Subscription saved = repository.findByEmailAndTopic(request.getEmail(), request.getTopic())
                .map(existing -> {
                    existing.setEnabled(true);
                    existing.setConsentGiven(request.isConsentGiven());
                    if (request.getBrandId() != null) {
                        existing.setBrandId(request.getBrandId());
                    }
                    return repository.save(existing);
                })
                .orElseGet(() -> repository.save(mapper.toEntity(request)));

        syncToMailchimp(saved, request.getBrandId(), request.isConsentGiven());

        return saved;
    }

    @Override
    public List<Subscription> getSubscriptionsByUserId(String userId) {
        return repository.findAll().stream()
                .filter(s -> userId.equals(s.getUserId()))
                .toList();
    }

    @Override
    @Transactional
    public void unsubscribe(UUID id) {
        log.info("Unsubscribing ID {}", id);
        repository.findById(id).ifPresent(subscription -> {
            subscription.setEnabled(false);
            repository.save(subscription);

            // Archive in Mailchimp only if the user was previously synced
            if (subscription.getMailchimpMemberId() != null) {
                try {
                    mailchimpAudienceProvider.archiveMember(subscription.getEmail());
                } catch (Exception e) {
                    log.error("Mailchimp archive failed for {} — subscription still disabled in DB", subscription.getEmail(), e);
                }
            }
        });
    }

    /**
     * Attempts to sync the subscription to Mailchimp after a successful DB save.
     * Any Mailchimp failure is caught and logged — it must never surface to the caller.
     */
    private void syncToMailchimp(Subscription subscription, String brandId, boolean consentGiven) {
        try {
            mailchimpAudienceProvider.addMember(subscription.getEmail(), brandId, consentGiven)
                    .ifPresent(memberId -> {
                        subscription.setMailchimpMemberId(memberId);
                        repository.save(subscription);
                    });
        } catch (Exception e) {
            log.error("Mailchimp sync failed for {} — subscription remains active in DB", subscription.getEmail(), e);
        }
    }
}
