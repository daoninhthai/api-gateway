package com.daoninhthai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway filter that transforms request headers before forwarding to downstream services.
 * <p>
 * This filter performs two key operations:
 * 1. Adds the X-Gateway-Source header to identify requests routed through the gateway
 * 2. Removes internal/sensitive headers that should not be forwarded to downstream services
 */
@Component
@Slf4j
public class RequestHeaderFilter extends AbstractGatewayFilterFactory<RequestHeaderFilter.Config> {

    private static final String GATEWAY_SOURCE_HEADER = "X-Gateway-Source";
    private static final String GATEWAY_SOURCE_VALUE = "api-gateway";

    /**
     * List of internal headers that should be stripped before forwarding
     * to downstream services. These headers are used internally by the gateway
     * and should not be leaked to backend services or manipulated by clients.
     */
    private static final List<String> INTERNAL_HEADERS_TO_REMOVE = Arrays.asList(
            "X-Internal-Token",
            "X-Internal-Request-Id",
            "X-Internal-Trace",
            "X-Debug-Mode",
            "X-Gateway-Secret",
            "X-Forwarded-Access-Token"
    );

    public RequestHeaderFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().value();

            log.debug("RequestHeaderFilter: Processing request to {}", requestPath);

            // Build the modified request with added/removed headers
            ServerHttpRequest.Builder requestBuilder = request.mutate();

            // Add gateway source identification header
            requestBuilder.header(GATEWAY_SOURCE_HEADER, GATEWAY_SOURCE_VALUE);

            // Add request timestamp header
            requestBuilder.header("X-Gateway-Timestamp", String.valueOf(System.currentTimeMillis()));

            // Remove internal headers that should not be forwarded
            for (String headerName : INTERNAL_HEADERS_TO_REMOVE) {
                if (request.getHeaders().containsKey(headerName)) {
                    log.debug("RequestHeaderFilter: Removing internal header '{}' from request to {}",
                            headerName, requestPath);
                    requestBuilder.headers(headers -> headers.remove(headerName));
                }
            }

            // Remove any headers specified in the config
            if (config.getAdditionalHeadersToRemove() != null) {
                for (String headerName : config.getAdditionalHeadersToRemove()) {
                    requestBuilder.headers(headers -> headers.remove(headerName));
                }
            }

            ServerHttpRequest modifiedRequest = requestBuilder.build();

            log.debug("RequestHeaderFilter: Request headers transformed for {}", requestPath);

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        private List<String> additionalHeadersToRemove;

        public Config() {
        }

        public List<String> getAdditionalHeadersToRemove() {
            return additionalHeadersToRemove;
        }

        public void setAdditionalHeadersToRemove(List<String> additionalHeadersToRemove) {
            this.additionalHeadersToRemove = additionalHeadersToRemove;
        }
    }
}
