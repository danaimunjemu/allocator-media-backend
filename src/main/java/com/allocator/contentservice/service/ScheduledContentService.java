package com.allocator.contentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledContentService {

    private final ContentWorkflowService workflowService;
    private final Lock schedulerLock = new ReentrantLock();

    @Scheduled(fixedRate = 60000) // Run every minute
    public void publishScheduledContent() {
        if (!schedulerLock.tryLock()) {
            log.debug("Content scheduler lock already held — skipping");
            return;
        }

        try {
            var publishedContent = workflowService.publishScheduledContent();

            if (!publishedContent.isEmpty()) {
                log.info("Auto-published {} scheduled content items", publishedContent.size());
                publishedContent.forEach(content ->
                        log.debug("Published content: {} at {}", content.getId(), content.getPublishedAt()));
            }
        } catch (Exception e) {
            log.error("Error in scheduled content publishing", e);
        } finally {
            schedulerLock.unlock();
        }
    }
}
