package com.allocator.analyticsservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for a PDF download event emitted by a public-facing brand site.
 *
 * <p>Only fired for authorised downloads — the frontend enforces access gating
 * before calling this endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTrackingRequest {

    /** UUID of the content item that was downloaded. Required. */
    @NotBlank(message = "contentId is required")
    private String contentId;

    /**
     * Brand identifier — identifies which public site triggered the download.
     * Sourced from the {@code NEXT_PUBLIC_SITE_BRAND_ID} env variable on the
     * frontend and injected directly into the payload (it is a public value).
     */
    private String brandId;

    /**
     * Authenticated user ID, if available.  The analytics endpoint is
     * unauthenticated so this is optional and caller-supplied.
     */
    private String userId;
}
