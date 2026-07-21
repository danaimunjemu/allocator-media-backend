package com.allocator.backend.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PathRewritingFilter implements Filter {

    private static final List<String> SERVICE_PREFIXES = Arrays.asList(
            "/auth-service",
            "/content-service",
            "/media-service",
            "/search-service",
            "/notification-service",
            "/analytics-service",
            "/payment-service",
            "/broker-service"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String uri = httpRequest.getRequestURI();
            
            // Special handling for the conflicting brokers endpoints
            if (uri.startsWith("/content-service/api/v1/brokers")) {
                String newUri = "/api/v1/content-brokers" + uri.substring("/content-service/api/v1/brokers".length());
                chain.doFilter(new PathRequestWrapper(httpRequest, "/content-service", newUri), response);
                return;
            }
            if (uri.startsWith("/broker-service/api/v1/brokers")) {
                String newUri = "/api/v1/brokers" + uri.substring("/broker-service/api/v1/brokers".length());
                chain.doFilter(new PathRequestWrapper(httpRequest, "/broker-service", newUri), response);
                return;
            }

            for (String prefix : SERVICE_PREFIXES) {
                if (uri.startsWith(prefix)) {
                    String newUri = uri.substring(prefix.length());
                    if (newUri.isEmpty()) {
                        newUri = "/";
                    }
                    chain.doFilter(new PathRequestWrapper(httpRequest, prefix, newUri), response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private static class PathRequestWrapper extends HttpServletRequestWrapper {
        private final String prefix;
        private final String newUri;

        public PathRequestWrapper(HttpServletRequest request, String prefix, String newUri) {
            super(request);
            this.prefix = prefix;
            this.newUri = newUri;
        }

        @Override
        public String getRequestURI() {
            return newUri;
        }

        @Override
        public String getServletPath() {
            String origServletPath = super.getServletPath();
            if (origServletPath.startsWith(prefix)) {
                String newServletPath = origServletPath.substring(prefix.length());
                return newServletPath.isEmpty() ? "/" : newServletPath;
            }
            return origServletPath;
        }
    }
}
