package com.allocator.contentservice.security;

import com.allocator.common.security.ResolvedTierAttribute;
import com.allocator.contentservice.model.AccessTier;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

// The only sanctioned way to read a viewer's subscription tier for gating
// decisions. Reads the request ATTRIBUTE JwtAuthenticationFilter sets after
// validating a JWT — never an HTTP header, which any caller could forge.
// Empty means anonymous (no valid JWT) or an unresolvable tier value.
public final class ViewerTierResolver {

    private ViewerTierResolver() {
    }

    public static Optional<AccessTier> resolve(HttpServletRequest request) {
        Object raw = request.getAttribute(ResolvedTierAttribute.REQUEST_ATTR);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(AccessTier.valueOf(raw.toString()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
