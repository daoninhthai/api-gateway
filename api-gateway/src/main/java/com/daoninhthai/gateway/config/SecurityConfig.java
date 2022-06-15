package com.daoninhthai.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    // Public endpoints that don't require JWT authentication
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/actuator/health",
            "/actuator/info"
    );

    // Admin-only endpoints
    private static final List<String> ADMIN_ENDPOINTS = Arrays.asList(
            "/api/users/admin/**",
            "/api/products/admin/**",
            "/api/orders/admin/**"
    );

    @Bean
    public List<String> publicEndpoints() {
        return PUBLIC_ENDPOINTS;
    }

    @Bean
    public List<String> adminEndpoints() {
        return ADMIN_ENDPOINTS;
    }

    /**
     * Check if the given path is a public endpoint
     */
    public boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint -> {
            if (endpoint.endsWith("/**")) {
                String prefix = endpoint.substring(0, endpoint.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(endpoint);
        });
    }

    /**
     * Check if the given path requires admin role
     */
    public boolean isAdminEndpoint(String path) {
        return ADMIN_ENDPOINTS.stream().anyMatch(endpoint -> {
            if (endpoint.endsWith("/**")) {
                String prefix = endpoint.substring(0, endpoint.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(endpoint);
        });
    }

}
