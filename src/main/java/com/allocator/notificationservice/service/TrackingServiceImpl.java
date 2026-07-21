package com.allocator.notificationservice.service;

import com.allocator.notificationservice.model.EmailTracking;
import com.allocator.notificationservice.repository.EmailTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final EmailTrackingRepository repository;

    @Override
    @Transactional
    public void recordOpen(UUID notificationId) {
        log.info("Email opened for notification: {}", notificationId);
        repository.findByNotificationId(notificationId)
                .map(tracking -> {
                    if (!tracking.isOpened()) {
                        tracking.setOpened(true);
                        tracking.setOpenedAt(LocalDateTime.now());
                        return repository.save(tracking);
                    }
                    return tracking;
                })
                .orElseGet(() -> {
                    log.warn("Tracking record not found for notification: {}, creating new one", notificationId);
                    EmailTracking newTracking = EmailTracking.builder()
                            .notificationId(notificationId)
                            .opened(true)
                            .openedAt(LocalDateTime.now())
                            .build();
                    return repository.save(newTracking);
                });
    }

    @Override
    @Transactional
    public void recordClick(UUID notificationId) {
        log.info("Email link clicked for notification: {}", notificationId);
        repository.findByNotificationId(notificationId)
                .map(tracking -> {
                    if (!tracking.isClicked()) {
                        tracking.setClicked(true);
                        tracking.setClickedAt(LocalDateTime.now());
                        return repository.save(tracking);
                    }
                    return tracking;
                })
                .orElseGet(() -> {
                    log.warn("Tracking record not found for notification: {}, creating new one for click",
                            notificationId);
                    EmailTracking newTracking = EmailTracking.builder()
                            .notificationId(notificationId)
                            .clicked(true)
                            .clickedAt(LocalDateTime.now())
                            .build();
                    return repository.save(newTracking);
                });
    }
}
