package com.daoninhthai.gateway.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration class that registers health indicators for all downstream microservices.
 * Each service gets its own health indicator that periodically checks its /actuator/health endpoint.
 */
@Configuration
@Slf4j
public class GatewayHealthConfig {

    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public WebClient healthCheckWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(256 * 1024))
                .build();
    }

    @Bean
    public HealthContributor userServiceHealthIndicator(WebClient healthCheckWebClient) {
        log.info("Registering health indicator for user-service");
        return new ServiceHealthIndicator(
                "user-service",
                "http://localhost:8081",
                healthCheckWebClient,
                HEALTH_CHECK_TIMEOUT
        );
    }

    @Bean
    public HealthContributor productServiceHealthIndicator(WebClient healthCheckWebClient) {
        log.info("Registering health indicator for product-service");
        return new ServiceHealthIndicator(
                "product-service",
                "http://localhost:8082",
                healthCheckWebClient,
                HEALTH_CHECK_TIMEOUT
        );
    }

    @Bean
    public HealthContributor orderServiceHealthIndicator(WebClient healthCheckWebClient) {
        log.info("Registering health indicator for order-service");
        return new ServiceHealthIndicator(
                "order-service",
                "http://localhost:8083",
                healthCheckWebClient,
                HEALTH_CHECK_TIMEOUT
        );
    }

    /**
     * Composite health contributor that aggregates all downstream service health checks
     * into a single "downstreamServices" health group.
     */
    @Bean
    public HealthContributor downstreamServicesHealthContributor(WebClient healthCheckWebClient) {
        Map<String, HealthContributor> contributors = new LinkedHashMap<>();
        contributors.put("user-service", new ServiceHealthIndicator(
                "user-service", "http://localhost:8081", healthCheckWebClient, HEALTH_CHECK_TIMEOUT));
        contributors.put("product-service", new ServiceHealthIndicator(
                "product-service", "http://localhost:8082", healthCheckWebClient, HEALTH_CHECK_TIMEOUT));
        contributors.put("order-service", new ServiceHealthIndicator(
                "order-service", "http://localhost:8083", healthCheckWebClient, HEALTH_CHECK_TIMEOUT));

        return CompositeHealthContributor.fromMap(contributors);
    }
}
