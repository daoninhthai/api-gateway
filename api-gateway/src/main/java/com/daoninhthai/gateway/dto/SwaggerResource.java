package com.daoninhthai.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Swagger/OpenAPI resource from a downstream microservice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerResource {

    /**
     * Display name of the service (e.g., "User Service").
     */
    private String name;

    /**
     * URL path to the service's API documentation.
     */
    private String url;

    /**
     * Swagger/OpenAPI version (e.g., "3.0", "2.0").
     */
    private String swaggerVersion;

    /**
     * The service's base path in the gateway.
     */
    private String location;

}
