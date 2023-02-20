package com.daoninhthai.gateway.service;

import com.daoninhthai.gateway.config.SwaggerConfig;
import com.daoninhthai.gateway.dto.SwaggerResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that aggregates Swagger/OpenAPI documentation from all
 * downstream microservices. Fetches /v3/api-docs from each service
 * via WebClient and merges them into a unified API specification.
 */
@Service
@Slf4j
public class SwaggerAggregatorService {

    private final SwaggerConfig swaggerConfig;
    private final WebClient webClient;
    private final RouteLocator routeLocator;

    public SwaggerAggregatorService(SwaggerConfig swaggerConfig,
                                    RouteLocator routeLocator) {
        this.swaggerConfig = swaggerConfig;
        this.routeLocator = routeLocator;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Get all configured Swagger resources.
     */
    public List<SwaggerResource> getSwaggerResources() {
        return swaggerConfig.toSwaggerResources();
    }

    /**
     * Fetch API documentation from a specific service.
     */
    public Mono<Map<String, Object>> fetchApiDocs(String serviceId) {
        return routeLocator.getRoutes()
                .filter(route -> route.getId().equals(serviceId))
                .next()
                .flatMap(route -> {
                    String serviceUri = route.getUri().toString();
                    String apiDocsUrl = serviceUri + swaggerConfig.getApiDocsPath();

                    log.debug("Fetching API docs from service: {} at {}", serviceId, apiDocsUrl);

                    return webClient.get()
                            .uri(apiDocsUrl)
                            .retrieve()
                            .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                            .timeout(Duration.ofSeconds(5))
                            .doOnError(e -> log.warn("Failed to fetch API docs from {}: {}",
                                    serviceId, e.getMessage()))
                            .onErrorReturn(new LinkedHashMap<>());
                })
                .defaultIfEmpty(new LinkedHashMap<>());
    }

    /**
     * Fetch and merge API documentation from all configured services.
     */
    public Mono<Map<String, Object>> getAggregatedApiDocs() {
        List<SwaggerResource> resources = getSwaggerResources();

        return Flux.fromIterable(resources)
                .flatMap(resource -> {
                    String serviceId = extractServiceId(resource.getUrl());
                    return fetchApiDocs(serviceId)
                            .map(docs -> {
                                Map<String, Object> entry = new LinkedHashMap<>();
                                entry.put("service", resource.getName());
                                entry.put("docs", docs);
                                return entry;
                            });
                })
                .collectList()
                .map(this::mergeApiDocs);
    }

    /**
     * Merge API docs from multiple services into a single specification.
     */
    private Map<String, Object> mergeApiDocs(List<Map<String, Object>> serviceDocsList) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("openapi", "3.0.1");

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "API Gateway - Aggregated Documentation");
        info.put("description", "Aggregated API documentation from all microservices");
        info.put("version", "1.0.0");
        merged.put("info", info);

        Map<String, Object> allPaths = new LinkedHashMap<>();
        Map<String, Object> allSchemas = new LinkedHashMap<>();

        for (Map<String, Object> serviceEntry : serviceDocsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> docs = (Map<String, Object>) serviceEntry.get("docs");
            if (docs == null || docs.isEmpty()) {
                continue;
            }

            // Merge paths
            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) docs.get("paths");
            if (paths != null) {
                allPaths.putAll(paths);
            }

            // Merge schemas from components
            @SuppressWarnings("unchecked")
            Map<String, Object> components = (Map<String, Object>) docs.get("components");
            if (components != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
                if (schemas != null) {
                    allSchemas.putAll(schemas);
                }
            }
        }

        merged.put("paths", allPaths);

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("schemas", allSchemas);
        merged.put("components", components);

        return merged;
    }

    private String extractServiceId(String url) {
        // Extract service ID from URL pattern like /api/users/v3/api-docs
        String[] parts = url.split("/");
        if (parts.length >= 3) {
            return parts[2] + "-service";
        }
        return "unknown-service";
    }

}
