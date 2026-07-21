package com.allocator.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Fires approved newsletters once their scheduledAt time arrives — mirrors
// contentservice's ScheduledContentService (same fixedRate + tryLock pattern).
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignSchedulerService {

    private final CampaignService campaignService;
    private final com.allocator.notificationservice.repository.CampaignRepository campaignRepository;
    private final Lock schedulerLock = new ReentrantLock();

    @Scheduled(fixedRate = 60000) // Run every minute
    public void sendDueNewsletters() {
        if (!schedulerLock.tryLock()) {
            log.debug("Newsletter scheduler lock already held — skipping");
            return;
        }
        try {
            var due = campaignRepository.findByStatusAndScheduledAtBefore(
                    com.allocator.notificationservice.model.CampaignStatus.SCHEDULED, LocalDateTime.now());
            for (var campaign : due) {
                try {
                    campaignService.sendCampaign(campaign.getId());
                    log.info("Triggered scheduled send for newsletter {}", campaign.getId());
                } catch (Exception e) {
                    log.error("Failed to trigger scheduled send for newsletter {}", campaign.getId(), e);
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }
}
