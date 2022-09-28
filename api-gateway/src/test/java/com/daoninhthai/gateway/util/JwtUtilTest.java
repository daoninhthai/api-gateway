package com.daoninhthai.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JwtUtil.
 * Tests token generation, validation, claim extraction, and expiration handling.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Key signingKey;
    private static final String SECRET = "daoninhthai-secret-key-for-jwt-authentication-2022-must-be-long-enough";

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();

        // Use reflection to set the secret field and initialize the signing key
        Field secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, SECRET);

        // Call PostConstruct manually
        jwtUtil.init();

        signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String createToken(String username, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-456");
        claims.put("roles", Arrays.asList("ROLE_USER", "ROLE_ADMIN"));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(signingKey)
                .compact();
    }

    private String createExpiredToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "user-456");
        claims.put("roles", Arrays.asList("ROLE_USER"));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(signingKey)
                .compact();
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsername() {
        String token = createToken("daoninhthai", 3600000);

        String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("daoninhthai");
    }

    @Test
    @DisplayName("Should validate a valid token successfully")
    void shouldValidateValidToken() {
        String token = createToken("testuser", 3600000);

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        String token = createExpiredToken("testuser");

        boolean isValid = jwtUtil.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject a malformed token")
    void shouldRejectMalformedToken() {
        boolean isValid = jwtUtil.validateToken("this.is.not.a.valid.jwt.token");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject token signed with wrong key")
    void shouldRejectWrongKeyToken() {
        Key wrongKey = Keys.hmacShaKeyFor(
                "this-is-a-completely-different-secret-key-that-should-not-work-here".getBytes());

        String token = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        boolean isValid = jwtUtil.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract all claims from token")
    void shouldExtractAllClaims() {
        String token = createToken("claimuser", 3600000);

        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("claimuser");
        assertThat(claims.get("userId", String.class)).isEqualTo("user-456");

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpiration() {
        long expectedExpiration = System.currentTimeMillis() + 3600000;
        String token = createToken("testuser", 3600000);

        Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isNotNull();
        // Allow 5 second tolerance for test execution time
        assertThat(expiration.getTime()).isCloseTo(expectedExpiration, org.assertj.core.data.Offset.offset(5000L));
    }

    @Test
    @DisplayName("Should detect expired token via isTokenExpired")
    void shouldDetectExpiredToken() {
        String token = createExpiredToken("testuser");

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should detect non-expired token via isTokenExpired")
    void shouldDetectNonExpiredToken() {
        String token = createToken("testuser", 3600000);

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should reject empty token string")
    void shouldRejectEmptyToken() {
        boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract custom claims using extractClaim")
    void shouldExtractCustomClaimWithResolver() {
        String token = createToken("customuser", 3600000);

        String userId = jwtUtil.extractClaim(token, claims -> claims.get("userId", String.class));

        assertThat(userId).isEqualTo("user-456");
    }
}
