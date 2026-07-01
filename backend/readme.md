# LPG Distribution & Queue Management System

A microservices-based smart LPG distribution and queue management system built to solve the real-world problem of LPG shortages and long queues in Sri Lanka. Citizens can check stock availability and claim virtual tokens remotely, eliminating the need to physically wait in queues without knowing whether gas is available.

---

## System Architecture

Event-Driven Microservices Architecture using the Netflix OSS stack.

```
                        ┌─────────────────┐
                        │   API Gateway   │
                        │   (Port 8080)   │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
     ┌────────▼───────┐ ┌───────▼────────┐ ┌───────▼────────┐
     │  User Service  │ │Inventory Service│ │ Queue Service  │
     │  (Port 8081)   │ │  (Port 8082)   │ │  (Port 8083)   │
     └────────┬───────┘ └───────┬────────┘ └───────┬────────┘
              │                  │                  │
     ┌────────▼───────┐ ┌───────▼────────┐ ┌───────▼────────┐
     │  PostgreSQL    │ │  PostgreSQL    │ │    MongoDB     │
     │  (userdb)      │ │  (inventorydb) │ │  (queuedb)     │
     └────────────────┘ └────────────────┘ └────────────────┘

                        ┌─────────────────┐
                        │  Eureka Server  │
                        │   (Port 8761)   │
                        └─────────────────┘
```

---

## Services Overview

| Service | Port | Database | Status |
|---|---|---|---|
| Discovery Server (Eureka) | 8761 | — | Active |
| API Gateway | 8080 | — | Active |
| User Service | 8081 | PostgreSQL (5432) | Active |
| Inventory Service | 8082 | PostgreSQL (5433) | Active |
| Queue Service | 8083 | MongoDB (27017) | Not yet implemented |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Inter-service Communication | OpenFeign |
| ORM | Spring Data JPA / Hibernate |
| Relational Database | PostgreSQL 15 |
| Document Database | MongoDB 7 (planned) |
| Security | Spring Security + JWT |
| Containerization | Docker + Docker Compose |
| Build Tool | Maven |

---

## Prerequisites

- [Java 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Git](https://git-scm.com/)

---

## Project Structure

```
lpg-system/
├── discovery-server/           # Eureka Service Registry
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── api-gateway/                # Spring Cloud Gateway
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── user-service/               # User & Auth Service
│   ├── src/
│   │   └── main/java/com/gastracker/user_service/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dao/
│   │       ├── dto/
│   │       ├── enums/
│   │       └── service/
│   ├── Dockerfile
│   └── pom.xml
├── inventory-service/          # Inventory & Stock Service
│   ├── src/
│   │   └── main/java/com/gastracker/inventory_service/
│   │       ├── controller/
│   │       ├── dao/
│   │       ├── dto/
│   │       ├── enums/
│   │       └── service/
│   ├── Dockerfile
│   └── pom.xml
├── build-all.bat               # Windows: build all services at once
├── docker-compose.yml          # Full system orchestration
└── readme.md
```

---

## Getting Started

### Run Full System — Backend + Frontend

Start the full system with one Docker Compose command.

```bash
cd "/home/ros/Downloads/distributed project/smart-gas-distribution-platform"
docker compose up --build
```

Run in background:

```bash
cd "/home/ros/Downloads/distributed project/smart-gas-distribution-platform"
docker compose up --build -d
```

This starts the frontend, backend services, databases, Kafka, Eureka, and the API Gateway.

Important URLs:

| URL | Description |
|---|---|
| http://localhost:3000 | Frontend application |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8080 | API Gateway |
| http://localhost:8080/actuator/health | API Gateway health check |

The frontend container serves the TanStack Start app on port `3000`. Requests to `/api/...` are proxied to `api-gateway:8080` inside the Compose network.

Stop the system with `Ctrl+C`. If it was started in detached mode, stop it with:

```bash
cd "/home/ros/Downloads/distributed project/smart-gas-distribution-platform"
docker compose down
```

---

### Option 1 — Backend Only With Docker Compose

Starts all active services and databases with a single command.

**Step 1 — Clone the repository**

```bash
git clone <repo-url>
cd lpg-system
```

**Step 2 — Build all services**

Windows:
```bat
build-all.bat
```

Linux / macOS:
```bash
cd discovery-server && ./mvnw clean package -DskipTests && cd ..
cd user-service     && ./mvnw clean package -DskipTests && cd ..
cd inventory-service && ./mvnw clean package -DskipTests && cd ..
cd api-gateway      && ./mvnw clean package -DskipTests && cd ..
```

**Step 3 — Start everything**

```bash
docker compose up --build
```

Run in background:
```bash
docker compose up --build -d
```

**Step 4 — Verify services**

| URL | Description |
|---|---|
| http://localhost:8761 | Eureka Dashboard — all services should appear |
| http://localhost:8080/api/v1/users/test | User Service test endpoint (via Gateway) |
| http://localhost:8080/api/v1/inventory/test | Inventory Service test endpoint (via Gateway) |

---

### Option 2 — Local Development (databases in Docker, services local)

Use this during development for faster iteration without rebuilding Docker images.

**Step 1 — Start only the databases and Eureka**

```bash
docker compose up postgres-user postgres-inventory eureka-server
```

**Step 2 — Run each service in a separate terminal**

```bash
# Terminal 1 — User Service (port 8081)
cd user-service
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows

# Terminal 2 — Inventory Service (port 8082)
cd inventory-service
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows

# Terminal 3 — API Gateway (port 8080)
cd api-gateway
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

> Start services in this order: Eureka → User Service → Inventory Service → API Gateway

---

## Test Endpoints

Quick smoke-test to confirm each service is up. All requests go through the API Gateway on port `8080`.

| Method | URL | Expected Response |
|---|---|---|
| GET | http://localhost:8080/api/v1/users/test | `{"service":"user-service","status":"ok","message":"User Service is running"}` |
| GET | http://localhost:8080/api/v1/inventory/test | `{"service":"inventory-service","status":"ok","message":"Inventory Service is running"}` |

---

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`.

### User Service — `/api/v1/users`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/v1/users/test` | Service health check | No |
| POST | `/api/v1/users/register` | Citizen self-registration | No |
| POST | `/api/v1/users/register/dealer` | Register a dealer | ADMIN |
| POST | `/api/v1/users/login` | Login, returns JWT | No |
| GET | `/api/v1/users/me` | Get own profile | Yes |
| GET | `/api/v1/users/{id}` | Get user by ID | Own or ADMIN |
| PUT | `/api/v1/users/{id}` | Update user profile | Own or ADMIN |
| GET | `/api/v1/users/role/{role}` | Get users by role | ADMIN |
| DELETE | `/api/v1/users/{id}` | Delete user | ADMIN |

**Citizen Registration** — public endpoint, role is always set to `CITIZEN`. NIC is the unique identifier; no two accounts can share the same NIC.

```json
POST /api/v1/users/register
{
  "nic": "123456789V",
  "email": "alice@example.com",
  "password": "secret123",
  "name": "Alice Perera"
}
```

NIC formats accepted:
- Old format: 9 digits + `V` or `X` — e.g. `123456789V`
- New format: 12 digits — e.g. `200012345678`

**Dealer Registration** — ADMIN only. Role is always set to `DEALER`. Requires business details.

```json
POST /api/v1/users/register/dealer
Authorization: Bearer <admin-token>

{
  "nic": "987654321X",
  "email": "dealer@gasstation.lk",
  "password": "secure456",
  "name": "Kamal Silva",
  "phone": "0771234567",
  "address": "45/B Galle Road, Colombo 03",
  "businessName": "Silva Gas Station",
  "businessRegNo": "BRN-2024-00123"
}
```

**Login** — authenticate with NIC and password.

```json
POST /api/v1/users/login
{
  "nic": "123456789V",
  "password": "secret123"
}
```

Both register and login return an `AuthResponse`:

```json
{
  "token": "<JWT>",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "nic": "123456789V",
    "email": "alice@example.com",
    "name": "Alice Perera",
    "role": "CITIZEN",
    "phone": null,
    "address": null,
    "businessName": null,
    "businessRegNo": null,
    "createdAt": "2026-04-30T10:00:00"
  }
}
```

For dealers, `phone`, `address`, `businessName`, and `businessRegNo` are populated.

**Protected endpoints** — pass the token in the `Authorization` header:

```
Authorization: Bearer <token>
```

**Ownership rule** — `GET /{id}` and `PUT /{id}` enforce ownership: a CITIZEN or DEALER can only access their own profile. The user's UUID (returned at login) must match the `{id}` path variable. ADMIN bypasses this check.

### Inventory Service — `/api/v1/inventory`

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | `/api/v1/inventory/test` | Service health check | No |
| POST | `/api/v1/inventory` | Add dealer inventory | DEALER |
| GET | `/api/v1/inventory/{id}` | Get inventory by ID | Yes |
| GET | `/api/v1/inventory/dealer/{dealerId}` | Get dealer stock | Yes |
| GET | `/api/v1/inventory/available` | Get all dealers with stock | Yes |
| PUT | `/api/v1/inventory/{id}/stock` | Update stock level | DEALER |
| GET | `/api/v1/inventory/location/{location}` | Get stock by location | Yes |

---

## User Roles

| Role | Description | Key Permissions |
|---|---|---|
| `CITIZEN` | Registered public user | Claim tokens, check stock |
| `DEALER` | LPG dealer / distributor | Update stock, advance queue |
| `ADMIN` | Government / system admin | Full access, reporting |

---

## Inter-Service Communication

Services communicate via OpenFeign through the Eureka service registry:

```
Queue Service     ──► Inventory Service   (verify stock before issuing token)
Queue Service     ──► User Service        (verify citizen identity)
Inventory Service ──► User Service        (verify dealer role before stock update)
```

No hardcoded URLs — Eureka resolves service locations dynamically.

---

## Docker Commands

```bash
# Start all services
docker compose up

# Start in background
docker compose up -d

# Rebuild and start
docker compose up --build

# Stop (data preserved)
docker compose down

# Stop and wipe all data (fresh start)
docker compose down -v

# View logs for a specific service
docker compose logs -f user-service
docker compose logs -f inventory-service

# Restart a single service
docker compose restart inventory-service

# Rebuild a single service after a code change
docker compose up --build user-service

# Check running containers
docker ps

# Inspect the shared network
docker network inspect lpg-network
```

---

## Actuator Endpoints

```
# Service health
GET http://localhost:8761/actuator/health   # Eureka
GET http://localhost:8081/actuator/health   # User Service
GET http://localhost:8082/actuator/health   # Inventory Service
GET http://localhost:8080/actuator/health   # API Gateway

# All registered routes (Gateway)
GET http://localhost:8080/actuator/gateway/routes
```

---

## API Gateway Notes

This project uses **Spring Cloud Gateway MVC** (`spring-cloud-starter-gateway-server-webmvc`), the servlet-based (non-reactive) variant. It runs on Tomcat instead of Netty.

The correct configuration namespace for routes and discovery is:

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
              filters:
                - StripPrefix=0
```

> **Note:** Using `spring.cloud.gateway.routes` (reactive namespace) or `spring.cloud.gateway.mvc.routes` (old MVC namespace) will silently register no routes and all requests will return 404. The correct prefix is `spring.cloud.gateway.server.webmvc`.
