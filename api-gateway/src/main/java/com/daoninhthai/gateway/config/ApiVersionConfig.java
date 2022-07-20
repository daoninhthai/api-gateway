package com.daoninhthai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for API versioning.
 * Maps version identifiers to target service instances.
 */
@Configuration
@ConfigurationProperties(prefix = "gateway.api-version")
@Data
public class ApiVersionConfig {

    /**
     * Default API version when not specified
     */
    private String defaultVersion = "v1";

    /**
     * Supported API versions
     */
    private Map<String, String> supportedVersions = new HashMap<>() {{
        put("v1", "1.0");
        put("v2", "2.0");
    }};

    /**
     * Version-specific service mappings
     * e.g., v2 -> user-service-v2
     */
    private Map<String, Map<String, String>> serviceMapping = new HashMap<>();

    /**
     * Check if a version is supported
     */
    public boolean isVersionSupported(String version) {
        return supportedVersions.containsKey(version);
    }

    /**
     * Get the target service for a given version and service name
     * Falls back to the default service name if no version-specific mapping exists
     */
    public String getTargetService(String version, String serviceName) {
        if (serviceMapping.containsKey(version)) {
            return serviceMapping.get(version).getOrDefault(serviceName, serviceName);
        }
        return serviceName;
    }

}
