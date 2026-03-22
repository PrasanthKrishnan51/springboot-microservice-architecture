<div align="center">

# 🛒 E-Commerce Microservices

**Production-ready microservices platform built with Spring Boot 3.3.5 & Java 21**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?style=flat-square&logo=mongodb)](https://www.mongodb.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-231F20?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

*API Gateway · User Service · Product Service · Order Service*

</div>

---

## 📐 Architecture Overview

```
                          ┌─────────────────────────────────────────────┐
                          │             API Gateway  :8080               │
Client ──────────────────►│  JWT Filter · Rate Limiting · Circuit Breaker│
                          └──────────┬──────────┬──────────┬────────────┘
                                     │          │          │
                          ┌──────────▼─┐ ┌──────▼──────┐ ┌▼────────────┐
                          │User Service│ │Product Svc  │ │ Order Svc   │
                          │   :8081    │ │   :8082     │ │   :8083     │
                          └──────┬─────┘ └──────┬──────┘ └──────┬──────┘
                                 │              │               │
                          ┌──────▼─────┐ ┌──────▼──────┐ ┌─────▼───────┐
                          │  user_db   │ │ product_db  │ │  order_db   │
                          │  MongoDB   │ │   MongoDB   │ │   MongoDB   │
                          └────────────┘ └──────┬──────┘ └──────┬──────┘
                                                │               │
                                       ┌────────▼───────────────▼───────┐
                                       │           Apache Kafka          │
                                       │  product-events · order-events  │
                                       └────────────────────────────────┘

        ┌────────────────────────────────────────────────────┐
        │  Service Discovery: Eureka :8761                   │
        │  Distributed Tracing: Micrometer + Zipkin :9411   │
        │  Metrics: Prometheus :9090 + Grafana :3000         │
        └────────────────────────────────────────────────────┘
```

**Order Service** uses **OpenFeign** to call Product Service for real-time stock checks during checkout.

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.5 |
| **Cloud** | Spring Cloud 2023.0.3 (Gateway, Eureka, OpenFeign, Resilience4j) |
| **Database** | MongoDB 7.0 — one database per service |
| **Messaging** | Apache Kafka (`product-events`, `order-events`) |
| **Tracing** | Micrometer + Zipkin |
| **Metrics** | Prometheus + Grafana |
| **Logging** | Logback → Logstash → Elasticsearch → Kibana |

---

## 🚀 Services

| Service | Port | Responsibility |
|---|---|---|
| **API Gateway** | `8080` | JWT auth, routing, circuit breakers (Resilience4j), Redis rate limiting |
| **User Service** | `8081` | Register / login, JWT issuance, user profiles |
| **Product Service** | `8082` | Catalogue, inventory management, Kafka producer |
| **Order Service** | `8083` | Checkout, OpenFeign → Product stock check, Kafka producer + consumer |

---

## ⚡ Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21
- Maven 3.9+

### Run the stack

```bash
# 1. Configure secrets
cp .env.example .env

# 2. Build all service JARs
mvn clean package -DskipTests

# 3. Start everything
docker compose up -d

# 4. Register a user
curl -X POST http://localhost:8080/api/v1/users/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'

# 5. Explore logs in Kibana
#    → http://localhost:5601 → Discover → create index pattern: ecommerce-*
```

---

## 📨 Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `product-events` | Product Service | Order Service |
| `order-events` | Order Service | Notification Service *(future)* |

---

## 📊 Observability

| Tool | URL | Credentials |
|---|---|---|
| **Kibana** | http://localhost:5601 | — |
| **Grafana** | http://localhost:3000 | `admin / admin` |
| **Zipkin** | http://localhost:9411 | — |
| **Prometheus** | http://localhost:9090 | — |
| **Kafka UI** | http://localhost:9093 | — |
| **Eureka Dashboard** | http://localhost:8761 | — |

---

## 🪵 ELK Log Pipeline

```
Service (Logback + LogstashEncoder)
  └─► LogstashTcpSocketAppender  →  TCP :5000
        └─► Logstash pipeline
              └─► Elasticsearch  (index: {service}-YYYY.MM.dd)
                    └─► Kibana
```

Every log line carries the following fields:

`@timestamp` · `service` · `env` · `level` · `traceId` · `spanId` · `correlationId` · `userId`

---

## 🌐 API Gateway Features

- **JWT Validation** — stateless token verification against the User Service public key
- **Rate Limiting** — Redis token-bucket algorithm per client IP
- **Circuit Breakers** — Resilience4j with configurable thresholds and fallbacks
- **Routing** — Spring Cloud Gateway route predicates and filters

---

## 📁 Project Structure

```
ecommerce-microservices/
├── api-gateway/          # Spring Cloud Gateway + JWT filter
├── user-service/         # Auth, JWT issuance, user profiles
├── product-service/      # Catalogue, inventory, Kafka producer
├── order-service/        # Checkout, Feign client, Kafka consumer
├── docker/
│   └── logstash/         # Logstash pipeline config
├── docker-compose.yml    # Full stack (services + infra)
├── .env.example          # Environment variable template
└── pom.xml               # Parent POM
```

---

## 🔐 Environment Variables

Copy `.env.example` to `.env` and fill in the required values:

```env
# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRY_MS=86400000

# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=changeme

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

## 📜 License

Distributed under the [MIT License](LICENSE).

---

<div align="center">

Built with ☕ Java · 🍃 Spring · 🍃 MongoDB · ⚡ Kafka

</div>