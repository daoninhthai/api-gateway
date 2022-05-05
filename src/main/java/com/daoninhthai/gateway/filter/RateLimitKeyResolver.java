package com.daoninhthai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Custom KeyResolver that resolves rate limit key by:
 * 1. Authenticated user ID (from JWT filter header)
 * 2. Falls back to client IP address
 * 3. Falls back to "anonymous" if neither is available
 */
@Component
@Slf4j
public class RateLimitKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        // First try to get authenticated user ID
        String userId = exchange.getRequest().getHeaders().getFirst("X-Auth-UserId");
        if (userId != null && !userId.isEmpty()) {
            log.debug("Rate limit key resolved by userId: {}", userId);
            return Mono.just("user:" + userId);
        }

        // Fallback to client IP
        if (exchange.getRequest().getRemoteAddress() != null) {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            log.debug("Rate limit key resolved by IP: {}", ip);
            return Mono.just("ip:" + ip);
        }

        // Last resort
        log.debug("Rate limit key resolved as anonymous");
        return Mono.just("anonymous");
    }

}
