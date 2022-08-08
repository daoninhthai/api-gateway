package com.daoninhthai.gateway.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Custom health indicator that checks the health of downstream microservices.
 * Uses WebClient to call the /actuator/health endpoint of each registered service.
 */
@Slf4j
public class ServiceHealthIndicator extends AbstractHealthIndicator {

    private final String serviceName;
    private final String serviceUrl;
    private final WebClient webClient;
    private final Duration timeout;

    public ServiceHealthIndicator(String serviceName, String serviceUrl, WebClient webClient) {
        this(serviceName, serviceUrl, webClient, Duration.ofSeconds(5));
    }

    public ServiceHealthIndicator(String serviceName, String serviceUrl, WebClient webClient, Duration timeout) {
        super("Health check failed for service: " + serviceName);
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
        this.webClient = webClient;
        this.timeout = timeout;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            String healthUrl = serviceUrl + "/actuator/health";
            log.debug("Checking health of service '{}' at: {}", serviceName, healthUrl);

            ServiceHealthResponse response = webClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .bodyToMono(ServiceHealthResponse.class)
                    .timeout(timeout)
                    .onErrorResume(ex -> {
                        log.warn("Health check failed for service '{}': {}", serviceName, ex.getMessage());
                        return Mono.just(new ServiceHealthResponse("DOWN"));
                    })
                    .block();

            if (response != null && "UP".equalsIgnoreCase(response.getStatus())) {
                builder.up()
                        .withDetail("service", serviceName)
                        .withDetail("url", serviceUrl)
                        .withDetail("status", response.getStatus());
            } else {
                builder.down()
                        .withDetail("service", serviceName)
                        .withDetail("url", serviceUrl)
                        .withDetail("status", response != null ? response.getStatus() : "UNKNOWN")
                        .withDetail("error", "Service is not responding or reported DOWN status");
            }
        } catch (Exception e) {
            log.error("Error checking health of service '{}': {}", serviceName, e.getMessage());
            builder.down()
                    .withDetail("service", serviceName)
                    .withDetail("url", serviceUrl)
                    .withDetail("error", e.getMessage());
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Simple POJO to deserialize the health response from downstream services.
     */
    public static class ServiceHealthResponse {
        private String status;

        public ServiceHealthResponse() {
        }

        public ServiceHealthResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
