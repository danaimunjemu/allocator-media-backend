package com.allocator.notificationservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import com.allocator.notificationservice.repository.EmailTrackingRepository;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NotificationMetrics {

    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Counter campaignSentCounter;
    private final EmailTrackingRepository trackingRepository;

    public NotificationMetrics(MeterRegistry registry, EmailTrackingRepository trackingRepository) {
        this.trackingRepository = trackingRepository;

        this.emailSentCounter = Counter.builder("email.sent.count")
                .description("Total number of emails sent successfully")
                .register(registry);

        this.emailFailedCounter = Counter.builder("email.failed.count")
                .description("Total number of emails that failed to send")
                .register(registry);

        this.campaignSentCounter = Counter.builder("campaign.sent.count")
                .description("Total number of campaigns triggered")
                .register(registry);

        Gauge.builder("email.open.rate", this::calculateOpenRate)
                .description("Percentage of emails opened")
                .register(registry);

        Gauge.builder("email.click.rate", this::calculateClickRate)
                .description("Percentage of emails clicked")
                .register(registry);
    }

    public void incrementEmailSent() {
        emailSentCounter.increment();
    }

    public void incrementEmailFailed() {
        emailFailedCounter.increment();
    }

    public void incrementCampaignSent() {
        campaignSentCounter.increment();
    }

    private double calculateOpenRate() {
        long total = trackingRepository.count();
        if (total == 0)
            return 0;
        long opened = trackingRepository.countByOpenedTrue();
        return (double) opened / total;
    }

    private double calculateClickRate() {
        long total = trackingRepository.count();
        if (total == 0)
            return 0;
        long clicked = trackingRepository.countByClickedTrue();
        return (double) clicked / total;
    }
}
