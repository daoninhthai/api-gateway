package com.daoninhthai.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for custom gateway metrics exposed via Prometheus.
 * Registers counters, timers, and gauges for monitoring gateway performance.
 */
@Configuration
@Slf4j
@Getter
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    private Counter totalRequestsCounter;
    private Timer requestDurationTimer;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initMetrics() {
        // Counter for total requests processed by the gateway
        totalRequestsCounter = Counter.builder("gateway_requests_total")
                .description("Total number of requests processed by the API gateway")
                .tag("type", "all")
                .register(meterRegistry);

        // Timer for request duration
        requestDurationTimer = Timer.builder("gateway_request_duration_seconds")
                .description("Duration of requests processed by the API gateway")
                .register(meterRegistry);

        // Gauge for active connections
        meterRegistry.gauge("gateway_active_connections",
                activeConnections);

        log.info("Custom gateway metrics initialized: gateway_requests_total, " +
                "gateway_request_duration_seconds, gateway_active_connections");
    }

    /**
     * Get or create a counter for a specific route.
     */
    public Counter getRouteCounter(String routeId, String method, int statusCode) {
        return Counter.builder("gateway_requests_total")
                .description("Total requests per route")
                .tag("route", routeId)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry);
    }

    /**
     * Get or create a timer for a specific route.
     */
    public Timer getRouteTimer(String routeId) {
        return Timer.builder("gateway_request_duration_seconds")
                .description("Request duration per route")
                .tag("route", routeId)
                .register(meterRegistry);
    }

    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

}
