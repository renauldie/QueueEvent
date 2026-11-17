# EventFlow — Event Ticketing & Reservation Platform

EventFlow is a high-throughput, event-driven ticketing and reservation platform built using Spring Boot, Spring Cloud Gateway, Apache Kafka, Redis, PostgreSQL, and Elasticsearch.

## 1. System Architecture
The application is structured as a 2-service microservices architecture:
- **`api-gateway`**: Spring Cloud Gateway handling dynamic routing, JWT-based authentication & authorization, Redis rate limiting, correlation ID tracking, and centralized error handling.
- **`ticket-queue-service`**: Core domain service managing event lifecycle, seat inventories, temporary seat reservations with Redis distributed locks, orders, payment simulations, Kafka transactional outbox messaging, and Elasticsearch synchronization.

## 2. Infrastructure Components
- **PostgreSQL 16**: Relational storage with advanced native SQL queries (CTE, Window Functions, Ranking, Aggregation).
- **Redis 7**: High-performance caching, temporary seat holds with TTL, distributed locking (Redisson), and rate limiting.
- **Apache Kafka**: Event streaming backbone with transactional outbox, retry mechanisms, and Dead Letter Queues (DLQ).
- **Elasticsearch 8**: Full-text search engine for high-speed event discovery, filtering, and pagination.

## 3. Technology Stack
- Java 21
- Spring Boot 3.4.x / 3.5.x
- Spring Cloud 2024.x
- Maven (Multi-Module)
- Docker & Docker Compose
