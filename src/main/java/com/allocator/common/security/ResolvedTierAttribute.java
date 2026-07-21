package com.allocator.common.security;

// Shared request-attribute name for the subscription tier JwtAuthenticationFilter
// resolves from a validated JWT. Content-service reads this attribute only —
// never a client-suppliable HTTP header — since a header of any name can be
// forged by a caller hitting the API directly.
public final class ResolvedTierAttribute {

    public static final String REQUEST_ATTR = "com.allocator.resolvedSubscriptionTier";

    private ResolvedTierAttribute() {
    }
}
