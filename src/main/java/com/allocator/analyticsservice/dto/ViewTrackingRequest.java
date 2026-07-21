package com.allocator.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for a page-view or content-view tracking event emitted by a
 * public-facing brand site.
 *
 * <p>All fields are optional so the endpoint degrades gracefully when the
 * browser omits metadata (e.g. private-browsing mode strips the referrer).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewTrackingRequest {

    // ---- Legacy fields (kept for backwards compatibility) ----

    /** UUID of the content item being viewed, when known. */
    private String contentId;

    /** Authenticated user ID, if the visitor is logged in. */
    private String userId;

    /** Anonymous session identifier. */
    private String sessionId;

    /** Time the visitor spent reading, in seconds (populated on page leave). */
    private Long readingTimeSeconds;

    // ---- Page-view enrichment fields ----

    /**
     * URL pathname of the viewed page (e.g. {@code /analysis/how-to-invest-in-nse}).
     * For content pages this is typically the content slug; for generic pages it
     * is the raw pathname.
     */
    private String slug;

    /**
     * Brand identifier — identifies which public site the view originated from.
     * Sourced from the {@code NEXT_PUBLIC_SITE_BRAND_ID} env variable on the
     * frontend; never set by the end user.
     */
    private String brandId;

    /**
     * Value of {@code document.referrer} at the time of the page view.
     * Empty string for direct navigation or same-origin client-side transitions.
     */
    private String referrer;

    /**
     * Value of {@code navigator.userAgent} — used for device/browser
     * segmentation in analytics dashboards.
     */
    private String userAgent;
}
