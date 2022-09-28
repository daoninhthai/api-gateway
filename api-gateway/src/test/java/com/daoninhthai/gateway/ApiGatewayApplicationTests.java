package com.daoninhthai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify that the Spring application context loads correctly.
 * This test ensures all beans are properly configured and there are no circular dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the application context starts successfully
        // All beans, configurations, and auto-configurations should load without errors
    }

}
