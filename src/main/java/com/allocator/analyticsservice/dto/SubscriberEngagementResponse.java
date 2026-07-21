package com.allocator.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SubscriberEngagementResponse {

    private long totalReads;
    private long timeSpentSeconds;
    private long downloads;
    private int engagementScore;
    private double newsletterOpenRate;
    private Instant lastActivityAt;

    /** Period-over-period deltas (last 7d vs the 7d before), formatted for display, e.g. "+12%". */
    private String readsTrend;
    private String timeSpentTrend;
    private String downloadsTrend;
    private String engagementScoreTrend;
    private String newsletterOpenRateTrend;

    private List<ActivityPoint> activityData;
    private List<NamedCount> engagementBreakdown;
    private List<FunnelStep> funnelData;
    private List<TopArticle> topArticles;
    private List<String> topTopics;
    private List<TimelineEntry> timeline;

    private String location;
    private String device;
    private String browser;

    @Data
    @Builder
    public static class ActivityPoint {
        private String date;
        private long views;
    }

    @Data
    @Builder
    public static class NamedCount {
        private String name;
        private long count;
    }

    @Data
    @Builder
    public static class FunnelStep {
        private String name;
        private long value;
    }

    @Data
    @Builder
    public static class TopArticle {
        private String id;
        private String title;
        private long reads;
    }

    @Data
    @Builder
    public static class TimelineEntry {
        private String id;
        private String type;
        private String title;
        private Instant time;
    }
}
