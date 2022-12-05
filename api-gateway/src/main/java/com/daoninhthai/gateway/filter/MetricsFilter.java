package com.daoninhthai.gateway.filter;

import com.daoninhthai.gateway.config.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Global filter that records Prometheus metrics for every request:
 * - Request count per route, method, and status code
 * - Request duration per route
 * - Active connection tracking
 */
@Component
@Slf4j
public class MetricsFilter implements GlobalFilter, Ordered {

    private final MetricsConfig metricsConfig;

    public MetricsFilter(MetricsConfig metricsConfig) {
        this.metricsConfig = metricsConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.nanoTime();

        // Track active connections
        metricsConfig.incrementActiveConnections();

        // Increment total request counter
        metricsConfig.getTotalRequestsCounter().increment();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Decrement active connections
                    metricsConfig.decrementActiveConnections();

                    long duration = System.nanoTime() - startTime;
                    String routeId = getRouteId(exchange);
                    String method = getMethod(exchange);
                    int statusCode = getStatusCode(exchange);

                    // Record per-route metrics
                    metricsConfig.getRouteCounter(routeId, method, statusCode).increment();

                    // Record request duration
                    Timer routeTimer = metricsConfig.getRouteTimer(routeId);
                    routeTimer.record(duration, TimeUnit.NANOSECONDS);

                    // Record on the global timer as well
                    metricsConfig.getRequestDurationTimer()
                            .record(duration, TimeUnit.NANOSECONDS);

                    log.debug("Metrics recorded - route: {}, method: {}, status: {}, duration: {}ms",
                            routeId, method, statusCode,
                            TimeUnit.NANOSECONDS.toMillis(duration));
                });
    }

    private String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    private String getMethod(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        return method != null ? method.name() : "UNKNOWN";
    }

    private int getStatusCode(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        return response.getStatusCode() != null
                ? response.getStatusCode().value()
                : 0;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

}
