package com.daoninhthai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that handles API versioning by rewriting paths.
 * /api/v1/users/** -> /api/users/** with X-API-Version: v1
 * /api/v2/users/** -> /api/users/** with X-API-Version: v2
 */
@Component
@Slf4j
public class ApiVersionFilter extends AbstractGatewayFilterFactory<ApiVersionFilter.Config> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("/api/(v\\d+)/(.+)");
    private static final String API_VERSION_HEADER = "X-API-Version";

    public ApiVersionFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String originalPath = request.getPath().value();

            Matcher matcher = VERSION_PATTERN.matcher(originalPath);
            if (matcher.matches()) {
                String version = matcher.group(1);
                String remainingPath = matcher.group(2);

                // Rewrite path to remove version prefix
                String newPath = "/api/" + remainingPath;

                log.debug("API version rewrite: {} -> {} (version: {})", originalPath, newPath, version);

                URI newUri = UriComponentsBuilder.fromUri(request.getURI())
                        .replacePath(newPath)
                        .build()
                        .toUri();

                ServerHttpRequest modifiedRequest = request.mutate()
                        .uri(newUri)
                        .header(API_VERSION_HEADER, version)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuration properties for version filter
    }

}
