# API Gateway - Microservices Architecture

A production-ready API Gateway built with **Spring Cloud Gateway** for routing, filtering, and securing microservices. This gateway serves as the single entry point for all client requests, providing cross-cutting concerns like authentication, rate limiting, circuit breaking, and request tracing.

---

## Architecture

```
                                    +------------------+
                                    |  Eureka Server   |
                                    |    (Port 8761)   |
                                    +--------+---------+
                                             |
                                    Service Discovery
                                             |
+----------+       +---------+---------------+----------------+-----------+
|          |       |         |               |                |           |
|  Client  +------>+   API   |    +----------+----------+     |   Redis   |
|          |       | Gateway |    |          |          |     | (Port 6379)|
+----------+       | (:8080) +--->+  User    | Product  |     +-----------+
                   |         |    | Service  | Service  |
                   |  - JWT  |    | (:8081)  | (:8082)  |
                   |  - Rate |    +----------+----------+
                   |  Limit  |               |
                   |  - CORS |        +------+------+
                   |  - CB   |        |    Order    |
                   +---------+        |   Service   |
                                      |   (:8083)   |
                                      +-------------+
```

---

## Features

| Feature                    | Description                                                     |
|----------------------------|-----------------------------------------------------------------|
| Dynamic Routing            | Route requests to microservices via Eureka service discovery     |
| JWT Authentication         | Token-based authentication with role extraction and forwarding   |
| Rate Limiting              | Redis-backed rate limiting per client with configurable limits   |
| Circuit Breaker            | Resilience4j circuit breaker with fallback endpoints             |
| CORS Configuration         | Global cross-origin resource sharing configuration               |
| Health Check Aggregation   | Aggregated health checks for all downstream services             |
| API Versioning             | Support for versioned API routes (v1, v2, etc.)                  |
| Request Tracing            | Correlation ID propagation across services                       |
| Request/Response Filtering | Header transformation and security header injection              |
| Custom Error Handling      | Centralized error handling with structured error responses        |
| Docker Support             | Multi-stage Docker builds with docker-compose orchestration      |

---

## Tech Stack

| Technology                | Version    | Purpose                          |
|---------------------------|------------|----------------------------------|
| Java                      | 11         | Programming language             |
| Spring Boot               | 2.7.5      | Application framework            |
| Spring Cloud Gateway      | 2021.0.5   | API Gateway (reactive/WebFlux)   |
| Spring Cloud Netflix      | 2021.0.5   | Eureka service discovery         |
| Resilience4j              | -          | Circuit breaker                  |
| Redis                     | 7          | Rate limiting backend            |
| JJWT                      | 0.11.5     | JWT token handling               |
| Maven                     | 3.8+       | Build tool                       |
| Docker                    | 20+        | Containerization                 |
| Lombok                    | -          | Boilerplate reduction            |

---

## Project Structure

```
api-gateway-rebuild/
├── pom.xml                          # Parent POM (multi-module)
├── Dockerfile                       # API Gateway Docker image
├── docker-compose.yml               # Full stack orchestration
├── docker-compose.dev.yml           # Dev environment (Redis only)
│
├── api-gateway/                     # API Gateway module
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/daoninhthai/gateway/
│       │   │   ├── ApiGatewayApplication.java
│       │   │   ├── config/
│       │   │   │   ├── CorsConfig.java
│       │   │   │   ├── GatewayConfig.java
│       │   │   │   ├── RouteConfig.java
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── RateLimitConfig.java
│       │   │   │   ├── CircuitBreakerConfig.java
│       │   │   │   └── ApiVersionConfig.java
│       │   │   ├── filter/
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── LoggingFilter.java
│       │   │   │   ├── RateLimitKeyResolver.java
│       │   │   │   ├── RequestCorrelationFilter.java
│       │   │   │   ├── ApiVersionFilter.java
│       │   │   │   ├── RequestHeaderFilter.java
│       │   │   │   └── ResponseHeaderFilter.java
│       │   │   ├── health/
│       │   │   │   ├── ServiceHealthIndicator.java
│       │   │   │   └── GatewayHealthConfig.java
│       │   │   ├── controller/
│       │   │   │   └── FallbackController.java
│       │   │   ├── dto/
│       │   │   │   ├── AuthResponse.java
│       │   │   │   └── ErrorResponse.java
│       │   │   ├── exception/
│       │   │   │   ├── GatewayExceptionHandler.java
│       │   │   │   └── UnauthorizedException.java
│       │   │   └── util/
│       │   │       └── JwtUtil.java
│       │   └── resources/
│       │       └── application.yml
│       └── test/
│           └── java/com/daoninhthai/gateway/
│               ├── ApiGatewayApplicationTests.java
│               ├── filter/
│               │   └── JwtAuthenticationFilterTest.java
│               └── util/
│                   └── JwtUtilTest.java
│
└── eureka-server/                   # Eureka Server module
    ├── pom.xml
    ├── Dockerfile
    └── src/
        └── main/
            ├── java/com/daoninhthai/eureka/
            │   └── EurekaServerApplication.java
            └── resources/
                └── application.yml
```

---

## Prerequisites

- Java 11+
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- Redis (for rate limiting - can be started via docker-compose)

---

## Setup & Run

### 1. Local Development

**Start Redis (required for rate limiting):**

```bash
docker-compose -f docker-compose.dev.yml up -d
```

**Build the project:**

```bash
mvn clean install -DskipTests
```

**Start Eureka Server:**

```bash
cd eureka-server
mvn spring-boot:run
```

**Start API Gateway:**

```bash
cd api-gateway
mvn spring-boot:run
```

### 2. Docker Deployment

**Build and start all services:**

```bash
docker-compose up --build -d
```

**View logs:**

```bash
docker-compose logs -f api-gateway
```

**Stop all services:**

```bash
docker-compose down
```

---

## API Routes

| Method | Route               | Service         | Auth Required |
|--------|---------------------|-----------------|---------------|
| ANY    | `/api/auth/**`      | auth-service    | No            |
| ANY    | `/api/users/**`     | user-service    | Yes (JWT)     |
| ANY    | `/api/v1/users/**`  | user-service    | Yes (JWT)     |
| ANY    | `/api/v2/users/**`  | user-service    | Yes (JWT)     |
| ANY    | `/api/products/**`  | product-service | Yes (JWT)     |
| ANY    | `/api/orders/**`    | order-service   | Yes (JWT)     |

### Actuator Endpoints

| Endpoint                | Description                         |
|-------------------------|-------------------------------------|
| `/actuator/health`      | Aggregated health status            |
| `/actuator/info`        | Application information             |
| `/actuator/metrics`     | Application metrics                 |
| `/actuator/gateway`     | Gateway route information           |

---

## Configuration

Key configuration properties in `application.yml`:

| Property                        | Default   | Description                          |
|---------------------------------|-----------|--------------------------------------|
| `server.port`                   | 8080      | Gateway server port                  |
| `spring.redis.host`             | localhost | Redis host for rate limiting         |
| `spring.redis.port`             | 6379      | Redis port                           |
| `jwt.secret`                    | -         | JWT signing secret key               |
| `gateway.rate-limit.replenish-rate` | 10    | Requests per second allowed          |
| `gateway.rate-limit.burst-capacity` | 20    | Maximum burst request count          |

---

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -pl api-gateway -Dtest=JwtUtilTest

# Run with coverage
mvn test jacoco:report
```

---

## Docker

### Build Images

```bash
# Build API Gateway image
docker build -t api-gateway:latest .

# Build Eureka Server image
docker build -t eureka-server:latest -f eureka-server/Dockerfile .
```

### Environment Variables

| Variable                                     | Description                    |
|----------------------------------------------|--------------------------------|
| `SPRING_PROFILES_ACTIVE`                     | Active Spring profile          |
| `SPRING_REDIS_HOST`                          | Redis hostname                 |
| `SPRING_REDIS_PORT`                          | Redis port                     |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`      | Eureka server URL              |

---

## Author

**daoninhthai**

- GitHub: [daoninhthai](https://github.com/daoninhthai)
- Email: thaimeo1131@gmail.com
