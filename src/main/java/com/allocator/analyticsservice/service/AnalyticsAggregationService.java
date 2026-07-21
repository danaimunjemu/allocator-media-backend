package com.allocator.analyticsservice.service;

import com.allocator.analyticsservice.model.ArticleEngagement;
import com.allocator.analyticsservice.repository.ArticleEngagementRepository;
import com.allocator.analyticsservice.repository.ArticleViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsAggregationService {

    private final ArticleViewRepository viewRepository;
    private final ArticleEngagementRepository engagementRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void aggregateArticleEngagement() {
        log.info("Starting hourly article engagement aggregation at {}", LocalDateTime.now());
        
        List<String> contentIds = viewRepository.findAllContentIds();
        
        for (String contentId : contentIds) {
            long totalViews = viewRepository.countByContentId(contentId);
            long uniqueViews = viewRepository.countUniqueViewsByContentId(contentId);
            Double avgReadingTime = viewRepository.averageReadingTimeByContentId(contentId);
            
            ArticleEngagement engagement = engagementRepository.findByContentId(contentId)
                    .orElse(ArticleEngagement.builder().contentId(contentId).build());
            
            engagement.setTotalViews(totalViews);
            engagement.setUniqueViews(uniqueViews);
            engagement.setAverageReadingTime(avgReadingTime != null ? avgReadingTime : 0.0);
            
            engagementRepository.save(engagement);
        }
        
        log.info("Finished article engagement aggregation. Processed {} articles.", contentIds.size());
    }
}
