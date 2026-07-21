package com.allocator.authservice.security.oauth2;

import com.allocator.authservice.dto.AuthResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Single-use, short-lived handoff for OAuth2 login results: the browser is
// redirected back with only an opaque code in the URL (never the JWTs
// themselves), and the frontend's server-side callback route exchanges it for
// the real token pair over a backend-to-backend call. In-memory by design —
// fine for a single instance; would need to move to Redis/DB if this app is
// ever scaled to multiple instances behind a load balancer.
@Component
public class OAuthExchangeCodeStore {

    private static final long TTL_SECONDS = 60;

    private record Entry(AuthResponse response, Instant expiresAt) {
    }

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public String store(AuthResponse response) {
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(response, Instant.now().plusSeconds(TTL_SECONDS)));
        return code;
    }

    // Single-use: removes the entry on read regardless of outcome.
    public AuthResponse consume(String code) {
        Entry entry = codes.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.response();
    }
}
