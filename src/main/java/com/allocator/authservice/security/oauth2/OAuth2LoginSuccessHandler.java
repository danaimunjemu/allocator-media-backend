package com.allocator.authservice.security.oauth2;

import com.allocator.authservice.dto.AuthResponse;
import com.allocator.authservice.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Runs after Google/Facebook/LinkedIn OAuth2 login succeeds. Finds-or-creates
// the reader account, mints this app's own JWT pair (same as a password
// login), and hands it to the frontend via a short-lived opaque code in the
// redirect URL — the JWTs themselves never appear in the browser's address
// bar or a JS-readable response until the frontend's server-side callback
// route exchanges the code over a backend-to-backend call.
@Component
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuthExchangeCodeStore codeStore;

    @Value("${app.frontend-url:http://localhost:3001}")
    private String frontendUrl;

    // AuthService is injected lazily (as a proxy) because it transitively
    // depends on AuthenticationManager, whose construction touches
    // SecurityConfig — and SecurityConfig depends on this handler. Eagerly
    // requiring a fully-built AuthService here would form a circular
    // dependency; deferring resolution until a request actually arrives breaks it.
    public OAuth2LoginSuccessHandler(@Lazy AuthService authService, OAuthExchangeCodeStore codeStore) {
        this.authService = authService;
        this.codeStore = codeStore;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String registrationId = extractRegistrationId(request);
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        ProviderProfile profile = extractProfile(registrationId, principal);

        if (profile.email() == null) {
            log.error("OAuth2 login via {} did not return an email address", registrationId);
            response.sendRedirect(frontendUrl + "/login?error=oauth_no_email");
            return;
        }

        try {
            AuthResponse authResponse = authService.loginOrRegisterOAuthUser(
                    profile.email(), profile.firstName(), profile.lastName(), profile.avatarUrl());
            String code = codeStore.store(authResponse);
            response.sendRedirect(frontendUrl + "/api/auth/oauth-callback?code="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to complete OAuth2 login for {}", profile.email(), e);
            response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
        }
    }

    private String extractRegistrationId(HttpServletRequest request) {
        // Set by Spring Security's OAuth2LoginAuthenticationFilter on the
        // request path it handles (/login/oauth2/code/{registrationId}).
        String uri = request.getRequestURI();
        String[] segments = uri.split("/");
        return segments[segments.length - 1];
    }

    private record ProviderProfile(String email, String firstName, String lastName, String avatarUrl) {
    }

    private ProviderProfile extractProfile(String registrationId, OAuth2User principal) {
        if (principal instanceof OidcUser oidcUser) {
            // Google and LinkedIn are both OIDC — standard claim names.
            return new ProviderProfile(
                    oidcUser.getEmail(),
                    oidcUser.getGivenName(),
                    oidcUser.getFamilyName(),
                    oidcUser.getPicture());
        }

        // Facebook is plain OAuth2 (not OIDC) — attributes come from the
        // user-info-uri response shape configured in application.yml.
        java.util.Map<String, Object> attrs = principal.getAttributes();
        String email = (String) attrs.get("email");
        String name = (String) attrs.get("name");
        String firstName = (String) attrs.getOrDefault("first_name", name);
        String lastName = (String) attrs.get("last_name");
        String avatarUrl = null;
        Object picture = attrs.get("picture");
        if (picture instanceof java.util.Map<?, ?> pictureMap) {
            Object data = pictureMap.get("data");
            if (data instanceof java.util.Map<?, ?> dataMap) {
                avatarUrl = (String) dataMap.get("url");
            }
        }
        return new ProviderProfile(email, firstName, lastName, avatarUrl);
    }
}
