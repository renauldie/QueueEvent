# 🎟️ QueueEvent — Distributed Event Reservation Application

QueueEvent is an enterprise-grade, high-throughput, event-driven ticketing platform designed to handle massive flash-sale traffic spikes without overselling seats. Built with **Spring Boot 3**, **Spring Cloud Gateway**, **Apache Kafka**, **Redis**, **PostgreSQL**, and **Elasticsearch**, using a **Database-per-Service** and **Feature-First (DDD)** architecture.

---

## 🏛️ System Architecture

QueueEvent is organized into an API Gateway and independent microservices:

```
                          [ Client / Front-End ]
                                    │
                                    ▼
                     ┌─────────────────────────────┐
                     │   API Gateway (:8080)       │
                     │  - JWT Authentication       │
                     │  - Redis Rate Limiter (20/s)│
                     │  - Dynamic Path Routing     │
                     └──────────────┬──────────────┘
                                    │
            ┌───────────────────────┴───────────────────────┐
            │ /api/v1/auth/**                               │ /api/v1/events/**
            │ /api/v1/users/**                              │ /api/v1/reservations/**
            ▼                                               │ /api/v1/orders/**
┌─────────────────────────────┐                             ▼
│    User Service (:8082)     │              ┌─────────────────────────────┐
│  - User Auth & JWT Token    │              │    Event Service (:8081)    │
│  - Profile Management       │              │  - Event & Seat Lifecycle   │
│  - BCrypt Encryption        │              │  - Redisson Distributed Lock│
└──────────────┬──────────────┘              │  - 10-Min Reservation Hold  │
               │                             │  - Transactional Outbox     │
               ▼                             └──────────────┬──────────────┘
    ┌─────────────────────┐                                 │
    │  queue_event_user_db│                   ┌─────────────┴─────────────┐
    │    (PostgreSQL)     │                   ▼                           ▼
    └─────────────────────┘        ┌─────────────────────┐     ┌─────────────────────┐
                                   │    queue_event_db   │     │   Redis (:6379)     │
                                   │    (PostgreSQL)     │     │ - Distributed Lock  │
                                   └─────────────────────┘     │ - Idempotency Cache │
                                              │                └─────────────────────┘
                                              ▼
                                   ┌─────────────────────┐
                                   │  Outbox Publisher   │
                                   └──────────┬──────────┘
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │   Apache Kafka      │
                                   │   - order-events    │
                                   │   - Retry & DLQ     │
                                   └──────────┬──────────┘
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │ Elasticsearch (:9200│
                                   │ - Full-text Search  │
                                   └─────────────────────┘
```

---

## 📦 Services Overview

| Service Name | Port | Base Package | Database | Description |
| :--- | :---: | :--- | :--- | :--- |
| **`api-gateway`** | `8080` | `com.renauldiedev.gateway` | Redis (Cache/Limiter) | Central entry point, JWT validation, Redis token-bucket rate limiter. |
| **`event-service`** | `8081` | `com.renauldiedev.event` | `queue_event_db` | Core domain: Events, Seat Tiers, Outbox pattern, Kafka event streaming, Elasticsearch syncing. |
| **`user-service`** | `8082` | `com.renauldiedev.user` | `queue_event_user_db` | Identity & Access: User registration, login, JWT issuance, profile lookup. |

---

## 🚀 Key Architectural Patterns

1. **Database-per-Service**: `user-service` and `event-service` have completely isolated PostgreSQL databases (`queue_event_user_db` and `queue_event_db`). Orders reference `userId` via token context (`X-User-Id`), ensuring loose coupling.
2. **Redisson Distributed Lock**: High-concurrency seat reservation is guarded by distributed locks (`lock:seat:<eventId>:<tierId>`) to prevent race conditions and overselling.
3. **Redisson / DB Transaction Ordering**: Database transactions commit *before* the distributed lock is released to avoid phantom reads.
4. **10-Minute Hold TTL**: Seat reservations hold seats temporarily. If unconverted into an order within 10 minutes, the hold expires.
5. **Transactional Outbox Pattern**: Order events are saved to the `outbox_events` table in the exact same DB transaction as the order creation, then published asynchronously to Kafka.
6. **Kafka Event Streaming & Dead Letter Queue (DLQ)**: Includes automatic retry topics with exponential backoff and a DLT handler for unrecoverable errors.
7. **Elasticsearch Fast Discovery**: Events are automatically indexed in Elasticsearch for fuzzy searching and filtering.

---

## 🛠️ Prerequisites

- **Java JDK 21** (e.g. Amazon Corretto 21)
- **Maven 3.9+**
- **Docker & Docker Compose**

---

## ⚙️ Environment Configuration (`.env`)

A template file [`.env.example`](file:///.env.example) is provided. To customize configurations:

```bash
cp .env.example .env
```

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `POSTGRES_HOST` | `localhost` | PostgreSQL Host |
| `POSTGRES_PORT` | `5432` | PostgreSQL Port |
| `POSTGRES_USER` | `rynnhaa` | Database Username |
| `POSTGRES_PASSWORD` | `rynn@dev` | Database Password |
| `POSTGRES_DB` | `queue_event_db` | Database for Event Service |
| `USER_POSTGRES_DB` | `queue_event_user_db` | Database for User Service |
| `REDIS_HOST` | `localhost` | Redis Host |
| `REDIS_PORT` | `6379` | Redis Port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka Broker address |
| `ELASTICSEARCH_HOST` | `localhost` | Elasticsearch Host |
| `ELASTICSEARCH_PORT` | `9200` | Elasticsearch HTTP Port |
| `JWT_SECRET` | *(64-byte Hex)* | HMAC-SHA256 signing secret |
| `JWT_EXPIRATION_MS` | `86400000` | Token expiration (24h in ms) |

---

## 🏃 Running the Application

### Option A: Running Infrastructure with Docker, Apps Locally (Recommended for Dev)

1. **Start Infrastructure Services**:
   ```bash
   docker compose up -d postgres redis kafka elasticsearch
   ```

2. **Build All Services**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Start Microservices (in separate terminal tabs)**:
   ```bash
   # Tab 1: API Gateway
   mvn --projects api-gateway spring-boot:run

   # Tab 2: User Service
   mvn --projects user-service spring-boot:run

   # Tab 3: Event Service
   mvn --projects event-service spring-boot:run
   ```

### Option B: Running Everything via Docker Compose

```bash
docker compose up -d --build
```

---

## 📚 API Documentation & Interactive Swagger

When services are running, interactive Swagger UI is accessible:
- **API Gateway Swagger Hub**: `http://localhost:8080/swagger-ui.html`
- **Event Service Docs**: `http://localhost:8081/swagger-ui.html`
- **User Service Docs**: `http://localhost:8082/swagger-ui.html`

---

## 🧪 End-to-End API Flow (cURL Examples)

All requests pass through the **API Gateway (`http://localhost:8080`)**.

### 1. Register a New User
```bash
curl --location 'http://localhost:8080/api/v1/auth/register' \
--header 'Content-Type: application/json' \
--data '{
    "email": "customer@gmail.com",
    "password": "password123",
    "fullName": "Budi Santoso"
}'
```

### 2. Login & Obtain JWT Token
```bash
curl --location 'http://localhost:8080/api/v1/auth/login' \
--header 'Content-Type: application/json' \
--data '{
    "email": "customer@gmail.com",
    "password": "password123"
}'
```
*Export the token from the response:*
```bash
export TOKEN="<jwt_token_here>"
```

### 3. Check Current User Profile
```bash
curl --location 'http://localhost:8080/api/v1/users/me' \
--header "Authorization: Bearer $TOKEN"
```

### 4. Browse Events
```bash
curl --location 'http://localhost:8080/api/v1/events?page=0&size=10'
```

### 5. Search Events (Elasticsearch Full-Text)
```bash
curl --location 'http://localhost:8080/api/v1/search/events?keyword=Coldplay&page=0&size=10'
```

### 6. Create Temporary Seat Reservation (10-Min Hold)
```bash
curl --location 'http://localhost:8080/api/v1/reservations' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $TOKEN" \
--data '{
    "eventId": 1,
    "ticketTierId": 1,
    "quantity": 2
}'
```
*Note the `reservationCode` from the response (e.g., `RES-ABC1234567`).*

### 7. Convert Reservation into Confirmed Order
```bash
curl --location 'http://localhost:8080/api/v1/orders' \
--header 'Content-Type: application/json' \
--header "Authorization: Bearer $TOKEN" \
--data '{
    "reservationCode": "RES-ABC1234567"
}'
```

### 8. View My Orders
```bash
curl --location 'http://localhost:8080/api/v1/orders/my-orders' \
--header "Authorization: Bearer $TOKEN"
```

### 9. Platform Analytics (Native PostgreSQL SQL)
```bash
curl --location 'http://localhost:8080/api/v1/analytics/platform' \
--header "Authorization: Bearer $TOKEN"
```
