package com.daoninhthai.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Represents a cached HTTP response entry with TTL support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheEntry {

    private byte[] body;

    private HttpHeaders headers;

    private HttpStatus statusCode;

    private Instant cachedAt;

    private long ttlSeconds;

    /**
     * Check if this cache entry has expired based on its TTL.
     */
    public boolean isExpired() {
        if (cachedAt == null) {
            return true;
        }
        return Instant.now().isAfter(cachedAt.plusSeconds(ttlSeconds));
    }

    /**
     * Get the remaining TTL in seconds.
     */
    public long getRemainingTtlSeconds() {
        if (cachedAt == null || isExpired()) {
            return 0;
        }
        return ttlSeconds - (Instant.now().getEpochSecond() - cachedAt.getEpochSecond());
    }

}
