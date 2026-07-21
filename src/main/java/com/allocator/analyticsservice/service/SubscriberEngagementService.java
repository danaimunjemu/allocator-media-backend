package com.allocator.analyticsservice.service;

import com.allocator.analyticsservice.dto.SubscriberEngagementResponse;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse.ActivityPoint;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse.FunnelStep;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse.NamedCount;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse.TimelineEntry;
import com.allocator.analyticsservice.dto.SubscriberEngagementResponse.TopArticle;
import com.allocator.analyticsservice.dto.SubscriberHeatmapPoint;
import com.allocator.analyticsservice.model.ArticleView;
import com.allocator.analyticsservice.model.ContentDownload;
import com.allocator.analyticsservice.model.NewsletterOpen;
import com.allocator.analyticsservice.repository.ArticleViewRepository;
import com.allocator.analyticsservice.repository.ContentDownloadRepository;
import com.allocator.analyticsservice.repository.NewsletterOpenRepository;
import com.allocator.authservice.model.LoginHistory;
import com.allocator.authservice.model.UserSession;
import com.allocator.authservice.repository.LoginHistoryRepository;
import com.allocator.authservice.repository.UserSessionRepository;
import com.allocator.contentservice.model.Content;
import com.allocator.contentservice.model.ContentType;
import com.allocator.contentservice.repository.ContentRepository;
import com.allocator.notificationservice.model.NotificationStatus;
import com.allocator.notificationservice.repository.NotificationRepository;
import com.allocator.paymentservice.entity.Subscriber;
import com.allocator.paymentservice.repository.SubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assembles the per-subscriber engagement dashboard shown on the admin
 * subscriber detail page, by aggregating existing raw event tables
 * (article views, downloads, newsletter opens, logins/sessions) rather than
 * maintaining a separate denormalized store.
 */
@Service
@RequiredArgsConstructor
public class SubscriberEngagementService {

    private static final long SUBSTANTIAL_READ_SECONDS = 30L;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter ISO_DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ArticleViewRepository articleViewRepository;
    private final ContentDownloadRepository contentDownloadRepository;
    private final NewsletterOpenRepository newsletterOpenRepository;
    private final NotificationRepository notificationRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserSessionRepository userSessionRepository;
    private final ContentRepository contentRepository;
    private final SubscriberRepository subscriberRepository;

    @Transactional(readOnly = true)
    public SubscriberEngagementResponse getEngagement(UUID userId) {
        Subscriber subscriber = subscriberRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscriber not found: " + userId));

        String uid = userId.toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime fourteenDaysAgo = now.minusDays(14);

        long totalReads = articleViewRepository.countDistinctContentByUserId(uid);
        long timeSpentSeconds = articleViewRepository.sumReadingTimeByUserId(uid);
        long downloads = contentDownloadRepository.countByUserId(uid);

        long opens = newsletterOpenRepository.countByEmail(subscriber.getEmail());
        long sent = notificationRepository.countByEmailAndStatus(subscriber.getEmail(), NotificationStatus.SENT);
        double newsletterOpenRate = sent > 0 ? Math.round((opens * 10000.0) / sent) / 100.0 : 0.0;

        int engagementScore = computeEngagementScore(totalReads, downloads, timeSpentSeconds);

        Instant lastActivity = computeLastActivity(uid, subscriber);

        // Trends: last 7 days vs the 7 days before that.
        long readsThisWeek = articleViewRepository.countByUserIdAndViewedAtAfter(uid, sevenDaysAgo);
        long readsLast14 = articleViewRepository.countByUserIdAndViewedAtAfter(uid, fourteenDaysAgo);
        long readsPrevWeek = Math.max(0, readsLast14 - readsThisWeek);

        long downloadsThisWeek = contentDownloadRepository.countByUserIdAndDownloadedAtAfter(uid, sevenDaysAgo);
        long downloadsLast14 = contentDownloadRepository.countByUserIdAndDownloadedAtAfter(uid, fourteenDaysAgo);
        long downloadsPrevWeek = Math.max(0, downloadsLast14 - downloadsThisWeek);

        // Activity Over Time (14d daily page views)
        List<ArticleView> views14d = articleViewRepository.findByUserIdAndViewedAtBetween(uid, fourteenDaysAgo, now);
        Map<LocalDate, Long> viewsByDay = views14d.stream()
                .collect(Collectors.groupingBy(v -> v.getViewedAt().toLocalDate(), Collectors.counting()));
        List<ActivityPoint> activityData = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate day = now.toLocalDate().minusDays(i);
            activityData.add(ActivityPoint.builder()
                    .date(day.format(DAY_FORMAT))
                    .views(viewsByDay.getOrDefault(day, 0L))
                    .build());
        }

        // Top content (by view count) — drives both "Most Read Articles" and the
        // content-type / topic breakdowns, capped at 50 distinct pieces of content.
        List<Object[]> topContentRows = articleViewRepository.findTopContentsByUserId(uid, PageRequest.of(0, 50));
        Map<String, Long> viewCountByContentId = new HashMap<>();
        List<String> orderedContentIds = new ArrayList<>();
        for (Object[] row : topContentRows) {
            String contentId = (String) row[0];
            Long count = (Long) row[1];
            viewCountByContentId.put(contentId, count);
            orderedContentIds.add(contentId);
        }
        Map<UUID, Content> contentById = fetchContentByIds(orderedContentIds);

        List<TopArticle> topArticles = orderedContentIds.stream()
                .limit(3)
                .map(id -> {
                    Content c = contentById.get(safeUuid(id));
                    return TopArticle.builder()
                            .id(id)
                            .title(c != null ? c.getTitle() : "Untitled")
                            .reads(viewCountByContentId.get(id))
                            .build();
                })
                .collect(Collectors.toList());

        List<NamedCount> engagementBreakdown = buildEngagementBreakdown(orderedContentIds, viewCountByContentId, contentById);
        List<String> topTopics = buildTopTopics(orderedContentIds, viewCountByContentId, contentById);

        // Conversion funnel
        long visits = articleViewRepository.countByUserId(uid);
        long substantialReads = articleViewRepository.countByUserIdAndReadingTimeSecondsGreaterThanEqual(uid, SUBSTANTIAL_READ_SECONDS);
        long distinctActiveDays = articleViewRepository.countDistinctActiveDaysByUserId(uid);
        long returns = Math.max(0, distinctActiveDays - 1);

        List<FunnelStep> funnelData = List.of(
                FunnelStep.builder().name("Visit").value(visits).build(),
                FunnelStep.builder().name("Read").value(substantialReads).build(),
                FunnelStep.builder().name("Download").value(downloads).build(),
                FunnelStep.builder().name("Subscribe").value(1).build(),
                FunnelStep.builder().name("Return").value(returns).build()
        );

        List<TimelineEntry> timeline = buildTimeline(userId, uid, subscriber.getEmail(), contentById);

        // Geography & tech — most recent session wins, falls back to the
        // backfilled snapshot on the subscriber row, then "Unknown".
        Optional<UserSession> latestSession = userSessionRepository.findFirstByUserIdOrderByLastActiveDesc(userId);
        String device = latestSession.map(UserSession::getDeviceName)
                .filter(s -> s != null && !s.isBlank())
                .orElse(subscriber.getPrimaryDevice() != null ? subscriber.getPrimaryDevice() : "Unknown");
        String browser = latestSession.map(UserSession::getBrowser)
                .filter(s -> s != null && !s.isBlank())
                .orElse(subscriber.getWebBrowser() != null ? subscriber.getWebBrowser() : "Unknown");
        String location = latestSession.map(UserSession::getLocation)
                .filter(s -> s != null && !s.isBlank() && !s.equals("Unknown"))
                .orElse(subscriber.getLocation() != null ? subscriber.getLocation() : "Unknown");

        return SubscriberEngagementResponse.builder()
                .totalReads(totalReads)
                .timeSpentSeconds(timeSpentSeconds)
                .downloads(downloads)
                .engagementScore(engagementScore)
                .newsletterOpenRate(newsletterOpenRate)
                .lastActivityAt(lastActivity)
                .readsTrend(trendLabel(readsThisWeek, readsPrevWeek))
                .timeSpentTrend(trendLabel(readsThisWeek, readsPrevWeek))
                .downloadsTrend(trendLabel(downloadsThisWeek, downloadsPrevWeek))
                .engagementScoreTrend(engagementScore >= 50 ? "Healthy" : "Needs attention")
                .newsletterOpenRateTrend(newsletterOpenRate >= 20 ? "Above avg" : "Below avg")
                .activityData(activityData)
                .engagementBreakdown(engagementBreakdown)
                .funnelData(funnelData)
                .topArticles(topArticles)
                .topTopics(topTopics)
                .timeline(timeline)
                .location(location)
                .device(device)
                .browser(browser)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SubscriberHeatmapPoint> getHeatmap(UUID userId, int year) {
        String uid = userId.toString();
        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        Map<LocalDate, Long> counts = new HashMap<>();
        for (ArticleView v : articleViewRepository.findByUserIdAndViewedAtBetween(uid, yearStart, yearEnd)) {
            counts.merge(v.getViewedAt().toLocalDate(), 1L, Long::sum);
        }
        for (ContentDownload d : contentDownloadRepository.findByUserIdAndDownloadedAtBetween(uid, yearStart, yearEnd)) {
            counts.merge(d.getDownloadedAt().toLocalDate(), 1L, Long::sum);
        }
        for (LoginHistory h : loginHistoryRepository.findByUserIdOrderByLoginAtDesc(userId)) {
            LocalDate day = h.getLoginAt().toLocalDate();
            if (!day.isBefore(yearStart.toLocalDate()) && !day.isAfter(yearEnd.toLocalDate())) {
                counts.merge(day, 1L, Long::sum);
            }
        }

        return counts.entrySet().stream()
                .map(e -> SubscriberHeatmapPoint.builder().date(e.getKey().format(ISO_DAY)).count(e.getValue()).build())
                .sorted(Comparator.comparing(SubscriberHeatmapPoint::getDate))
                .collect(Collectors.toList());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Instant computeLastActivity(String uid, Subscriber subscriber) {
        Instant last = subscriber.getLastActiveAt();
        List<ArticleView> recentView = articleViewRepository.findTop5ByUserIdOrderByViewedAtDesc(uid);
        if (!recentView.isEmpty()) {
            Instant viewInstant = recentView.get(0).getViewedAt().toInstant(ZoneOffset.UTC);
            if (last == null || viewInstant.isAfter(last)) last = viewInstant;
        }
        List<ContentDownload> recentDownload = contentDownloadRepository.findTop5ByUserIdOrderByDownloadedAtDesc(uid);
        if (!recentDownload.isEmpty()) {
            Instant dlInstant = recentDownload.get(0).getDownloadedAt().toInstant(ZoneOffset.UTC);
            if (last == null || dlInstant.isAfter(last)) last = dlInstant;
        }
        return last;
    }

    private int computeEngagementScore(long totalReads, long downloads, long timeSpentSeconds) {
        // Weighted, clamped 0-100: reads and downloads matter most, time spent
        // is a smaller secondary signal (diminishing returns past ~1hr).
        double score = (totalReads * 4.0) + (downloads * 6.0) + (Math.min(timeSpentSeconds / 60.0, 60) * 0.5);
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    private String trendLabel(long current, long previous) {
        if (previous == 0) return current > 0 ? "+" + current : "0";
        double pct = ((current - previous) * 100.0) / previous;
        return (pct >= 0 ? "+" : "") + Math.round(pct) + "%";
    }

    private Map<UUID, Content> fetchContentByIds(List<String> contentIds) {
        List<UUID> uuids = contentIds.stream()
                .map(this::safeUuid)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (uuids.isEmpty()) return Map.of();
        return contentRepository.findAllById(uuids).stream()
                .collect(Collectors.toMap(Content::getId, c -> c));
    }

    private UUID safeUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return null;
        }
    }

    private List<NamedCount> buildEngagementBreakdown(List<String> contentIds, Map<String, Long> counts, Map<UUID, Content> contentById) {
        Map<ContentType, Long> byType = new HashMap<>();
        for (String id : contentIds) {
            Content c = contentById.get(safeUuid(id));
            if (c == null || c.getContentType() == null) continue;
            byType.merge(c.getContentType(), counts.get(id), Long::sum);
        }
        long articles = byType.getOrDefault(ContentType.ARTICLE, 0L);
        long research = byType.getOrDefault(ContentType.RESEARCH, 0L);
        long podcasts = byType.getOrDefault(ContentType.PODCAST, 0L);
        long videos = byType.getOrDefault(ContentType.VIDEO, 0L) + byType.getOrDefault(ContentType.INTERVIEW, 0L);

        return List.of(
                NamedCount.builder().name("Articles").count(articles).build(),
                NamedCount.builder().name("Research").count(research).build(),
                NamedCount.builder().name("Podcasts").count(podcasts).build(),
                NamedCount.builder().name("Videos").count(videos).build()
        );
    }

    private List<String> buildTopTopics(List<String> contentIds, Map<String, Long> counts, Map<UUID, Content> contentById) {
        Map<String, Long> tagWeights = new HashMap<>();
        for (String id : contentIds) {
            Content c = contentById.get(safeUuid(id));
            if (c == null || c.getTags() == null) continue;
            long weight = counts.getOrDefault(id, 1L);
            for (String tag : c.getTags()) {
                if (tag == null || tag.isBlank()) continue;
                tagWeights.merge(tag, weight, Long::sum);
            }
        }
        return tagWeights.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<TimelineEntry> buildTimeline(UUID userId, String uid, String email, Map<UUID, Content> contentById) {
        List<TimelineEntry> entries = new ArrayList<>();

        for (ArticleView v : articleViewRepository.findTop5ByUserIdOrderByViewedAtDesc(uid)) {
            Content c = contentById.get(safeUuid(v.getContentId()));
            String title = c != null ? c.getTitle() : (v.getSlug() != null ? v.getSlug() : "an article");
            entries.add(TimelineEntry.builder()
                    .id(v.getId().toString())
                    .type("article")
                    .title("Viewed: " + title)
                    .time(v.getViewedAt().toInstant(ZoneOffset.UTC))
                    .build());
        }

        for (ContentDownload d : contentDownloadRepository.findTop5ByUserIdOrderByDownloadedAtDesc(uid)) {
            Content c = contentById.get(safeUuid(d.getContentId()));
            String title = c != null ? c.getTitle() : "a report";
            entries.add(TimelineEntry.builder()
                    .id(d.getId().toString())
                    .type("download")
                    .title("Downloaded: " + title)
                    .time(d.getDownloadedAt().toInstant(ZoneOffset.UTC))
                    .build());
        }

        for (LoginHistory h : loginHistoryRepository.findByUserIdOrderByLoginAtDesc(userId)) {
            if (entries.stream().filter(e -> e.getType().equals("login")).count() >= 5) break;
            entries.add(TimelineEntry.builder()
                    .id(h.getId().toString())
                    .type("login")
                    .title("Logged in via " + (h.getDeviceInfo() != null ? h.getDeviceInfo() : "web"))
                    .time(h.getLoginAt().toInstant(ZoneOffset.UTC))
                    .build());
        }

        for (NewsletterOpen n : newsletterOpenRepository.findByEmailOrderByOpenedAtDesc(email)) {
            if (entries.stream().filter(e -> e.getType().equals("newsletter")).count() >= 5) break;
            entries.add(TimelineEntry.builder()
                    .id(n.getId().toString())
                    .type("newsletter")
                    .title("Opened newsletter email")
                    .time(n.getOpenedAt().toInstant(ZoneOffset.UTC))
                    .build());
        }

        return entries.stream()
                .sorted(Comparator.comparing(TimelineEntry::getTime).reversed())
                .limit(8)
                .collect(Collectors.toList());
    }
}
