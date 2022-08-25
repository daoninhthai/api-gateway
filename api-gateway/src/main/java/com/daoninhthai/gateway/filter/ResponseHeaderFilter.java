package com.daoninhthai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global post-filter that adds security headers to all responses.
 * <p>
 * Security headers added:
 * - X-Content-Type-Options: nosniff - Prevents MIME type sniffing
 * - X-Frame-Options: DENY - Prevents clickjacking by denying framing
 * - X-XSS-Protection: 1; mode=block - Enables XSS filter in browsers
 * - Strict-Transport-Security - Enforces HTTPS connections (HSTS)
 * - Content-Security-Policy - Controls resources the browser is allowed to load
 * - Referrer-Policy - Controls how much referrer info is sent
 * - Permissions-Policy - Controls browser features available to the page
 */
@Component
@Slf4j
public class ResponseHeaderFilter implements GlobalFilter, Ordered {

    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    private static final String REFERRER_POLICY = "Referrer-Policy";
    private static final String PERMISSIONS_POLICY = "Permissions-Policy";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            String requestPath = exchange.getRequest().getPath().value();
            log.debug("ResponseHeaderFilter: Adding security headers to response for {}", requestPath);

            // Prevent MIME type sniffing
            addHeaderIfAbsent(headers, X_CONTENT_TYPE_OPTIONS, "nosniff");

            // Prevent clickjacking
            addHeaderIfAbsent(headers, X_FRAME_OPTIONS, "DENY");

            // Enable browser XSS protection
            addHeaderIfAbsent(headers, X_XSS_PROTECTION, "1; mode=block");

            // Enforce HTTPS for 1 year, including subdomains
            addHeaderIfAbsent(headers, STRICT_TRANSPORT_SECURITY,
                    "max-age=31536000; includeSubDomains; preload");

            // Basic content security policy
            addHeaderIfAbsent(headers, CONTENT_SECURITY_POLICY,
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");

            // Control referrer information
            addHeaderIfAbsent(headers, REFERRER_POLICY, "strict-origin-when-cross-origin");

            // Restrict browser features
            addHeaderIfAbsent(headers, PERMISSIONS_POLICY,
                    "camera=(), microphone=(), geolocation=(), payment=()");

            // Remove server identification headers for security
            headers.remove("Server");
            headers.remove("X-Powered-By");

            log.debug("ResponseHeaderFilter: Security headers added successfully for {}", requestPath);
        }));
    }

    /**
     * Only add the header if it is not already present in the response.
     * This allows downstream services to override security headers if needed.
     */
    private void addHeaderIfAbsent(HttpHeaders headers, String headerName, String headerValue) {
        if (!headers.containsKey(headerName)) {
            headers.add(headerName, headerValue);
        }
    }

    @Override
    public int getOrder() {
        // Run after all other filters to ensure security headers are added last
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
