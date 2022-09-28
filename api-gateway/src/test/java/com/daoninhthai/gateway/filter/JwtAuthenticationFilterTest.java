package com.daoninhthai.gateway.filter;

import com.daoninhthai.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JwtAuthenticationFilter.
 * Verifies JWT token validation, expiration handling, and unauthorized access prevention.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private GatewayFilter filter;
    private Key signingKey;
    private static final String SECRET = "daoninhthai-secret-key-for-jwt-authentication-2022-must-be-long-enough";

    @BeforeEach
    void setUp() {
        filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String generateValidToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-123");
        claims.put("roles", Arrays.asList("ROLE_USER"));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(signingKey)
                .compact();
    }

    private String generateExpiredToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-123");
        claims.put("roles", Arrays.asList("ROLE_USER"));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Valid JWT token should pass through filter successfully")
    void validTokenShouldPass() {
        String token = generateValidToken("testuser");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> {
            // Verify user info headers are added
            ServerWebExchange mutatedExchange = ex;
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-User"))
                    .isEqualTo("testuser");
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Auth-UserId"))
                    .isEqualTo("user-123");
            return Mono.empty();
        });

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Invalid JWT token should return 401 Unauthorized")
    void invalidTokenShouldReturn401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> Mono.empty());

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Expired JWT token should return 401 Unauthorized")
    void expiredTokenShouldReturn401() {
        String token = generateExpiredToken("testuser");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> Mono.empty());

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Missing Authorization header on protected route should return 401")
    void missingTokenShouldReturn401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> Mono.empty());

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Missing Bearer prefix should return 401 Unauthorized")
    void missingBearerPrefixShouldReturn401() {
        String token = generateValidToken("testuser");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, token) // Missing "Bearer " prefix
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> Mono.empty());

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token signed with wrong key should return 401 Unauthorized")
    void wrongKeyTokenShouldReturn401() {
        Key wrongKey = Keys.hmacShaKeyFor(
                "this-is-a-completely-different-secret-key-that-should-not-work-at-all".getBytes());

        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/users/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<Void> result = filter.filter(exchange, ex -> Mono.empty());

        StepVerifier.create(result)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
