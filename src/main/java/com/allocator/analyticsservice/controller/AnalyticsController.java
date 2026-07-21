package com.allocator.analyticsservice.controller;

import com.allocator.analyticsservice.dto.ActivityLogResponse;
import com.allocator.analyticsservice.dto.AnalyticsSummaryResponse;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse;
import com.allocator.analyticsservice.dto.SubscriberHeatmapPoint;
import com.allocator.analyticsservice.dto.TrendingArticleResponse;
import com.allocator.analyticsservice.model.ArticleAnalytics;
import com.allocator.analyticsservice.model.AuthorAnalytics;
import com.allocator.analyticsservice.model.TopicAnalytics;
import com.allocator.analyticsservice.repository.ActivityLogRepository;
import com.allocator.analyticsservice.service.AnalyticsQueryService;
import com.allocator.analyticsservice.service.AnalyticsSummaryService;
import com.allocator.analyticsservice.service.SubscriberEngagementService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Validated
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final AnalyticsSummaryService summaryService;
    private final com.allocator.analyticsservice.service.AnalyticsUpdateService updateService;
    private final ActivityLogRepository activityLogRepository;
    private final SubscriberEngagementService subscriberEngagementService;

    /**
     * Admin-only aggregated dashboard summary.
     * Combines analytics-service DB data with financial metrics from payment-service.
     * Gracefully degrades: if payment-service is down, financial fields are null/0.
     */
    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(
            @RequestParam @NotBlank String brandId,
            @RequestParam(defaultValue = "30d") String period,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        return summaryService.getSummary(brandId, period);
    }

    @PostMapping("/view")
    public void trackView(@RequestBody com.allocator.analyticsservice.dto.ViewTrackingRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        updateService.trackView(ipAddress, request);
    }

    @PostMapping("/search")
    public void trackSearch(@RequestBody com.allocator.analyticsservice.dto.SearchTrackingRequest request) {
        updateService.trackSearch(request);
    }

    /**
     * Records an authorised PDF download event.
     *
     * <p>This endpoint is intentionally unauthenticated — the public-site
     * frontend enforces access gating client-side before firing this event.
     * Only successful, authorised downloads should reach this endpoint.
     */
    @PostMapping("/download")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackDownload(
            @RequestBody com.allocator.analyticsservice.dto.DownloadTrackingRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        updateService.trackDownload(ipAddress, request);
    }

    @GetMapping("/articles/top")
    public List<ArticleAnalytics> getTopArticles(@RequestParam(defaultValue = "10") int limit) {
        return queryService.getTopArticles(limit);
    }

    @GetMapping("/topics/trending")
    public List<TopicAnalytics> getTrendingTopics(@RequestParam(defaultValue = "5") int limit) {
        return queryService.getTrendingTopics(limit);
    }

    @GetMapping("/authors/performance")
    public List<AuthorAnalytics> getAuthorPerformance() {
        return queryService.getAuthorPerformance();
    }

    @GetMapping("/trending")
    public List<TrendingArticleResponse> getTrendingArticles(@RequestParam(defaultValue = "10") int limit) {
        return queryService.getTrendingArticles(limit);
    }

    @GetMapping("/authors/{authorId}")
    public AuthorAnalytics getAuthorStats(@PathVariable String authorId) {
        return queryService.getAuthorStats(authorId);
    }

    @GetMapping("/search/trending")
    public List<String> getTrendingSearchQueries() {
        return queryService.getTrendingSearches();
    }

    @GetMapping("/activity-logs")
    public Page<ActivityLogResponse> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String activityType,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {

        requireAdmin(roles);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return activityLogRepository
                .findWithFilters(
                        (userId != null && !userId.isBlank()) ? userId : null,
                        (activityType != null && !activityType.isBlank()) ? activityType : null,
                        pageable)
                .map(log -> ActivityLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .contentId(log.getContentId())
                        .activityType(log.getActivityType())
                        .metadata(log.getMetadata())
                        .timestamp(log.getTimestamp())
                        .build());
    }

    @GetMapping("/admin/subscribers/{userId}/engagement")
    public SubscriberEngagementResponse getSubscriberEngagement(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return subscriberEngagementService.getEngagement(userId);
    }

    @GetMapping("/admin/subscribers/{userId}/heatmap")
    public List<SubscriberHeatmapPoint> getSubscriberHeatmap(
            @PathVariable UUID userId,
            @RequestParam(required = false) Integer year,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles) {
        requireAdmin(roles);
        return subscriberEngagementService.getHeatmap(userId, year != null ? year : Year.now().getValue());
    }

    private void requireAdmin(String rolesHeader) {
        boolean isAdmin = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> r.equalsIgnoreCase("ROLE_ADMIN") || r.equalsIgnoreCase("ADMIN")
                        || r.equalsIgnoreCase("ROLE_SUPER_ADMIN") || r.equalsIgnoreCase("SUPER_ADMIN"));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
