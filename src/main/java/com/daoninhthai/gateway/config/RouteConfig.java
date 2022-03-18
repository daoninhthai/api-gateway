package com.daoninhthai.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri("lb://user-service"))

                // Product Service
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .uri("lb://product-service"))

                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .uri("lb://order-service"))

                // Auth Service
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))

                .build();
    }

}
