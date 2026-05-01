# Member 2 — Allocation Service: Work Done

**Service:** `allocation-service`
**Port:** `8083`
**Database:** PostgreSQL — `allocationdb`
**Branch:** `sathmi/allocation`
**Status:** ✅ Complete

---

## What Was Built

### Service Overview

The allocation service manages the gas cylinder supply workflow between the government admin and dealers. Dealers submit requests, the admin approves or rejects them, and upon physical delivery confirmation a Kafka event fires to update inventory automatically.

---

## Folder Structure

```
allocation-service/
├── Dockerfile
├── pom.xml
└── src/main/java/com/gastracker/allocation_service/
    ├── AllocationServiceApplication.java
    ├── config/
    │   ├── GlobalExceptionHandler.java     ← handles all exceptions → standard error JSON
    │   ├── JwtAuthFilter.java              ← validates Bearer JWT on every request
    │   ├── KafkaProducerConfig.java        ← Kafka producer with idempotence + retries
    │   └── SecurityConfig.java             ← stateless JWT security + @PreAuthorize
    ├── controller/
    │   └── AllocationController.java       ← all 8 REST endpoints
    ├── dao/
    │   ├── entity/
    │   │   └── Allocation.java             ← JPA entity mapped to `allocations` table
    │   └── repository/
    │       └── AllocationRepository.java   ← Spring Data JPA queries
    ├── dto/
    │   ├── request/
    │   │   ├── AllocationRequest.java      ← dealer submits (quantity + dealerName)
    │   │   ├── ApproveRequest.java         ← admin sets approvedQuantity
    │   │   └── RejectRequest.java          ← admin sets rejection reason
    │   └── response/
    │       ├── AllocationResponse.java     ← full allocation state returned to client
    │       └── ErrorResponse.java          ← standard error format
    ├── enums/
    │   └── AllocationStatus.java           ← PENDING | APPROVED | REJECTED | DELIVERED
    ├── event/
    │   └── AllocationConfirmedEvent.java   ← Kafka payload: {allocationId, dealerId, quantity}
    ├── exception/
    │   ├── InvalidStateException.java      ← 409 on illegal state transitions
    │   └── ResourceNotFoundException.java  ← 404 when allocation not found
    └── service/
        ├── AllocationService.java          ← all business logic + Kafka publish
        ├── helper/
        │   └── JwtHelper.java              ← JWT validation/extraction (read-only)
        └── transformer/
            └── AllocationTransformer.java  ← entity → response DTO
```

---

## REST API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/allocations/test` | Public | Health check |
| POST | `/api/v1/allocations/request` | `DEALER` | Submit new allocation request |
| GET | `/api/v1/allocations/pending` | `ADMIN` | List all PENDING allocations |
| GET | `/api/v1/allocations` | `ADMIN` | List all allocations (optional `?status=` filter) |
| PUT | `/api/v1/allocations/{id}/approve` | `ADMIN` | Approve + set approved quantity |
| PUT | `/api/v1/allocations/{id}/reject` | `ADMIN` | Reject with reason |
| PUT | `/api/v1/allocations/{id}/confirm` | `DEALER` | Confirm physical delivery → fires Kafka |
| GET | `/api/v1/allocations/dealer/{dealerId}` | `DEALER` (own only) | Dealer's allocation history |
| GET | `/api/v1/allocations/{id}` | Any authenticated | Get single allocation |

---

## State Machine

```
PENDING ──► APPROVED   (admin approves  → PUT /approve)
PENDING ──► REJECTED   (admin rejects   → PUT /reject)
APPROVED ──► DELIVERED (dealer confirms → PUT /confirm)

Any other transition throws InvalidStateException (HTTP 409)
```

---

## Kafka Integration

**Topic published:** `allocation.confirmed`

**Trigger:** When a dealer calls `PUT /api/v1/allocations/{id}/confirm` and the allocation status is `APPROVED`.

**Event payload:**
```json
{
  "allocationId": "uuid-string",
  "dealerId": "uuid-string",
  "quantity": 80
}
```

**Consumed by:** `inventory-service` (Member 1) — adds quantity to dealer's stock, then publishes `stock.updated`.

**Fault tolerance on publish:**
- `acks=all` — waits for all replicas before confirming write
- `retries=3` with 1-second backoff
- `enable.idempotence=true` — prevents duplicate messages on retry
- Async callback logs errors without blocking the HTTP response

---

## Fault Tolerance

| Mechanism | Implementation |
|---|---|
| Kafka producer retries | `retries=3`, `retry.backoff.ms=1000` |
| Exactly-once delivery | `enable.idempotence=true` + `acks=all` |
| Circuit breaker (Feign) | `spring-cloud-starter-circuitbreaker-resilience4j` wired, ready for inter-service calls |
| State machine validation | `InvalidStateException` (409) prevents illegal transitions |
| Global exception handler | All exceptions caught → consistent error JSON, no stack traces leaked |

---

## Security

- JWT validated on every request via `JwtAuthFilter` (reads the same secret as `user-service`)
- Role enforcement via `@PreAuthorize` at the method level:
  - `hasRole('DEALER')` on submit + confirm
  - `hasRole('ADMIN')` on approve + reject + list all
  - Dealer ownership enforced in service layer on confirm (`dealerId == JWT principal`)
- Session is stateless — no server-side session stored

---

## Database

**Auto-created table:** `allocations`

| Column | Type | Notes |
|---|---|---|
| `id` | UUID | PK, auto-generated |
| `dealer_id` | VARCHAR | From JWT principal (user-service UUID) |
| `dealer_name` | VARCHAR | Submitted by dealer |
| `requested_quantity` | INTEGER | Dealer's ask |
| `approved_quantity` | INTEGER | Set by admin on approval |
| `status` | VARCHAR | Enum: PENDING / APPROVED / REJECTED / DELIVERED |
| `rejection_reason` | VARCHAR | Set by admin on rejection |
| `requested_at` | TIMESTAMP | Auto-set on persist |
| `resolved_at` | TIMESTAMP | Set on approve/reject |
| `delivered_at` | TIMESTAMP | Set on dealer confirmation |

---

## Docker / Infrastructure Changes

The following were added to the root `docker-compose.yml`:

| Container | Image | Port | Purpose |
|---|---|---|---|
| `zookeeper` | confluentinc/cp-zookeeper:7.4.0 | — | Kafka coordination |
| `kafka` | confluentinc/cp-kafka:7.4.0 | 9092 | Message broker |
| `postgres-allocation` | postgres:15 | 5434 | allocationdb |
| `mongodb` | mongo:7 | 27017 | For Member 3 (queue-service) |
| `allocation-service` | built from `./allocation-service` | 8083 | This service |

The api-gateway route `/api/v1/allocations/**` → `lb://allocation-service` was also added.

---

## Integration Points

### For Member 1 (inventory-service)

Subscribe to Kafka topic `allocation.confirmed`:
```json
{ "allocationId": "...", "dealerId": "...", "quantity": 80 }
```
Add `quantity` to `inventory.availableStock` for the given `dealerId`, then publish `stock.updated`.

### For the Team Lead

The allocation-service gateway route has been added to `api-gateway/src/main/resources/application.yaml`. No other changes were made to shared services.

---

## How to Build and Run

```bash
# 1 — Build the JAR (from allocation-service directory)
cd allocation-service
mvn clean package -DskipTests

# 2 — Start all services
cd ..
docker-compose up -d

# 3 — Verify registration
curl http://localhost:8761   # Eureka dashboard — check ALLOCATION-SERVICE appears

# 4 — Test health
curl http://localhost:8080/api/v1/allocations/test
```

---

## Example Request Flow

```
# Dealer logs in (user-service) → gets JWT
POST /api/v1/users/login

# Dealer submits allocation request
POST /api/v1/allocations/request
Authorization: Bearer <DEALER_JWT>
{ "requestedQuantity": 100, "dealerName": "Silva Gas Station" }

# Admin approves
PUT /api/v1/allocations/{id}/approve
Authorization: Bearer <ADMIN_JWT>
{ "approvedQuantity": 80 }

# Dealer confirms delivery → triggers Kafka event → inventory updated
PUT /api/v1/allocations/{id}/confirm
Authorization: Bearer <DEALER_JWT>
```

---

*CO4353 Distributed Systems — Mini Project | Member 2 | April 2026*
