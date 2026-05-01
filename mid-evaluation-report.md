# CO4353 — Distributed Systems
# Mini Project Mid-Evaluation Report

**Project Title:** LPG Distribution & Virtual Queue Management System
**Module:** CO4353 — Distributed Systems
**Evaluation:** Mid-Project Evaluation
**Date:** May 2026
**Team Size:** 4 Members
**Architecture:** Event-Driven Microservices

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Utility Services](#4-utility-services)
5. [Core Service 1 — User Service](#5-core-service-1--user-service)
6. [Core Service 2 — Inventory Service](#6-core-service-2--inventory-service)
7. [Core Service 3 — Allocation Service](#7-core-service-3--allocation-service)
8. [Asynchronous Communication — Apache Kafka](#8-asynchronous-communication--apache-kafka)
9. [Fault Tolerance — Resilience4j](#9-fault-tolerance--resilience4j)
10. [Current Implementation Status](#10-current-implementation-status)
11. [Example Requests and Responses](#11-example-requests-and-responses)
12. [Collaboration and GitHub Insights](#12-collaboration-and-github-insights)
13. [Challenges and Solutions](#13-challenges-and-solutions)
14. [Conclusion](#14-conclusion)

---

## 1. Project Overview

### 1.1 Problem Statement

Sri Lanka faces a recurring national crisis in LPG (Liquefied Petroleum Gas) distribution. Citizens are forced to wait in long physical queues at gas dealer outlets — often for several hours — without any reliable way to know whether stock is even available at their nearest dealer. This causes:

- Wasted time and productivity for citizens
- Unfair, first-come-first-served access with no transparency
- Dealers unable to manage demand or communicate stock levels efficiently
- The government (petroleum regulatory authority) having no centralised visibility over distribution

### 1.2 Solution

The LPG Distribution & Virtual Queue Management System is a cloud-native, event-driven microservices platform that digitises the entire gas distribution lifecycle:

- The **government admin** has central control over approving and managing gas allocations to dealers
- **Dealers** request gas supply digitally, confirm physical delivery, and manage a virtual customer queue
- **Citizens** can discover nearby dealers with stock availability and join a virtual queue remotely — eliminating the need to physically stand in line

### 1.3 User Roles

| Role | Who | Key Capabilities |
|---|---|---|
| `ADMIN` | Government / Regulatory Authority | Register dealers, approve/reject gas allocations, full system visibility |
| `DEALER` | Licensed LPG gas station | Request gas supply, confirm delivery, advance virtual queue |
| `CITIZEN` | General public | Self-register, find nearby dealers, request queue token, receive notifications |

---

## 2. System Architecture

### 2.1 High-Level Architecture Overview

The system follows a **microservices architecture** with an **event-driven backbone** using Apache Kafka. All client traffic enters through a single API Gateway, which routes requests to the appropriate downstream service. Services register themselves with a Eureka Discovery Server, enabling dynamic load-balanced routing without hardcoded addresses.

```
                    ┌─────────────────────────────┐
                    │         API Gateway          │
                    │     (Spring Cloud Gateway)   │
                    │          Port 8080           │
                    └──────────────┬──────────────┘
                                   │
         ┌─────────────────────────┼──────────────────────┐
         │                         │                      │
┌────────▼──────┐    ┌─────────────▼──────┐   ┌──────────▼──────────┐
│  user-service  │    │ inventory-service   │   │  allocation-service  │
│   Port 8081    │    │    Port 8082        │   │     Port 8083        │
│   ✅ DONE      │    │  Member 1 (WIP)     │   │     ✅ DONE          │
└────────┬──────┘    └─────────────┬──────┘   └──────────┬──────────┘
         │                         │                      │
    PostgreSQL                PostgreSQL              PostgreSQL
    (userdb)                (inventorydb)           (allocationdb)

              ┌──────────────────────┐    ┌──────────────────────┐
              │    queue-service     │    │  notification-service │
              │     Port 8084        │    │      Port 8085        │
              │   Member 3 (WIP)     │    │   Member 3 (WIP)      │
              └──────────┬───────────┘    └───────────────────────┘
                         │
                      MongoDB
                     (queuedb)

                    ┌──────────────────────┐
                    │    Apache Kafka       │
                    │  (Message Broker)     │
                    │     Port 9092         │
                    └──────────────────────┘

                    ┌──────────────────────┐
                    │    Eureka Server      │
                    │ (Service Discovery)   │
                    │     Port 8761         │
                    └──────────────────────┘
```

### 2.2 Component Interactions

The system has two communication patterns:

**Synchronous (REST via Feign):** Used for real-time, user-facing queries where an immediate response is required — for example, a citizen querying their token status, or a dealer checking their allocation history.

**Asynchronous (Kafka Events):** Used for cross-service state changes where decoupling and reliability matter more than immediacy — for example, notifying inventory-service when a delivery is confirmed, or notifying citizens when their queue token is called.

### 2.3 Event Flow

```
[1] Dealer confirms delivery
        │
        ▼
allocation-service ──[allocation.confirmed]──► inventory-service
                                                     │
                                              updates stock level
                                                     │
                                                     ▼
                                         [stock.updated] ──► queue-service
                                                                   │
                                                        citizen sees stock availability

[2] Dealer calls next token
        │
        ▼
queue-service ──[token.called]──► notification-service ──► log/email/SMS to citizen
```

---

## 3. Technology Stack

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 17 | All services |
| Framework | Spring Boot | 3.5.14 | Application framework |
| Service Discovery | Netflix Eureka | Spring Cloud 2025.0.2 | Dynamic service registry |
| API Gateway | Spring Cloud Gateway MVC | Spring Cloud 2025.0.2 | Single entry point, routing |
| Inter-service (sync) | Spring Cloud OpenFeign | Spring Cloud 2025.0.2 | Declarative HTTP clients |
| Message Broker | Apache Kafka | 3.x (Confluent 7.4.0) | Async event streaming |
| ORM | Spring Data JPA / Hibernate | Spring Boot 3.5.14 | Relational data access |
| Relational DB | PostgreSQL | 15 | user, inventory, allocation data |
| Document DB | MongoDB | 7 | Queue token storage |
| Security | Spring Security + JWT (JJWT) | 0.12.6 | Stateless authentication |
| Fault Tolerance | Resilience4j | Spring Boot 3.x | Circuit breaker pattern |
| Build Tool | Maven | 3.9.15 | Dependency management |
| Containerisation | Docker + Docker Compose | Latest | Service orchestration |

---

## 4. Utility Services

### 4.1 Discovery Server (Eureka)

**Port:** `8761`
**Status:** ✅ Complete

The Eureka Discovery Server acts as the **service registry** for the entire platform. Every microservice registers itself with Eureka on startup with its name, host, and port. When one service needs to call another, it asks Eureka for the live address rather than using a hardcoded IP.

**Key configuration in each service:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

**Benefits in this project:**
- Services can be scaled horizontally — multiple instances register under the same name and the gateway load-balances between them
- If a service instance goes down, Eureka removes it from the registry within seconds, preventing the gateway from routing to dead instances
- No hardcoded service URLs anywhere in the codebase

### 4.2 API Gateway

**Port:** `8080`
**Status:** ✅ Complete

The API Gateway is the **single entry point** for all external client traffic (Postman, mobile app, web front-end). It uses Eureka's `lb://` (load-balanced) scheme to route requests to the correct downstream service.

**Routing table:**

| Incoming Path | Routed To | Service |
|---|---|---|
| `/api/v1/users/**` | `lb://user-service` | User Service |
| `/api/v1/inventory/**` | `lb://inventory-service` | Inventory Service |
| `/api/v1/allocations/**` | `lb://allocation-service` | Allocation Service |
| `/api/v1/queue/**` | `lb://queue-service` | Queue Service |

**Gateway configuration:**
```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true
          routes:
            - id: user-service
              uri: lb://user-service
              predicates:
                - Path=/api/v1/users/**
            - id: allocation-service
              uri: lb://allocation-service
              predicates:
                - Path=/api/v1/allocations/**
```

The gateway does **not** validate JWTs — each service is responsible for its own authentication. This is a deliberate distributed security model: no single point of auth failure can bring down all services.

---

## 5. Core Service 1 — User Service

**Port:** `8081` | **Database:** PostgreSQL (`userdb`) | **Status:** ✅ Complete

### 5.1 Responsibility

The User Service is the **authentication and identity management** service for the entire platform. It handles:
- Citizen self-registration
- Admin-managed dealer registration
- JWT token issuance on login
- User profile management

### 5.2 Data Model

```
users table:
├── id            UUID (PK, auto-generated)
├── nic           VARCHAR (unique) — old: 123456789V, new: 200012345678
├── email         VARCHAR (unique)
├── password      VARCHAR (BCrypt hashed)
├── name          VARCHAR
├── role          ENUM: CITIZEN | DEALER | ADMIN
├── phone         VARCHAR (dealers only)
├── address       VARCHAR (dealers only)
├── businessName  VARCHAR (dealers only)
├── businessRegNo VARCHAR (dealers only)
└── createdAt     TIMESTAMP (auto-set)
```

### 5.3 JWT Token Design

All services share the same JWT secret. When a user logs in, user-service issues a signed JWT. All other services validate this token independently — no inter-service call is needed for auth.

**JWT payload:**
```json
{
  "sub": "<userId UUID>",
  "email": "user@example.com",
  "role": "CITIZEN | DEALER | ADMIN",
  "iat": 1714464000,
  "exp": 1714550400
}
```

### 5.4 REST Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/users/register` | Public | Citizen self-registration |
| `POST` | `/api/v1/users/register/dealer` | `ADMIN` | Admin registers a dealer |
| `POST` | `/api/v1/users/login` | Public | Returns signed JWT |
| `GET` | `/api/v1/users/me` | Any | Own profile |
| `GET` | `/api/v1/users/{id}` | Own or `ADMIN` | Get user by ID |
| `PUT` | `/api/v1/users/{id}` | Own or `ADMIN` | Update profile |
| `GET` | `/api/v1/users/role/{role}` | `ADMIN` | List users by role |
| `DELETE` | `/api/v1/users/{id}` | `ADMIN` | Delete user |

---

## 6. Core Service 2 — Inventory Service

**Port:** `8082` | **Database:** PostgreSQL (`inventorydb`) | **Status:** 🔧 In Progress (Member 1)

### 6.1 Responsibility

The Inventory Service tracks **real-time LPG cylinder stock levels** for every registered dealer. It is the source of truth for stock data and exposes endpoints that citizens use to find available dealers nearby.

### 6.2 Data Model

```
inventory table:
├── id              UUID (PK)
├── dealerId        VARCHAR (FK reference to user-service)
├── dealerName      VARCHAR
├── address         VARCHAR
├── latitude        DOUBLE
├── longitude       DOUBLE
├── availableStock  INTEGER (number of cylinders)
└── lastUpdated     TIMESTAMP (auto-updated)
```

### 6.3 REST Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/inventory` | `ADMIN` | Create inventory record for a dealer |
| `GET` | `/api/v1/inventory/dealer/{dealerId}` | Any | Get stock for a dealer |
| `GET` | `/api/v1/inventory/available` | Any | All dealers with stock > 0 |
| `GET` | `/api/v1/inventory/nearby?lat=&lng=&radius=` | Any | Dealers with stock within radius (km) |
| `PUT` | `/api/v1/inventory/{id}/stock` | `DEALER` (own only) | Update own stock level |

### 6.4 Kafka Integration

**Consumes:** `allocation.confirmed` — when allocation-service fires this event, inventory-service adds the approved quantity to the dealer's stock.

**Publishes:** `stock.updated` — after updating stock, inventory-service fires this event so queue-service can update its local cache.

```
allocation.confirmed received
        │
        ▼
inventory.availableStock += event.quantity
        │
        ▼
stock.updated published → queue-service cache refreshed
```

---

## 7. Core Service 3 — Allocation Service

**Port:** `8083` | **Database:** PostgreSQL (`allocationdb`) | **Status:** ✅ Complete

### 7.1 Responsibility

The Allocation Service manages the **gas cylinder supply workflow** between the government and dealers. Dealers submit requests for gas cylinders. The admin reviews and either approves with a quantity or rejects with a reason. Once approved, the dealer physically receives the delivery and confirms it in the system — which triggers an asynchronous Kafka event that causes inventory to be updated automatically.

### 7.2 Data Model

```
allocations table:
├── id                  UUID (PK, auto-generated)
├── dealer_id           VARCHAR — from JWT principal (user-service UUID)
├── dealer_name         VARCHAR
├── requested_quantity  INTEGER — dealer's ask
├── approved_quantity   INTEGER — set by admin on approval
├── status              ENUM: PENDING | APPROVED | REJECTED | DELIVERED
├── rejection_reason    VARCHAR — set by admin on rejection
├── requested_at        TIMESTAMP — auto-set on create
├── resolved_at         TIMESTAMP — set when approved or rejected
└── delivered_at        TIMESTAMP — set when dealer confirms
```

### 7.3 State Machine

The allocation lifecycle follows a strict state machine. Any attempt to make an illegal transition throws an `InvalidStateException` (HTTP 409 Conflict).

```
         ┌─────────────────────────────────────────┐
         │                                         │
   [Dealer submits]                                │
         │                                         │
         ▼                                         │
      PENDING ──[Admin approves]──► APPROVED ──[Dealer confirms]──► DELIVERED
         │
         └──[Admin rejects]──► REJECTED
```

### 7.4 REST Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/v1/allocations/test` | Public | Health check |
| `POST` | `/api/v1/allocations/request` | `DEALER` | Submit new allocation request |
| `GET` | `/api/v1/allocations/pending` | `ADMIN` | List all PENDING allocations |
| `GET` | `/api/v1/allocations` | `ADMIN` | List all (optional `?status=` filter) |
| `PUT` | `/api/v1/allocations/{id}/approve` | `ADMIN` | Approve and set approved quantity |
| `PUT` | `/api/v1/allocations/{id}/reject` | `ADMIN` | Reject with reason |
| `PUT` | `/api/v1/allocations/{id}/confirm` | `DEALER` | Confirm delivery → fires Kafka |
| `GET` | `/api/v1/allocations/dealer/{dealerId}` | `DEALER` (own only) | Dealer's allocation history |
| `GET` | `/api/v1/allocations/{id}` | Any authenticated | Get single allocation |

### 7.5 Security Implementation

Security is implemented using Spring Security with stateless JWT validation. There is no session state — every request must carry a valid Bearer token. Role-based access is enforced at the method level using `@PreAuthorize`.

```java
@PostMapping("/request")
@PreAuthorize("hasRole('DEALER')")
public ResponseEntity<AllocationResponse> requestAllocation(...) { }

@PutMapping("/{id}/approve")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<AllocationResponse> approve(...) { }

@GetMapping("/dealer/{dealerId}")
@PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal")
public ResponseEntity<List<AllocationResponse>> getByDealer(...) { }
```

The dealer ownership check on `/confirm` is enforced in the service layer:
```java
if (!allocation.getDealerId().equals(dealerId)) {
    throw new AccessDeniedException("You do not own this allocation");
}
```

### 7.6 Package Structure

```
allocation-service/src/main/java/com/gastracker/allocation_service/
├── config/
│   ├── GlobalExceptionHandler.java
│   ├── JwtAuthFilter.java
│   ├── KafkaProducerConfig.java
│   └── SecurityConfig.java
├── controller/
│   └── AllocationController.java
├── dao/
│   ├── entity/Allocation.java
│   └── repository/AllocationRepository.java
├── dto/
│   ├── request/  (AllocationRequest, ApproveRequest, RejectRequest)
│   └── response/ (AllocationResponse, ErrorResponse)
├── enums/
│   └── AllocationStatus.java
├── event/
│   └── AllocationConfirmedEvent.java
├── exception/
│   ├── InvalidStateException.java
│   └── ResourceNotFoundException.java
└── service/
    ├── AllocationService.java
    ├── helper/JwtHelper.java
    └── transformer/AllocationTransformer.java
```

---

## 8. Asynchronous Communication — Apache Kafka

### 8.1 Why Kafka

In a microservices system, services must be **loosely coupled**. If allocation-service called inventory-service directly (synchronously) to update stock, it would mean:
- Allocation-service is **blocked** waiting for inventory-service to respond
- If inventory-service is down, the confirmation fails — even though the delivery physically happened
- The services are tightly coupled and cannot be scaled or deployed independently

Apache Kafka solves this with **asynchronous event streaming**: allocation-service publishes an event to a Kafka topic and immediately returns a response to the dealer. Inventory-service reads the event from the topic at its own pace — even if it was down for an hour, it will process all missed events when it comes back up.

### 8.2 Kafka Topics

| Topic | Published By | Consumed By | Trigger |
|---|---|---|---|
| `allocation.confirmed` | allocation-service | inventory-service | Dealer confirms delivery |
| `stock.updated` | inventory-service | queue-service | Stock level changes |
| `token.issued` | queue-service | notification-service | Citizen joins queue |
| `token.called` | queue-service | notification-service | Dealer calls next token |

### 8.3 Event Schemas (Shared Contracts)

**`allocation.confirmed`** — published by allocation-service:
```json
{
  "allocationId": "550e8400-e29b-41d4-a716-446655440000",
  "dealerId":     "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "quantity":     80
}
```

**`stock.updated`** — published by inventory-service:
```json
{
  "dealerId":       "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "dealerName":     "Silva Gas Station",
  "address":        "45/B Galle Road, Colombo 03",
  "availableStock": 125,
  "latitude":       6.9271,
  "longitude":      79.8612
}
```

**`token.called`** — published by queue-service:
```json
{
  "tokenId":      "uuid",
  "tokenNumber":  42,
  "citizenId":    "uuid",
  "citizenEmail": "alice@example.com",
  "dealerName":   "Silva Gas Station"
}
```

### 8.4 Kafka Producer Configuration (allocation-service)

The Kafka producer in allocation-service is configured for **reliability and fault tolerance**:

```java
config.put(ProducerConfig.ACKS_CONFIG, "all");
config.put(ProducerConfig.RETRIES_CONFIG, 3);
config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

| Setting | Value | Purpose |
|---|---|---|
| `acks=all` | all replicas acknowledge | No message is lost even if the broker crashes mid-write |
| `retries=3` | 3 automatic retries | Transient network errors are recovered automatically |
| `retry.backoff.ms=1000` | 1 second between retries | Avoids hammering a struggling broker |
| `enable.idempotence=true` | exactly-once delivery | Even if the same message is retried, it is only written once |

### 8.5 Async Callback for Observability

Rather than blocking on the Kafka send, the service uses a non-blocking callback:

```java
kafkaTemplate.send("allocation.confirmed", event)
    .whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("Failed to publish allocation.confirmed: {}", ex.getMessage());
        } else {
            log.info("Published allocation.confirmed allocationId={} qty={}", id, qty);
        }
    });
```

This means the HTTP response is returned to the dealer immediately, and any Kafka failure is logged for alerting — the dealer is not made to wait.

---

## 9. Fault Tolerance — Resilience4j

### 9.1 What is Fault Tolerance in This Context

In a distributed system, **any service can fail at any time** — network partitions, database overload, or a crashed process. Without fault tolerance mechanisms, a failure in one service can cascade across the entire system (a "cascade failure"). Resilience4j provides patterns to contain failures.

### 9.2 Circuit Breaker Pattern

The allocation-service includes `spring-cloud-starter-circuitbreaker-resilience4j` for any Feign-based inter-service calls. A circuit breaker works like an electrical circuit breaker:

```
CLOSED (normal) → too many failures → OPEN (failing fast)
                                           │
                                    after wait period
                                           │
                                    HALF-OPEN (testing)
                                       │         │
                                  success      failure
                                     │             │
                                  CLOSED         OPEN
```

**Configuration in `application.yaml`:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory-service:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
```

This means: if 50% of the last 10 calls to inventory-service fail, the circuit opens and all further calls immediately return the fallback response for 10 seconds — without waiting for a timeout.

### 9.3 Feign Fallback

```java
@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {
    @GetMapping("/api/v1/inventory/dealer/{dealerId}")
    InventoryResponse getStock(@PathVariable String dealerId);
}

@Component
public class InventoryClientFallback implements InventoryClient {
    @Override
    public InventoryResponse getStock(String dealerId) {
        return InventoryResponse.builder().availableStock(0).build();
    }
}
```

When inventory-service is unavailable, the fallback returns a safe default (`availableStock=0`) so the calling service can still respond meaningfully instead of throwing a 500 error.

### 9.4 Stateless JWT — Horizontal Scalability

By using stateless JWT tokens (no server-side session), any instance of any service can serve any request. This means:
- Multiple instances of allocation-service can run behind the gateway simultaneously
- Eureka load-balances requests across them
- If one instance crashes, the others continue serving — no single point of failure

### 9.5 Database Fault Tolerance

Each service has its own isolated PostgreSQL database. A failure in one database affects only that service. The `spring.jpa.hibernate.ddl-auto=update` ensures tables are created or updated automatically on startup — no manual migration scripts that could fail.

---

## 10. Current Implementation Status

### 10.1 Service Completion Matrix

| Service | Port | Database | Status | Owner |
|---|---|---|---|---|
| discovery-server (Eureka) | 8761 | — | ✅ Complete | Team Lead |
| api-gateway | 8080 | — | ✅ Complete | Team Lead |
| user-service | 8081 | PostgreSQL (userdb) | ✅ Complete | Team Lead |
| inventory-service | 8082 | PostgreSQL (inventorydb) | 🔧 In Progress | Member 1 |
| allocation-service | 8083 | PostgreSQL (allocationdb) | ✅ Complete | Member 2 |
| queue-service | 8084 | MongoDB (queuedb) | 🔧 In Progress | Member 3 |
| notification-service | 8085 | — (event consumer only) | 🔧 In Progress | Member 3 |

### 10.2 Infrastructure Running

| Container | Image | Status |
|---|---|---|
| eureka-server | Spring Boot app | ✅ Running |
| api-gateway | Spring Boot app | ✅ Running |
| postgres-user | postgres:15 | ✅ Healthy |
| postgres-inventory | postgres:15 | ✅ Healthy |
| postgres-allocation | postgres:15 | ✅ Healthy |
| mongodb | mongo:7 | ✅ Running |
| zookeeper | confluentinc/cp-zookeeper:7.4.0 | ✅ Running |
| kafka | confluentinc/cp-kafka:7.4.0 | ✅ Running |

### 10.3 Implemented Features

**Authentication & Identity (user-service):**
- Full user registration for CITIZEN and DEALER roles
- Admin-only dealer registration endpoint
- JWT-based login — token issued on success
- Profile management (view, update, delete)
- BCrypt password hashing

**Gas Allocation Workflow (allocation-service):**
- Full CRUD for allocation requests
- Role-based access control (DEALER, ADMIN) using `@PreAuthorize`
- Complete state machine: PENDING → APPROVED/REJECTED → DELIVERED
- Kafka event publishing on delivery confirmation
- Idempotent Kafka producer with retry and `acks=all`
- Resilience4j circuit breaker dependency wired
- Global exception handler — consistent JSON error responses
- Dealer ownership enforcement on confirm endpoint

**Infrastructure:**
- Eureka service registry — all services self-register
- API Gateway — single entry point with dynamic routing via `lb://`
- Docker Compose — full stack up with one command
- Health checks on all containers

---

## 11. Example Requests and Responses

### 11.1 User Registration

**Request:**
```http
POST /api/v1/users/register
Content-Type: application/json

{
  "nic": "123456789V",
  "email": "dealer@silva.lk",
  "password": "securepassword",
  "name": "Silva Gas Station",
  "role": "DEALER"
}
```

**Response `201 Created`:**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "nic": "123456789V",
  "email": "dealer@silva.lk",
  "name": "Silva Gas Station",
  "role": "DEALER",
  "createdAt": "2026-05-01T09:00:00"
}
```

### 11.2 Login

**Request:**
```http
POST /api/v1/users/login
Content-Type: application/json

{
  "email": "dealer@silva.lk",
  "password": "securepassword"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

### 11.3 Dealer Submits Allocation Request

**Request:**
```http
POST /api/v1/allocations/request
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "requestedQuantity": 100,
  "dealerName": "Silva Gas Station"
}
```

**Response `201 Created`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "dealerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "dealerName": "Silva Gas Station",
  "requestedQuantity": 100,
  "approvedQuantity": null,
  "status": "PENDING",
  "rejectionReason": null,
  "requestedAt": "2026-05-01T09:15:00",
  "resolvedAt": null,
  "deliveredAt": null
}
```

### 11.4 Admin Approves Allocation

**Request:**
```http
PUT /api/v1/allocations/550e8400-e29b-41d4-a716-446655440000/approve
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json

{
  "approvedQuantity": 80
}
```

**Response `200 OK`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "dealerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "dealerName": "Silva Gas Station",
  "requestedQuantity": 100,
  "approvedQuantity": 80,
  "status": "APPROVED",
  "rejectionReason": null,
  "requestedAt": "2026-05-01T09:15:00",
  "resolvedAt": "2026-05-01T10:00:00",
  "deliveredAt": null
}
```

### 11.5 Dealer Confirms Delivery (Kafka fires)

**Request:**
```http
PUT /api/v1/allocations/550e8400-e29b-41d4-a716-446655440000/confirm
Authorization: Bearer <DEALER_TOKEN>
```

**Response `200 OK`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "dealerId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "dealerName": "Silva Gas Station",
  "requestedQuantity": 100,
  "approvedQuantity": 80,
  "status": "DELIVERED",
  "rejectionReason": null,
  "requestedAt": "2026-05-01T09:15:00",
  "resolvedAt": "2026-05-01T10:00:00",
  "deliveredAt": "2026-05-01T14:30:00"
}
```

**Kafka log (allocation-service):**
```
INFO  Published allocation.confirmed for allocationId=550e8400... dealerId=7c9e6679... qty=80
```

### 11.6 Standard Error Response Format

All services return errors in the same JSON structure:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Only PENDING allocations can be approved. Current status: APPROVED",
  "timestamp": "2026-05-01T10:05:00"
}
```

---

## 12. Collaboration and GitHub Insights

### 12.1 Repository Structure

The project is maintained in a single monorepo on GitHub, with each microservice in its own top-level directory. This keeps service code independent while sharing infrastructure configuration (`docker-compose.yml`, documentation).

```
smart-gas-distribution-platform/
├── api-gateway/
├── discovery-server/
├── user-service/
├── inventory-service/
├── allocation-service/          ← Member 2's work
├── docker-compose.yml
├── guide.md
└── task for each member.md
```

### 12.2 Branching Strategy

| Branch | Owner | Purpose |
|---|---|---|
| `main` | Team Lead | Stable, reviewed code only |
| `nuwan/user-service` | Team Lead | User service development |
| `sathmi/allocation` | Member 2 | Allocation service development |
| `member1/inventory` | Member 1 | Inventory service development |
| `member3/queue` | Member 3 | Queue + notification services |

Each member works on their own feature branch and raises a Pull Request to `main` when their service is ready for integration. This prevents incomplete code from breaking other members' work.

### 12.3 Shared Contracts

The team agreed on Kafka event schemas, error response formats, JWT structure, and service names before development started — documented in `task for each member.md`. This contract-first approach ensures services can be integrated without last-minute incompatibilities.

### 12.4 Contribution Summary

| Member | Primary Contribution |
|---|---|
| Team Lead | user-service (auth, JWT, security), api-gateway routing, Eureka, Docker Compose base, developer guide |
| Member 1 | inventory-service (in progress) — entity, stock endpoints, Kafka consumer |
| Member 2 | allocation-service — full implementation, Kafka producer, state machine, security |
| Member 3 | queue-service + notification-service (in progress) — MongoDB documents, token lifecycle |

---

## 13. Challenges and Solutions

### Challenge 1 — Zookeeper Health Check Incompatibility

**Problem:** The Docker Compose health check for Zookeeper used `nc` (netcat), which is not available in the Confluent Zookeeper image. This caused Zookeeper to report as `unhealthy`, which blocked Kafka from starting (since Kafka had `depends_on: zookeeper: condition: service_healthy`).

**Solution:** Changed the Zookeeper `depends_on` condition in the Kafka service from `service_healthy` to `service_started`, and updated the health check command. The Kafka application itself handles reconnection to Zookeeper internally, so the strict health gate was unnecessary.

### Challenge 2 — Maven Wrapper Files Missing

**Problem:** The `mvnw` / `mvnw.cmd` wrapper scripts were committed without the `.mvn/wrapper/maven-wrapper.properties` file that they depend on. Running `.\mvnw clean package` failed with a "cannot find path" error.

**Solution:** Used the globally installed Maven (`mvn clean package -DskipTests`) directly, bypassing the wrapper entirely. This is equally valid since Maven 3.9.x is installed on the development machine.

### Challenge 3 — Kafka Inter-Container Communication

**Problem:** Spring Boot services inside Docker containers were initially configured to connect to `localhost:9092` for Kafka. However, within the Docker network, `localhost` refers to the container itself, not the Kafka broker.

**Solution:** Configured two Kafka listener protocols — `PLAINTEXT://localhost:9092` for host-machine access and `PLAINTEXT_INTERNAL://kafka:29092` for inter-container communication. Services running in Docker use the `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092` environment variable injected via Docker Compose.

### Challenge 4 — JWT Validation Across Services

**Problem:** Each service needs to validate JWTs issued by user-service, but services cannot call user-service on every request (too slow, too tightly coupled).

**Solution:** All services share the same JWT signing secret. Each service independently validates tokens using a local `JwtHelper` component — no inter-service call required. This keeps authentication stateless and fast.

---

## 14. Conclusion

The LPG Distribution & Virtual Queue Management System demonstrates the core principles of distributed systems engineering applied to a real-world national infrastructure problem:

- **Loose coupling** through asynchronous Kafka messaging — services evolve and deploy independently
- **Fault tolerance** through Resilience4j circuit breakers, idempotent Kafka producers, and stateless JWT design
- **Scalability** through Eureka-based service discovery and load-balanced routing at the gateway
- **Security** through role-based, stateless JWT authentication enforced at the method level in each service
- **Observability** through Spring Actuator health endpoints consumed by both Eureka and Docker Compose health checks

At the mid-evaluation point, three services are fully operational — the user-service, allocation-service, and all infrastructure (Eureka, API Gateway, Kafka, all databases). The remaining services (inventory-service, queue-service, notification-service) are under active development and will complete the full citizen-facing queue flow for the final evaluation.

The collaboration model — contract-first design, feature branching, and shared Docker infrastructure — has enabled parallel independent development without integration blockers.

---

*CO4353 Distributed Systems — Mini Project Mid-Evaluation | May 2026*
