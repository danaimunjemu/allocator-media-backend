package com.allocator.authservice.security;

import com.allocator.common.security.ResolvedTierAttribute;
import com.allocator.paymentservice.service.SubscriptionService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final SubscriptionService subscriptionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);
        HttpServletRequest requestToUse = request;

        if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
            try {
                String email = jwtProvider.getEmailFromToken(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Extract claims and inject into request headers to mimic API Gateway behavior
                Claims claims = jwtProvider.getClaimsFromToken(jwt);
                String userId = claims.get("userId", String.class);

                // Resolve the caller's subscription tier and stamp it as a request
                // ATTRIBUTE (never a header) — attributes can't be forged by a client
                // hitting the API directly, unlike a header of any name. Failure
                // (e.g. a staff account with no Subscriber row) fails safe to FREE,
                // never granting more access than the lowest tier.
                String resolvedTier = "FREE";
                try {
                    resolvedTier = subscriptionService.getSubscription(UUID.fromString(userId)).getTier().name();
                } catch (Exception ex) {
                    log.debug("No resolvable subscription tier for user {}, defaulting to FREE", userId);
                }
                request.setAttribute(ResolvedTierAttribute.REQUEST_ATTR, resolvedTier);
                String userEmail = claims.get("email", String.class);
                if (userEmail == null) {
                    userEmail = claims.getSubject();
                }
                String userName = claims.get("name", String.class);

                List<?> rolesList = claims.get("roles", List.class);
                String roles = "";
                if (rolesList != null) {
                    roles = rolesList.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));
                }

                List<?> brandList = claims.get("brandIds", List.class);
                String brandIds = "";
                if (brandList != null) {
                    brandIds = brandList.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(","));
                }

                HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(request);
                wrappedRequest.addHeader("X-User-Id", userId);
                wrappedRequest.addHeader("X-User-Email", userEmail);
                wrappedRequest.addHeader("X-User-Roles", roles);
                wrappedRequest.addHeader("X-Brand-Ids", brandIds);
                if (userName != null) {
                    wrappedRequest.addHeader("X-User-Name", userName);
                }
                requestToUse = wrappedRequest;

                log.debug("Injected headers for authenticated user: {} (id: {})", userEmail, userId);

            } catch (Exception ex) {
                log.error("Could not set user authentication/headers in security context", ex);
            }
        }

        filterChain.doFilter(requestToUse, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
