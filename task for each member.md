# LPG Distribution & Queue Management System
## Project Specification Document

**Course:** CO4353 — Distributed Systems  
**Project Type:** Mini Project  
**Team Size:** 4 Members  
**Architecture:** Event-Driven Microservices  
**Date:** April 2026

---

## Task Assignment Summary

| Member | Services | Status |
|---|---|---|
| **Team Lead** | user-service, api-gateway, discovery-server | ✅ Complete |
| **Member 1** | inventory-service | 🔧 To Build |
| **Member 2** | allocation-service | 🔧 To Build |
| **Member 3** | queue-service + notification-service | 🔧 To Build |

> **Note for all members:** The team lead has already completed user-service, api-gateway, and the Eureka discovery server. Do not modify those services. Read Section 4 (Completed Work) carefully — it defines the JWT format and user data structures your services will depend on.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Completed Work](#4-completed-work)
5. [Shared Contracts](#5-shared-contracts)
6. [Member 1 — Inventory Service](#6-member-1--inventory-service)
7. [Member 2 — Allocation Service](#7-member-2--allocation-service)
8. [Member 3 — Queue Service & Notification Service](#8-member-3--queue-service--notification-service)
9. [Integration Guide](#9-integration-guide)
10. [Quality Attributes](#10-quality-attributes)
11. [Project Timeline](#11-project-timeline)

---

## 1. Project Overview

### Problem Statement

Sri Lanka faces recurring LPG shortages that cause citizens to wait in long physical queues at gas dealers — often without knowing whether stock is available. This leads to wasted time, unfair access, and no transparency in the distribution process.

### Solution

A national-scale microservices platform that:

- Gives the **government admin** central control over gas allocation to dealers
- Allows **dealers** to request stock and update inventory digitally
- Enables **citizens** to find nearby dealers with stock and join a **virtual queue** remotely — eliminating the need to physically wait

### User Roles

| Role | Who | Key Actions |
|---|---|---|
| `ADMIN` | Government authority | Register dealers, approve gas allocations, full system access |
| `DEALER` | LPG gas station | Request gas supply, confirm delivery, advance queue |
| `CITIZEN` | Public user | Self-register, find dealers, request queue token |

---

## 2. System Architecture

### High-Level Diagram

```
                        ┌──────────────────────┐
                        │      API Gateway      │
                        │    (Port 8080)        │
                        │  JWT validated here   │
                        └──────────┬────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
┌───────▼──────┐    ┌──────────────▼────┐    ┌───────────────▼──────┐
│ user-service │    │ inventory-service  │    │  allocation-service  │
│  Port 8081   │    │    Port 8082      │    │     Port 8083        │
│  (DONE ✓)   │    │   (Member 1)      │    │    (Member 2)        │
└───────┬──────┘    └──────────┬─────────┘    └───────────┬──────────┘
        │                      │                          │
   PostgreSQL             PostgreSQL                 PostgreSQL
   (userdb)               (inventorydb)             (allocationdb)

        ┌──────────────────────┐        ┌─────────────────────────┐
        │    queue-service     │        │   notification-service   │
        │     Port 8084        │        │      Port 8085           │
        │    (Member 3)        │        │      (Member 3)          │
        └──────────┬───────────┘        └────────────┬─────────────┘
                   │                                 │
                MongoDB                     (no DB — event consumer)
               (queuedb)

                        ┌──────────────────────┐
                        │     Kafka Broker     │   ← async event bus
                        │     Port 9092        │
                        └──────────────────────┘

                        ┌──────────────────────┐
                        │    Eureka Server     │   ← service registry (DONE ✓)
                        │     Port 8761        │
                        └──────────────────────┘
```

### Communication Patterns

| Type | Used For | Technology |
|---|---|---|
| Synchronous | Auth, real-time queries | REST (Spring Cloud Feign) |
| Asynchronous | Stock updates, notifications | Apache Kafka |

### Event Flow

```
Dealer confirms delivery
       │
       ▼
allocation-service  ──[allocation.confirmed]──►  inventory-service
                                                      │ updates stock
                                                      ▼
                                               [stock.updated] ──► queue-service
                                                                        │ updates local cache
                                                                        ▼
                                                                 citizen sees availability

Dealer calls next token
       │
       ▼
queue-service  ──[token.called]──►  notification-service  ──► SMS/Email to citizen
```

---

## 3. Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.x |
| Service Discovery | Netflix Eureka | Spring Cloud |
| API Gateway | Spring Cloud Gateway MVC | Spring Cloud |
| Inter-service (sync) | Spring Cloud OpenFeign | Spring Cloud |
| Message Broker | Apache Kafka | 3.x |
| ORM | Spring Data JPA / Hibernate | Spring Boot 3.x |
| Relational DB | PostgreSQL | 15 |
| Document DB | MongoDB | 7 |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| Fault Tolerance | Resilience4j | Spring Boot 3.x |
| Build Tool | Maven | 3.8+ |
| Containerization | Docker + Docker Compose | Latest |

### Maven Dependencies Reference

**Every service must include:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

**Services with PostgreSQL add:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Services with Kafka add:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

## 4. Completed Work

The following services are **fully implemented** and must not be modified without team discussion.

### 4.1 Discovery Server (Eureka)
- Port: `8761`
- All services register here automatically via `@EnableEurekaClient`

### 4.2 API Gateway
- Port: `8080`
- All client requests go through here
- Routes defined under `spring.cloud.gateway.server.webmvc.routes`
- Does **not** validate JWT — services validate their own tokens

### 4.3 User Service
- Port: `8081`
- Database: PostgreSQL (`userdb`)
- **All endpoints finalized — do not change paths or response schemas**

#### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users/register` | Public | Citizen self-registration |
| POST | `/api/v1/users/register/dealer` | ADMIN | Admin registers a dealer |
| POST | `/api/v1/users/login` | Public | Returns JWT token |
| GET | `/api/v1/users/me` | Any | Own profile |
| GET | `/api/v1/users/{id}` | Own or ADMIN | Get user by ID |
| PUT | `/api/v1/users/{id}` | Own or ADMIN | Update profile |
| GET | `/api/v1/users/role/{role}` | ADMIN | List by role |
| DELETE | `/api/v1/users/{id}` | ADMIN | Delete user |

#### JWT Token Format

All protected endpoints require this header:
```
Authorization: Bearer <token>
```

JWT payload structure:
```json
{
  "sub": "<userId UUID>",
  "email": "user@example.com",
  "role": "CITIZEN | DEALER | ADMIN",
  "iat": 1714464000,
  "exp": 1714550400
}
```

The **subject (`sub`)** is the user's UUID. When services read the token, `authentication.getPrincipal()` returns this UUID string.

#### User Entity Fields

| Field | Type | Notes |
|---|---|---|
| id | String (UUID) | Primary key |
| nic | String | Unique — old format `123456789V` or new `200012345678` |
| email | String | Unique |
| name | String | Full name |
| role | Enum | `CITIZEN`, `DEALER`, `ADMIN` |
| phone | String | Dealers only |
| address | String | Dealers only |
| businessName | String | Dealers only |
| businessRegNo | String | Dealers only |
| createdAt | LocalDateTime | Auto-set on save |

---

## 5. Shared Contracts

**Agree on these before starting development. Do not change without team discussion.**

### 5.1 Kafka Topics and Event Schemas

#### Topic: `allocation.confirmed`
Published by: `allocation-service` (Member 2)  
Consumed by: `inventory-service` (Member 1)

```json
{
  "allocationId": "uuid-string",
  "dealerId": "uuid-string",
  "quantity": 100
}
```

#### Topic: `stock.updated`
Published by: `inventory-service` (Member 1)  
Consumed by: `queue-service` (Member 3)

```json
{
  "dealerId": "uuid-string",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "availableStock": 45,
  "latitude": 6.9271,
  "longitude": 79.8612
}
```

#### Topic: `token.called`
Published by: `queue-service` (Member 3)  
Consumed by: `notification-service` (Member 3)

```json
{
  "tokenId": "uuid-string",
  "tokenNumber": 42,
  "citizenId": "uuid-string",
  "citizenEmail": "alice@example.com",
  "dealerName": "Silva Gas Station"
}
```

#### Topic: `token.issued`
Published by: `queue-service` (Member 3)  
Consumed by: `notification-service` (Member 3)

```json
{
  "tokenId": "uuid-string",
  "tokenNumber": 42,
  "citizenId": "uuid-string",
  "citizenEmail": "alice@example.com",
  "dealerName": "Silva Gas Station",
  "queuePosition": 5
}
```

### 5.2 Standard Error Response

All services must return errors in this format:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Dealer not found",
  "timestamp": "2026-04-30T10:30:00"
}
```

Implement `GlobalExceptionHandler` with `@RestControllerAdvice` in every service. Reference user-service implementation.

### 5.3 Service Names in Eureka

These names must match exactly in `application.yaml` (`spring.application.name`) and in Feign client declarations:

| Service | Eureka Name |
|---|---|
| user-service | `user-service` |
| inventory-service | `inventory-service` |
| allocation-service | `allocation-service` |
| queue-service | `queue-service` |
| notification-service | `notification-service` |

---

## 6. Member 1 — Inventory Service

### Responsibility
Track real-time LPG cylinder stock levels for each dealer. Expose stock availability so citizens can find dealers near them. Consume allocation confirmation events to update stock.

### Service Configuration

**File:** `inventory-service/src/main/resources/application.yaml`
```yaml
server:
  port: 8082

spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:postgresql://localhost:5432/inventorydb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: inventory-service
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Folder Structure

```
inventory-service/src/main/java/com/gastracker/inventory_service/
├── config/
│   ├── SecurityConfig.java         ← JWT filter + role rules
│   ├── JwtAuthFilter.java          ← same as user-service
│   └── KafkaConsumerConfig.java    ← Kafka deserializer setup
├── controller/
│   └── InventoryController.java
├── dao/
│   ├── entity/
│   │   └── Inventory.java
│   └── repository/
│       └── InventoryRepository.java
├── dto/
│   ├── request/
│   │   └── UpdateStockRequest.java
│   └── response/
│       ├── InventoryResponse.java
│       └── ErrorResponse.java
├── event/
│   ├── AllocationConfirmedEvent.java  ← consumed
│   └── StockUpdatedEvent.java         ← published
├── exception/
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
└── service/
    ├── InventoryService.java
    └── transformer/
        └── InventoryTransformer.java
```

### Database Entity

```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "inventory")
public class Inventory {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String dealerId;          // UUID from user-service

    @Column(nullable = false)
    private String dealerName;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Integer availableStock;   // number of cylinders

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist @PreUpdate
    protected void onUpdate() { lastUpdated = LocalDateTime.now(); }
}
```

### API Endpoints

#### GET `/api/v1/inventory/dealer/{dealerId}`
Get stock for a specific dealer.

**Auth:** Any authenticated user  
**Response 200:**
```json
{
  "id": "uuid",
  "dealerId": "uuid",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 45,
  "lastUpdated": "2026-04-30T09:00:00"
}
```

#### GET `/api/v1/inventory/available`
Get all dealers that currently have stock > 0.

**Auth:** Any authenticated user  
**Response 200:** Array of `InventoryResponse`

#### GET `/api/v1/inventory/nearby?lat={lat}&lng={lng}&radius={km}`
Get dealers with available stock within a radius. Sort by distance ascending.

**Auth:** Any authenticated user  
**Query Params:** `lat` (double), `lng` (double), `radius` (double, default 10)  
**Response 200:** Array of `InventoryResponse` sorted by distance

**Distance formula (Haversine — implement in service layer):**
```java
private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
    double R = 6371;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a = Math.sin(dLat/2) * Math.sin(dLat/2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLng/2) * Math.sin(dLng/2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}
```

#### PUT `/api/v1/inventory/{id}/stock`
Dealer manually updates their own stock level (e.g., after a small local purchase).

**Auth:** `DEALER` — can only update their own inventory  
**Request Body:**
```json
{ "availableStock": 60 }
```
**Response 200:** Updated `InventoryResponse`

#### POST `/api/v1/inventory`
Create inventory record for a dealer. Called by ADMIN after registering a dealer.

**Auth:** `ADMIN`  
**Request Body:**
```json
{
  "dealerId": "uuid",
  "dealerName": "Silva Gas Station",
  "address": "45/B Galle Road, Colombo 03",
  "latitude": 6.9271,
  "longitude": 79.8612,
  "availableStock": 0
}
```
**Response 201:** Created `InventoryResponse`

### Kafka: Consume `allocation.confirmed`

When Member 2's allocation-service confirms a delivery, this service receives the event and adds the quantity to the dealer's stock.

```java
@Component
@RequiredArgsConstructor
public class AllocationConfirmedConsumer {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, StockUpdatedEvent> kafkaTemplate;

    @KafkaListener(topics = "allocation.confirmed", groupId = "inventory-service")
    public void handleAllocationConfirmed(AllocationConfirmedEvent event) {
        Inventory inventory = inventoryRepository.findByDealerId(event.getDealerId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for dealer"));

        inventory.setAvailableStock(inventory.getAvailableStock() + event.getQuantity());
        inventoryRepository.save(inventory);

        // Notify queue-service that stock changed
        kafkaTemplate.send("stock.updated", StockUpdatedEvent.builder()
                .dealerId(inventory.getDealerId())
                .dealerName(inventory.getDealerName())
                .address(inventory.getAddress())
                .availableStock(inventory.getAvailableStock())
                .latitude(inventory.getLatitude())
                .longitude(inventory.getLongitude())
                .build());
    }
}
```

### Security Rules

```java
// SecurityConfig permitted paths
.requestMatchers("/api/v1/inventory/test", "/actuator/health").permitAll()
.anyRequest().authenticated()

// Method-level (on controller methods)
@PreAuthorize("hasRole('ADMIN')")                                 // POST /
@PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal")  // PUT stock
// GET endpoints — just authenticated (no role restriction)
```

### Checklist

- [ ] Create Spring Boot project with correct dependencies
- [ ] Copy `JwtAuthFilter.java` and `SecurityConfig.java` from user-service (update permitted paths)
- [ ] Copy `GlobalExceptionHandler.java`, `ResourceNotFoundException.java`, `ErrorResponse.java` from user-service
- [ ] Create `Inventory` entity and repository
- [ ] Implement `InventoryService` with all business logic
- [ ] Implement `InventoryController` with all endpoints
- [ ] Implement `AllocationConfirmedConsumer` (Kafka listener)
- [ ] Implement `StockUpdatedEvent` publisher
- [ ] Add Haversine distance calculation for `/nearby` endpoint
- [ ] Register in Eureka (verify on dashboard at http://localhost:8761)
- [ ] Notify team lead to add gateway route for `/api/v1/inventory/**`

---

## 7. Member 2 — Allocation Service

### Responsibility
Manage the gas supply workflow. Dealers submit requests for cylinders. Admin reviews and approves or rejects. Dealer confirms when the delivery arrives. On confirmation, emits an event so inventory is updated automatically.

### Service Configuration

**File:** `allocation-service/src/main/resources/application.yaml`
```yaml
server:
  port: 8083

spring:
  application:
    name: allocation-service
  datasource:
    url: jdbc:postgresql://localhost:5432/allocationdb
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Folder Structure

```
allocation-service/src/main/java/com/gastracker/allocation_service/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── KafkaProducerConfig.java
├── controller/
│   └── AllocationController.java
├── dao/
│   ├── entity/
│   │   └── Allocation.java
│   └── repository/
│       └── AllocationRepository.java
├── dto/
│   ├── request/
│   │   ├── AllocationRequest.java
│   │   └── RejectRequest.java
│   └── response/
│       ├── AllocationResponse.java
│       └── ErrorResponse.java
├── enums/
│   └── AllocationStatus.java
├── event/
│   └── AllocationConfirmedEvent.java   ← published
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── InvalidStateException.java
│   └── GlobalExceptionHandler.java
└── service/
    ├── AllocationService.java
    └── transformer/
        └── AllocationTransformer.java
```

### Enums

```java
public enum AllocationStatus {
    PENDING,    // dealer submitted, waiting for admin
    APPROVED,   // admin approved, awaiting delivery
    REJECTED,   // admin rejected
    DELIVERED   // dealer confirmed receipt
}
```

### Database Entity

```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "allocations")
public class Allocation {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String dealerId;           // UUID from user-service

    @Column(nullable = false)
    private String dealerName;

    @Column(nullable = false)
    private Integer requestedQuantity;

    private Integer approvedQuantity;  // set by admin on approval

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    private String rejectionReason;    // set by admin on rejection

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime resolvedAt;  // when approved or rejected

    private LocalDateTime deliveredAt; // when dealer confirms

    @PrePersist
    protected void onCreate() { requestedAt = LocalDateTime.now(); }
}
```

### API Endpoints

#### POST `/api/v1/allocations/request`
Dealer submits a gas cylinder request.

**Auth:** `DEALER`  
**Request Body:**
```json
{
  "requestedQuantity": 100,
  "dealerName": "Silva Gas Station"
}
```
**Business Logic:** Read `dealerId` from `authentication.getPrincipal()` (the JWT subject). Set status to `PENDING`.  
**Response 201:**
```json
{
  "id": "uuid",
  "dealerId": "uuid",
  "dealerName": "Silva Gas Station",
  "requestedQuantity": 100,
  "approvedQuantity": null,
  "status": "PENDING",
  "rejectionReason": null,
  "requestedAt": "2026-04-30T09:00:00",
  "resolvedAt": null,
  "deliveredAt": null
}
```

#### GET `/api/v1/allocations/pending`
Admin views all pending allocation requests.

**Auth:** `ADMIN`  
**Response 200:** Array of `AllocationResponse` where `status == PENDING`

#### GET `/api/v1/allocations`
Admin views all allocations (all statuses).

**Auth:** `ADMIN`  
**Query Params (optional):** `status=PENDING|APPROVED|REJECTED|DELIVERED`  
**Response 200:** Array of `AllocationResponse`

#### PUT `/api/v1/allocations/{id}/approve`
Admin approves and sets the approved quantity (may differ from requested).

**Auth:** `ADMIN`  
**Request Body:**
```json
{ "approvedQuantity": 80 }
```
**Business Logic:** Validate current status is `PENDING`. Set status to `APPROVED`, set `resolvedAt`.  
**Response 200:** Updated `AllocationResponse`  
**Error 409:** If status is not `PENDING`

#### PUT `/api/v1/allocations/{id}/reject`
Admin rejects with a reason.

**Auth:** `ADMIN`  
**Request Body:**
```json
{ "reason": "Quota exceeded for this quarter" }
```
**Business Logic:** Validate current status is `PENDING`. Set status to `REJECTED`, set `resolvedAt`.  
**Response 200:** Updated `AllocationResponse`

#### PUT `/api/v1/allocations/{id}/confirm`
Dealer confirms the delivery has physically arrived.

**Auth:** `DEALER`  
**Business Logic:**
1. Validate current status is `APPROVED`
2. Validate the calling dealer owns this allocation (`dealerId == authentication.getPrincipal()`)
3. Set status to `DELIVERED`, set `deliveredAt`
4. **Publish `allocation.confirmed` Kafka event** with `dealerId` and `approvedQuantity`

**Response 200:** Updated `AllocationResponse`

#### GET `/api/v1/allocations/dealer/{dealerId}`
Dealer views their own allocation history.

**Auth:** `DEALER` — enforce `#dealerId == authentication.principal`  
**Response 200:** Array of `AllocationResponse`

### Kafka: Publish `allocation.confirmed`

```java
@Service
@RequiredArgsConstructor
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final KafkaTemplate<String, AllocationConfirmedEvent> kafkaTemplate;

    public AllocationResponse confirmDelivery(String id, String dealerId) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found"));

        if (!allocation.getDealerId().equals(dealerId)) {
            throw new AccessDeniedException("You do not own this allocation");
        }
        if (allocation.getStatus() != AllocationStatus.APPROVED) {
            throw new InvalidStateException("Only APPROVED allocations can be confirmed");
        }

        allocation.setStatus(AllocationStatus.DELIVERED);
        allocation.setDeliveredAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        kafkaTemplate.send("allocation.confirmed", AllocationConfirmedEvent.builder()
                .allocationId(allocation.getId())
                .dealerId(allocation.getDealerId())
                .quantity(allocation.getApprovedQuantity())
                .build());

        return allocationTransformer.toResponse(allocation);
    }
}
```

### State Machine (Allowed Transitions)

```
PENDING ──► APPROVED  (admin approves)
PENDING ──► REJECTED  (admin rejects)
APPROVED ──► DELIVERED (dealer confirms)

Any other transition must throw InvalidStateException (409)
```

### Security Rules

```java
.requestMatchers("/api/v1/allocations/test", "/actuator/health").permitAll()
.anyRequest().authenticated()

// Method-level
@PreAuthorize("hasRole('DEALER')")                                           // POST /request
@PreAuthorize("hasRole('ADMIN')")                                            // GET /pending, PUT approve/reject
@PreAuthorize("hasRole('DEALER') and #dealerId == authentication.principal") // GET /dealer/{dealerId}
@PreAuthorize("hasRole('DEALER')")                                           // PUT /confirm (ownership checked in service)
```

### Checklist

- [ ] Create Spring Boot project with correct dependencies
- [ ] Copy security files from user-service (JwtAuthFilter, SecurityConfig, GlobalExceptionHandler)
- [ ] Create `AllocationStatus` enum
- [ ] Create `Allocation` entity and repository
- [ ] Create `InvalidStateException` (returns HTTP 409)
- [ ] Implement `AllocationService` with state machine logic
- [ ] Implement `AllocationController` with all endpoints
- [ ] Implement Kafka producer for `allocation.confirmed`
- [ ] Register in Eureka (verify on dashboard at http://localhost:8761)
- [ ] Notify team lead to add gateway route for `/api/v1/allocations/**`

---

## 8. Member 3 — Queue Service & Notification Service

### Responsibility

**queue-service:** Each dealer has a virtual queue. Citizens join the queue and receive a token number. The dealer advances the queue when they serve a customer. Citizens can check their position without going to the shop.

**notification-service:** Listens to queue events on Kafka and sends alerts to citizens (email/log for now — real SMS requires a third-party provider).

---

### 8.1 Queue Service

#### Service Configuration

**File:** `queue-service/src/main/resources/application.yaml`
```yaml
server:
  port: 8084

spring:
  application:
    name: queue-service
  data:
    mongodb:
      uri: mongodb://localhost:27017/queuedb
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: queue-service
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### Folder Structure

```
queue-service/src/main/java/com/gastracker/queue_service/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── KafkaConfig.java
├── controller/
│   └── QueueController.java
├── dao/
│   ├── document/
│   │   ├── Token.java
│   │   └── DealerStock.java         ← local stock cache from Kafka events
│   └── repository/
│       ├── TokenRepository.java
│       └── DealerStockRepository.java
├── dto/
│   ├── request/
│   │   └── TokenRequest.java
│   └── response/
│       ├── TokenResponse.java
│       ├── DealerAvailabilityResponse.java
│       └── ErrorResponse.java
├── enums/
│   └── TokenStatus.java
├── event/
│   ├── StockUpdatedEvent.java       ← consumed from inventory-service
│   ├── TokenIssuedEvent.java        ← published
│   └── TokenCalledEvent.java        ← published
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── NoStockAvailableException.java
│   └── GlobalExceptionHandler.java
└── service/
    ├── QueueService.java
    └── transformer/
        └── TokenTransformer.java
```

#### MongoDB Documents

```java
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "tokens")
public class Token {

    @Id
    private String id;

    private String dealerId;
    private String dealerName;
    private String citizenId;        // from JWT principal
    private String citizenEmail;

    private Integer tokenNumber;     // sequential per dealer (1, 2, 3...)

    @Enumerated(EnumType.STRING)
    private TokenStatus status;      // WAITING, CALLED, COMPLETED, CANCELLED, EXPIRED

    private LocalDateTime issuedAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
}
```

```java
// Local cache of dealer stock — updated by Kafka, never hits inventory-service directly
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "dealer_stock_cache")
public class DealerStock {

    @Id
    private String dealerId;

    private String dealerName;
    private String address;
    private Integer availableStock;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
}
```

#### TokenStatus Enum

```java
public enum TokenStatus {
    WAITING,    // in queue, not yet called
    CALLED,     // dealer called this token — citizen must come now
    COMPLETED,  // dealer marked as served
    CANCELLED,  // citizen cancelled
    EXPIRED     // citizen did not show up after being called
}
```

#### API Endpoints

##### POST `/api/v1/queue/request`
Citizen requests a token from a specific dealer.

**Auth:** `CITIZEN`  
**Request Body:**
```json
{ "dealerId": "uuid" }
```
**Business Logic:**
1. Check local `DealerStock` cache — if `availableStock == 0`, throw `NoStockAvailableException`
2. Check citizen does not already have a `WAITING` or `CALLED` token for this dealer
3. Find the highest existing `tokenNumber` for this dealer, increment by 1
4. Save token with status `WAITING`
5. Publish `token.issued` Kafka event

**Response 201:**
```json
{
  "id": "uuid",
  "tokenNumber": 12,
  "dealerName": "Silva Gas Station",
  "status": "WAITING",
  "queuePosition": 5,
  "issuedAt": "2026-04-30T10:00:00"
}
```
**Error 400:** No stock available  
**Error 409:** Citizen already has an active token for this dealer

##### GET `/api/v1/queue/token/{tokenId}`
Citizen checks their own token status.

**Auth:** `CITIZEN` — enforce ownership (`token.citizenId == authentication.principal`)  
**Response 200:**
```json
{
  "id": "uuid",
  "tokenNumber": 12,
  "dealerName": "Silva Gas Station",
  "status": "WAITING",
  "queuePosition": 5,
  "issuedAt": "2026-04-30T10:00:00",
  "calledAt": null
}
```

##### GET `/api/v1/queue/dealer/{dealerId}`
View the current waiting queue for a dealer (public queue board).

**Auth:** Any authenticated  
**Response 200:** Array of `TokenResponse` where `status == WAITING`, sorted by `tokenNumber`

##### PUT `/api/v1/queue/{dealerId}/next`
Dealer calls the next token in their queue.

**Auth:** `DEALER` — enforce `#dealerId == authentication.principal`  
**Business Logic:**
1. Find the lowest-numbered `WAITING` token for this dealer
2. Set its status to `CALLED`, set `calledAt`
3. Publish `token.called` Kafka event

**Response 200:** The called `TokenResponse`  
**Error 404:** No tokens waiting

##### PUT `/api/v1/queue/token/{tokenId}/complete`
Dealer marks a token as served.

**Auth:** `DEALER`  
**Business Logic:** Set status `CALLED` → `COMPLETED`. Reduce dealer's local stock cache by 1. Publish `stock.updated` event.  
**Response 200:** Updated `TokenResponse`

##### DELETE `/api/v1/queue/token/{tokenId}`
Citizen cancels their own token.

**Auth:** `CITIZEN` — enforce ownership  
**Business Logic:** Only `WAITING` tokens can be cancelled. Set status to `CANCELLED`.  
**Response 204:** No content

##### GET `/api/v1/queue/dealers/nearby?lat={lat}&lng={lng}&radius={km}`
List dealers with stock in the area (reads from local `DealerStock` cache).

**Auth:** Any authenticated  
**Response 200:** Array of `DealerAvailabilityResponse` sorted by distance

#### Kafka: Consume `stock.updated`

```java
@KafkaListener(topics = "stock.updated", groupId = "queue-service")
public void handleStockUpdated(StockUpdatedEvent event) {
    DealerStock cache = dealerStockRepository.findById(event.getDealerId())
            .orElse(DealerStock.builder().dealerId(event.getDealerId()).build());
    cache.setDealerName(event.getDealerName());
    cache.setAddress(event.getAddress());
    cache.setAvailableStock(event.getAvailableStock());
    cache.setLatitude(event.getLatitude());
    cache.setLongitude(event.getLongitude());
    cache.setUpdatedAt(LocalDateTime.now());
    dealerStockRepository.save(cache);
}
```

#### Kafka: Publish `token.issued` and `token.called`

```java
// After saving a new token
kafkaTemplate.send("token.issued", TokenIssuedEvent.builder()
        .tokenId(token.getId())
        .tokenNumber(token.getTokenNumber())
        .citizenId(token.getCitizenId())
        .citizenEmail(token.getCitizenEmail())
        .dealerName(token.getDealerName())
        .queuePosition(queuePosition)
        .build());

// After calling next token
kafkaTemplate.send("token.called", TokenCalledEvent.builder()
        .tokenId(token.getId())
        .tokenNumber(token.getTokenNumber())
        .citizenId(token.getCitizenId())
        .citizenEmail(token.getCitizenEmail())
        .dealerName(token.getDealerName())
        .build());
```

---

### 8.2 Notification Service

#### Service Configuration

**File:** `notification-service/src/main/resources/application.yaml`
```yaml
server:
  port: 8085

spring:
  application:
    name: notification-service
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service
      auto-offset-reset: earliest

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### Folder Structure

```
notification-service/src/main/java/com/gastracker/notification_service/
├── consumer/
│   ├── TokenIssuedConsumer.java
│   └── TokenCalledConsumer.java
├── event/
│   ├── TokenIssuedEvent.java
│   └── TokenCalledEvent.java
└── service/
    └── NotificationService.java
```

#### Implementation

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenIssuedConsumer {

    @KafkaListener(topics = "token.issued", groupId = "notification-service")
    public void handle(TokenIssuedEvent event) {
        // Replace with real email/SMS provider (e.g., Twilio, SendGrid)
        log.info("[NOTIFICATION] Token issued to {} — Token #{} at {}. Queue position: {}",
                event.getCitizenEmail(),
                event.getTokenNumber(),
                event.getDealerName(),
                event.getQueuePosition());
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCalledConsumer {

    @KafkaListener(topics = "token.called", groupId = "notification-service")
    public void handle(TokenCalledEvent event) {
        log.info("[NOTIFICATION] Token #{} CALLED at {}. Citizen {} please proceed.",
                event.getTokenNumber(),
                event.getDealerName(),
                event.getCitizenEmail());
    }
}
```

> For production, replace `log.info` with an email sender (Spring Mail) or SMS API (Twilio).

### Member 3 Checklist

**Queue Service:**
- [ ] Create Spring Boot project with MongoDB dependency
- [ ] Copy security files from user-service
- [ ] Create `Token` and `DealerStock` MongoDB documents
- [ ] Create `TokenStatus` enum
- [ ] Implement `QueueService` with all business logic
- [ ] Implement `QueueController` with all endpoints
- [ ] Implement Kafka consumer for `stock.updated`
- [ ] Implement Kafka publishers for `token.issued` and `token.called`
- [ ] Add Haversine distance for `/dealers/nearby`
- [ ] Register in Eureka

**Notification Service:**
- [ ] Create Spring Boot project (web + kafka only, no DB)
- [ ] Implement `TokenIssuedConsumer`
- [ ] Implement `TokenCalledConsumer`
- [ ] Register in Eureka

---

## 9. Integration Guide

### API Gateway Routes

Member 1 must add these routes to `api-gateway/src/main/resources/application.yaml`:

```yaml
spring:
  cloud:
    gateway:
      server:
        webmvc:
          routes:
            - id: user-service
              uri: lb://user-service
              predicates:
                - Path=/api/v1/users/**
              filters:
                - StripPrefix=0

            - id: inventory-service
              uri: lb://inventory-service
              predicates:
                - Path=/api/v1/inventory/**
              filters:
                - StripPrefix=0

            - id: allocation-service
              uri: lb://allocation-service
              predicates:
                - Path=/api/v1/allocations/**
              filters:
                - StripPrefix=0

            - id: queue-service
              uri: lb://queue-service
              predicates:
                - Path=/api/v1/queue/**
              filters:
                - StripPrefix=0
```

### Copying Security Files

Every new service needs these files copied from user-service and updated:

1. `JwtAuthFilter.java` — copy as-is, only change the package declaration
2. `SecurityConfig.java` — copy and update the permitted paths for the new service
3. `GlobalExceptionHandler.java` — copy as-is, only change package
4. `ResourceNotFoundException.java`, `ErrorResponse.java` — copy as-is

### Docker Compose Addition

Add these to the root `docker-compose.yml`:

```yaml
# Kafka (required for Member 2 and 3)
zookeeper:
  image: confluentinc/cp-zookeeper:7.4.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
  networks:
    - lpg-network

kafka:
  image: confluentinc/cp-kafka:7.4.0
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"
  environment:
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
  networks:
    - lpg-network

postgres-allocation:
  image: postgres:15
  environment:
    POSTGRES_DB: allocationdb
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
  ports:
    - "5434:5432"
  networks:
    - lpg-network

mongodb:
  image: mongo:7
  ports:
    - "27017:27017"
  networks:
    - lpg-network
```

### Integration Test Scenario (Run After All Services Are Up)

```
Step 1 — Admin registers a dealer
  POST /api/v1/users/register/dealer  [ADMIN token]

Step 2 — Admin creates inventory record for dealer
  POST /api/v1/inventory              [ADMIN token]

Step 3 — Dealer requests gas allocation
  POST /api/v1/allocations/request    [DEALER token]

Step 4 — Admin approves allocation
  PUT /api/v1/allocations/{id}/approve [ADMIN token]

Step 5 — Dealer confirms delivery
  PUT /api/v1/allocations/{id}/confirm [DEALER token]
  → Kafka: allocation.confirmed fires
  → inventory-service adds stock
  → Kafka: stock.updated fires
  → queue-service cache updates

Step 6 — Citizen registers and logs in
  POST /api/v1/users/register

Step 7 — Citizen finds nearby dealers
  GET /api/v1/queue/dealers/nearby?lat=6.9271&lng=79.8612

Step 8 — Citizen requests a token
  POST /api/v1/queue/request          [CITIZEN token]
  → Kafka: token.issued fires
  → notification-service logs alert

Step 9 — Dealer calls next token
  PUT /api/v1/queue/{dealerId}/next   [DEALER token]
  → Kafka: token.called fires
  → notification-service logs alert

Step 10 — Dealer marks token complete
  PUT /api/v1/queue/token/{id}/complete [DEALER token]
```

---

## 10. Quality Attributes

### Fault Tolerance — Resilience4j Circuit Breaker

Add to any service that calls another service synchronously via Feign:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

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
        return InventoryResponse.builder().availableStock(0).build(); // safe default
    }
}
```

### Availability

- Every service exposes `/actuator/health` — Eureka uses this to detect unhealthy instances
- Stateless JWT design means any instance can serve any request — enables horizontal scaling

### Consistency

- Kafka guarantees the `allocation.confirmed` event is not lost — inventory will eventually be updated even if inventory-service is temporarily down
- Token numbers are assigned sequentially using `findTopByDealerIdOrderByTokenNumberDesc()` — race conditions must be handled with `@Transactional` on the issue method

### Observability

Add to every service's `application.yaml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

---

## 11. Project Timeline

| Week | Team Lead | Member 1 | Member 2 | Member 3 |
|---|---|---|---|---|
| **Week 1** | Add gateway routes for all 3 new services; update docker-compose; support team setup | Set up inventory-service project; create entity and repository; implement all GET endpoints and POST (create) | Set up allocation-service project; create entity and repository; implement request + approve/reject endpoints | Set up queue-service project; create MongoDB documents; implement token request and status endpoints |
| **Week 2** | Code review; resolve integration blockers | Implement PUT stock update; wire Kafka consumer for `allocation.confirmed`; publish `stock.updated` event | Implement confirm endpoint + Kafka producer for `allocation.confirmed`; add state machine validation | Implement Kafka consumer for `stock.updated`; build notification-service; implement next/complete/cancel endpoints |
| **Week 3** | Final integration test lead; documentation | Integration test with Member 2 (allocation → stock flow) | Integration test with Member 1; verify stock updates flow end-to-end | Integration test full citizen flow; verify token.called notification works |

---

*Document prepared for CO4353 Distributed Systems — Mini Project, April 2026*
