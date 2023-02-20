package com.daoninhthai.gateway.controller;

import com.daoninhthai.gateway.dto.SwaggerResource;
import com.daoninhthai.gateway.service.SwaggerAggregatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Controller that exposes aggregated Swagger/OpenAPI documentation
 * from all downstream microservices through the gateway.
 */
@RestController
@Slf4j
public class SwaggerController {

    private final SwaggerAggregatorService swaggerAggregatorService;

    public SwaggerController(SwaggerAggregatorService swaggerAggregatorService) {
        this.swaggerAggregatorService = swaggerAggregatorService;
    }

    /**
     * Returns a list of all available API documentation resources
     * from downstream services.
     */
    @GetMapping("/swagger-resources")
    public ResponseEntity<List<SwaggerResource>> getSwaggerResources() {
        log.debug("Fetching swagger resources for all services");
        List<SwaggerResource> resources = swaggerAggregatorService.getSwaggerResources();
        return ResponseEntity.ok(resources);
    }

    /**
     * Returns the aggregated OpenAPI specification from all services.
     */
    @GetMapping("/v3/api-docs/aggregated")
    public Mono<ResponseEntity<Map<String, Object>>> getAggregatedApiDocs() {
        log.debug("Fetching aggregated API documentation");
        return swaggerAggregatorService.getAggregatedApiDocs()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Returns the API documentation for a specific service.
     */
    @GetMapping("/v3/api-docs/{serviceId}")
    public Mono<ResponseEntity<Map<String, Object>>> getServiceApiDocs(
            @PathVariable String serviceId) {
        log.debug("Fetching API documentation for service: {}", serviceId);
        return swaggerAggregatorService.fetchApiDocs(serviceId)
                .map(docs -> {
                    if (docs.isEmpty()) {
                        return ResponseEntity.notFound().<Map<String, Object>>build();
                    }
                    return ResponseEntity.ok(docs);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

}
