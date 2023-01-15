package com.daoninhthai.gateway.service;

import com.daoninhthai.gateway.config.CacheConfig;
import com.daoninhthai.gateway.dto.CacheEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing cached HTTP responses.
 * Uses a ConcurrentHashMap for thread-safe, in-memory caching
 * with automatic eviction of expired entries.
 */
@Service
@Slf4j
@EnableScheduling
public class CacheService {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final CacheConfig cacheConfig;

    public CacheService(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
    }

    /**
     * Get a cached entry by key. Returns empty if not found or expired.
     */
    public Optional<CacheEntry> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            log.debug("Cache MISS for key: {}", key);
            return Optional.empty();
        }

        if (entry.isExpired()) {
            log.debug("Cache entry expired for key: {}", key);
            cache.remove(key);
            return Optional.empty();
        }

        log.debug("Cache HIT for key: {} (remaining TTL: {}s)", key, entry.getRemainingTtlSeconds());
        return Optional.of(entry);
    }

    /**
     * Put an entry into the cache. Respects max cache size.
     */
    public void put(String key, CacheEntry entry) {
        if (!cacheConfig.isEnabled()) {
            return;
        }

        // Enforce max size - evict expired entries first if at capacity
        if (cache.size() >= cacheConfig.getMaxSize()) {
            evictExpired();
            if (cache.size() >= cacheConfig.getMaxSize()) {
                log.warn("Cache is full ({} entries), skipping cache for key: {}",
                        cache.size(), key);
                return;
            }
        }

        cache.put(key, entry);
        log.debug("Cached response for key: {} (TTL: {}s)", key, entry.getTtlSeconds());
    }

    /**
     * Evict a specific cache entry by key.
     */
    public void evict(String key) {
        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            log.debug("Evicted cache entry for key: {}", key);
        }
    }

    /**
     * Evict all cache entries whose keys match the given pattern.
     */
    public int evictByPattern(String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        int count = 0;

        for (String key : cache.keySet()) {
            if (pattern.matcher(key).matches()) {
                cache.remove(key);
                count++;
            }
        }

        log.info("Evicted {} cache entries matching pattern: {}", count, patternStr);
        return count;
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", cache.size());
        stats.put("maxSize", cacheConfig.getMaxSize());
        stats.put("enabled", cacheConfig.isEnabled());
        stats.put("defaultTtlSeconds", cacheConfig.getDefaultTtlSeconds());

        long activeEntries = cache.values().stream()
                .filter(entry -> !entry.isExpired())
                .count();
        long expiredEntries = cache.size() - activeEntries;

        stats.put("activeEntries", activeEntries);
        stats.put("expiredEntries", expiredEntries);

        // Route breakdown
        Map<String, Long> routeBreakdown = cache.keySet().stream()
                .collect(Collectors.groupingBy(
                        key -> key.split("\\|")[0],
                        Collectors.counting()));
        stats.put("routeBreakdown", routeBreakdown);

        return stats;
    }

    /**
     * Scheduled task to evict expired entries periodically.
     */
    @Scheduled(fixedDelayString = "${gateway.cache.eviction-interval-seconds:30}000")
    public void evictExpired() {
        int evicted = 0;
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                evicted++;
            }
        }

        if (evicted > 0) {
            log.info("Evicted {} expired cache entries. Remaining: {}", evicted, cache.size());
        }
    }

    /**
     * Clear the entire cache.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        log.info("Cleared entire cache ({} entries)", size);
    }

}
