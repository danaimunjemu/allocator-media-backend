package com.allocator.analyticsservice.service;

import com.allocator.common.event.ArticlePublishedEvent;
import com.allocator.common.event.ArticleViewedEvent;
import com.allocator.common.event.MediaUploadedEvent;

public interface AnalyticsUpdateService {
    void processArticlePublished(ArticlePublishedEvent event);

    void processArticleViewed(ArticleViewedEvent event);

    void processMediaUploaded(MediaUploadedEvent event);

    void processNewsletterOpen(com.allocator.analyticsservice.dto.NotificationEvent event);

    void processNewsletterClick(com.allocator.analyticsservice.dto.NotificationEvent event);

    void trackView(String ipAddress, com.allocator.analyticsservice.dto.ViewTrackingRequest request);

    void trackSearch(com.allocator.analyticsservice.dto.SearchTrackingRequest request);

    /**
     * Records an authorised PDF download event.
     *
     * @param ipAddress originating IP captured server-side
     * @param request   download payload from the public-site client
     */
    void trackDownload(String ipAddress, com.allocator.analyticsservice.dto.DownloadTrackingRequest request);
}
