package com.daoninhthai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Log request details
        String requestId = request.getId();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String uri = request.getURI().toString();
        String path = request.getPath().value();
        String queryParams = request.getQueryParams().isEmpty() ? "" : request.getQueryParams().toString();
        String remoteAddr = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        // Log safe headers (exclude sensitive ones)
        String headers = filterSensitiveHeaders(request.getHeaders());

        log.info("[PRE] Request ID: {} | {} {} | Remote: {} | Query: {} | Headers: {}",
                requestId, method, path, remoteAddr, queryParams, headers);

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;

            int statusCode = response.getStatusCode() != null
                    ? response.getStatusCode().value()
                    : 0;

            log.info("[POST] Request ID: {} | {} {} | Status: {} | Duration: {}ms",
                    requestId, method, path, statusCode, duration);

            if (duration > 3000) {
                log.warn("[SLOW] Request {} {} took {}ms", method, path, duration);
            }
        }));
    }

    private String filterSensitiveHeaders(HttpHeaders headers) {
        return headers.entrySet().stream()
                .filter(entry -> !SENSITIVE_HEADERS.contains(entry.getKey().toLowerCase()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

}
