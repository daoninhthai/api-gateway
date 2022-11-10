package com.daoninhthai.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "oauth2")
@Data
public class OAuth2Config {

    private String introspectionUri = "http://localhost:9000/oauth2/introspect";

    private String clientId = "api-gateway-client";

    private String clientSecret = "gateway-secret";

    private String issuerUri = "http://localhost:9000";

    private int connectTimeout = 5000;

    private int readTimeout = 5000;

    @Bean(name = "oauth2WebClient")
    public WebClient oauth2WebClient() {
        return WebClient.builder()
                .baseUrl(introspectionUri)
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
    }

}
