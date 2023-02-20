package com.daoninhthai.gateway.config;

import com.daoninhthai.gateway.dto.SwaggerResource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for aggregating Swagger/OpenAPI documentation
 * from all downstream microservices through the gateway.
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.swagger")
@Data
public class SwaggerConfig {

    /**
     * Whether Swagger aggregation is enabled.
     */
    private boolean enabled = true;

    /**
     * The path pattern for fetching API docs from downstream services.
     */
    private String apiDocsPath = "/v3/api-docs";

    /**
     * List of service definitions for Swagger aggregation.
     */
    private List<ServiceDefinition> services = new ArrayList<>();

    @PostConstruct
    public void initDefaults() {
        if (services.isEmpty()) {
            // Register default services based on common route configuration
            services.add(new ServiceDefinition("user-service", "User Service",
                    "/api/users", "3.0"));
            services.add(new ServiceDefinition("product-service", "Product Service",
                    "/api/products", "3.0"));
            services.add(new ServiceDefinition("order-service", "Order Service",
                    "/api/orders", "3.0"));
            services.add(new ServiceDefinition("auth-service", "Auth Service",
                    "/api/auth", "3.0"));
        }
    }

    /**
     * Convert service definitions to SwaggerResource list.
     */
    public List<SwaggerResource> toSwaggerResources() {
        List<SwaggerResource> resources = new ArrayList<>();
        for (ServiceDefinition service : services) {
            resources.add(SwaggerResource.builder()
                    .name(service.getDisplayName())
                    .url(service.getBasePath() + apiDocsPath)
                    .swaggerVersion(service.getSwaggerVersion())
                    .location(service.getBasePath())
                    .build());
        }
        return resources;
    }

    @Data
    public static class ServiceDefinition {

        private String serviceId;
        private String displayName;
        private String basePath;
        private String swaggerVersion;

        public ServiceDefinition() {
        }

        public ServiceDefinition(String serviceId, String displayName,
                                 String basePath, String swaggerVersion) {
            this.serviceId = serviceId;
            this.displayName = displayName;
            this.basePath = basePath;
            this.swaggerVersion = swaggerVersion;
        }
    }

}
