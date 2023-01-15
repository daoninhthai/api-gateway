package com.daoninhthai.gateway.filter;

import com.daoninhthai.gateway.config.CacheConfig;
import com.daoninhthai.gateway.dto.CacheEntry;
import com.daoninhthai.gateway.service.CacheService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

/**
 * Gateway filter that caches GET responses in memory with configurable TTL.
 * Cache key is composed of: method + path + query parameters.
 * Only successful (2xx) GET responses are cached.
 */
@Component
@Slf4j
public class ResponseCacheFilter extends AbstractGatewayFilterFactory<ResponseCacheFilter.Config> {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheConfig cacheConfig;

    public ResponseCacheFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!cacheConfig.isEnabled()) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();

            // Only cache GET requests
            if (request.getMethod() != HttpMethod.GET) {
                return chain.filter(exchange);
            }

            String cacheKey = buildCacheKey(exchange);

            // Check if we have a cached response
            Optional<CacheEntry> cachedEntry = cacheService.get(cacheKey);
            if (cachedEntry.isPresent()) {
                log.debug("Serving cached response for: {}", cacheKey);
                return writeCachedResponse(exchange, cachedEntry.get());
            }

            // No cached response - proceed with request and cache the response
            ServerHttpResponse originalResponse = exchange.getResponse();
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();

            ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    HttpStatus statusCode = getStatusCode();

                    // Only cache successful responses
                    if (statusCode != null && statusCode.is2xxSuccessful()) {
                        return DataBufferUtils.join(Flux.from(body))
                                .flatMap(dataBuffer -> {
                                    byte[] content = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(content);
                                    DataBufferUtils.release(dataBuffer);

                                    // Determine TTL for this route
                                    long ttl = config.getTtlSeconds() > 0
                                            ? config.getTtlSeconds()
                                            : getRouteTtl(exchange);

                                    // Store in cache
                                    CacheEntry entry = CacheEntry.builder()
                                            .body(content)
                                            .headers(getHeaders())
                                            .statusCode(statusCode)
                                            .cachedAt(Instant.now())
                                            .ttlSeconds(ttl)
                                            .build();
                                    cacheService.put(cacheKey, entry);

                                    // Write the response body
                                    DataBuffer buffer = bufferFactory.wrap(content);
                                    return super.writeWith(Mono.just(buffer));
                                });
                    }

                    return super.writeWith(body);
                }
            };

            return chain.filter(exchange.mutate().response(decoratedResponse).build());
        };
    }

    /**
     * Build a cache key from the request method, path, and query parameters.
     */
    private String buildCacheKey(org.springframework.web.server.ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        String path = request.getPath().value();
        String query = request.getURI().getRawQuery();

        return method + "|" + path + (query != null ? "?" + query : "");
    }

    /**
     * Get TTL for the current route from configuration.
     */
    private long getRouteTtl(org.springframework.web.server.ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            return cacheConfig.getTtlForRoute(route.getId());
        }
        return cacheConfig.getDefaultTtlSeconds();
    }

    /**
     * Write a cached response back to the client.
     */
    private Mono<Void> writeCachedResponse(org.springframework.web.server.ServerWebExchange exchange,
                                           CacheEntry entry) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(entry.getStatusCode());

        // Copy cached headers
        if (entry.getHeaders() != null) {
            entry.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("Transfer-Encoding")) {
                    response.getHeaders().put(key, values);
                }
            });
        }

        // Add cache-related headers
        response.getHeaders().add("X-Cache", "HIT");
        response.getHeaders().add("X-Cache-TTL", String.valueOf(entry.getRemainingTtlSeconds()));

        DataBuffer buffer = response.bufferFactory().wrap(entry.getBody());
        return response.writeWith(Mono.just(buffer));
    }

    @Data
    public static class Config {
        /**
         * TTL in seconds for this specific route. 0 means use the global default.
         */
        private long ttlSeconds = 0;
    }

}
