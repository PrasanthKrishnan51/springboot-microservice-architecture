# E-Commerce Microservices — Spring Boot 3.3.5

Production-ready microservices: API Gateway · User · Product · Order

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Cloud | Spring Cloud 2023.0.3 (Gateway, Eureka, OpenFeign, Resilience4j) |
| Database | MongoDB 7.0 (per-service) |
| Messaging | Apache Kafka (product-events, order-events) |
| Tracing | Micrometer + Zipkin |
| Metrics | Prometheus + Grafana |
| Logging | Logback → Logstash → Elasticsearch → Kibana |

## Services & Ports

| Service        | Port | Role |
|----------------|------|------|
| API Gateway    | 8080 | JWT auth, routing, circuit breakers, rate limiting |
| User Service   | 8081 | Register / login (issues JWT), profiles |
| Product Service| 8082 | Catalogue, inventory, Kafka producer |
| Order Service  | 8083 | Checkout, Feign → Product, Kafka producer + consumer |

## Observability UIs

| Tool       | URL                      | Login        |
|------------|--------------------------|--------------|
| Kibana     | http://localhost:5601    | —            |
| Grafana    | http://localhost:3000    | admin/admin  |
| Zipkin     | http://localhost:9411    | —            |
| Prometheus | http://localhost:9090    | —            |
| Kafka UI   | http://localhost:9093    | —            |
| Eureka     | http://localhost:8761    | —            |

## ELK Log Flow

```
Service (Logback + LogstashEncoder)
  └─► LogstashTcpSocketAppender  →  TCP :5000
        └─► Logstash pipeline
              └─► Elasticsearch  (index: {service}-YYYY.MM.dd)
                    └─► Kibana
```

Every log line carries: `@timestamp`, `service`, `env`, `level`, `traceId`, `spanId`, `correlationId`, `userId`

## Quick Start

```bash
# 1. Set secrets
cp .env.example .env

# 2. Build all jars
mvn clean package -DskipTests

# 3. Start stack
docker compose up -d

# 4. Try it out
curl -X POST http://localhost:8080/api/v1/users/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@example.com","password":"password123","firstName":"John","lastName":"Doe"}'

# 5. View logs in Kibana
#    → http://localhost:5601 → Discover → create index: ecommerce-*
```

## Kafka Topics

| Topic           | Producer        | Consumer       |
|-----------------|-----------------|----------------|
| product-events  | product-service | order-service  |
| order-events    | order-service   | (notifications)|

## Architecture

```
Client
  │
  ▼
API Gateway :8080
  ├── JWT filter (validates token from user-service)
  ├── Rate limiting (Redis token bucket)
  ├── Circuit breakers (Resilience4j)
  │
  ├─► User Service    :8081  ─── MongoDB (user_db)
  ├─► Product Service :8082  ─── MongoDB (product_db) ──► Kafka (product-events)
  └─► Order Service   :8083  ─── MongoDB (order_db)
           │  Feign (REST)               ▲
           └─────────────────────────────┘  (stock check)
           └──────────────────► Kafka (order-events)
```
