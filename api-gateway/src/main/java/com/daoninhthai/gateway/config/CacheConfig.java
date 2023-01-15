package com.daoninhthai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for response caching.
 * Supports global defaults and per-route TTL overrides.
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.cache")
@Data
public class CacheConfig {

    /**
     * Whether response caching is enabled globally.
     */
    private boolean enabled = true;

    /**
     * Default TTL for cached responses in seconds.
     */
    private long defaultTtlSeconds = 60;

    /**
     * Maximum number of entries in the cache.
     */
    private int maxSize = 1000;

    /**
     * Per-route TTL overrides. Key is the route ID, value is TTL in seconds.
     */
    private Map<String, Long> routeTtl = new HashMap<>();

    /**
     * Interval in seconds for running cache eviction of expired entries.
     */
    private long evictionIntervalSeconds = 30;

    /**
     * Get the TTL for a specific route, falling back to the default.
     */
    public long getTtlForRoute(String routeId) {
        return routeTtl.getOrDefault(routeId, defaultTtlSeconds);
    }

}
