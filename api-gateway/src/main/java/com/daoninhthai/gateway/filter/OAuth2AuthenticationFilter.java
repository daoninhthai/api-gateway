package com.daoninhthai.gateway.filter;

import com.daoninhthai.gateway.config.OAuth2Config;
import com.daoninhthai.gateway.dto.TokenIntrospectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * OAuth2 Authentication Filter that validates bearer tokens
 * by calling the OAuth2 token introspection endpoint.
 * Uses non-blocking WebClient for reactive token validation.
 */
@Component
@Slf4j
public class OAuth2AuthenticationFilter extends AbstractGatewayFilterFactory<OAuth2AuthenticationFilter.Config> {

    @Autowired
    @Qualifier("oauth2WebClient")
    private WebClient oauth2WebClient;

    @Autowired
    private OAuth2Config oauth2Config;

    public OAuth2AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Extract Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format. Expected: Bearer <token>",
                        HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Call introspection endpoint to validate token
            return introspectToken(token)
                    .flatMap(introspectionResponse -> {
                        if (!introspectionResponse.isActive()) {
                            log.warn("OAuth2 token is not active");
                            return onError(exchange, "Token is not active or has expired",
                                    HttpStatus.UNAUTHORIZED);
                        }

                        // Check token expiration
                        if (introspectionResponse.getExp() != null) {
                            long now = System.currentTimeMillis() / 1000;
                            if (introspectionResponse.getExp() < now) {
                                log.warn("OAuth2 token has expired for subject: {}",
                                        introspectionResponse.getSubject());
                                return onError(exchange, "Token has expired", HttpStatus.UNAUTHORIZED);
                            }
                        }

                        // Add authenticated user info to headers for downstream services
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-OAuth2-Subject", safeString(introspectionResponse.getSubject()))
                                .header("X-OAuth2-ClientId", safeString(introspectionResponse.getClientId()))
                                .header("X-OAuth2-Scope", safeString(introspectionResponse.getScope()))
                                .header("X-Auth-Type", "oauth2")
                                .build();

                        log.debug("OAuth2 token validated for subject: {}, client: {}",
                                introspectionResponse.getSubject(),
                                introspectionResponse.getClientId());

                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    })
                    .onErrorResume(ex -> {
                        log.error("Error during OAuth2 token introspection: {}", ex.getMessage());
                        return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    /**
     * Calls the OAuth2 token introspection endpoint using WebClient (non-blocking).
     */
    private Mono<TokenIntrospectionResponse> introspectToken(String token) {
        return oauth2WebClient.post()
                .uri(oauth2Config.getIntrospectionUri())
                .headers(headers -> headers.setBasicAuth(
                        oauth2Config.getClientId(),
                        oauth2Config.getClientSecret()))
                .body(BodyInserters.fromFormData("token", token))
                .retrieve()
                .bodyToMono(TokenIntrospectionResponse.class)
                .doOnError(e -> log.error("Token introspection request failed: {}", e.getMessage()));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        log.error("OAuth2 authentication error: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    public static class Config {
        // Configuration properties for OAuth2 filter
    }

}
