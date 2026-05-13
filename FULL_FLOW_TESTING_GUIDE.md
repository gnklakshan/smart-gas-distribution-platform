# 🔥 Smart Gas Distribution Platform — Full Flow Testing Guide

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Starting All Services](#2-starting-all-services)
3. [TablePlus Database Connections](#3-tableplus-database-connections)
4. [Full Flow Testing (Postman / cURL)](#4-full-flow-testing)
5. [Database Verification at Each Step](#5-database-verification-at-each-step)
6. [Kafka Event Verification](#6-kafka-event-verification)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerequisites

- **Java 17** installed
- **Maven** installed (or use `./mvnw`)
- **Docker Desktop** running
- **Postman** or **cURL** for API testing
- **TablePlus** for database inspection
- **Make** (optional) — for one-shot commands. Install via: `choco install make` or `scoop install make`

---

## 2. Starting All Services

### 🚀 Quick Start with Makefile (One-Shot Commands)

If you have `make` installed, use these shortcuts from the project root:

```bash
cd "c:\fyp\fyp\testing codes\smart-gas-distribution-platform"
```

| Command | What it does |
|---|---|
| `make run-all` | **⭐ One-shot: starts infra + all 7 services** in separate windows |
| `make dev` | Starts infra only, prints instructions to run services manually |
| `make up` | Builds all JARs + starts everything inside Docker |
| `make build` | Builds all service JARs (no start) |
| `make infra` | Starts only databases + Kafka |
| `make health` | Checks health of all services |
| `make stop` | Stops all Docker containers |
| `make clean` | Stops containers + deletes all DB data (fresh start) |
| `make ps` | Shows running containers |
| `make logs` | Tail Docker container logs |
| `make kafka-topics` | Lists all Kafka topics |
| `make restart` | Full restart (stop + rebuild + start in Docker) |
| `make help` | Show all available commands |

**One-shot to start everything locally (recommended for dev):**
```bash
make run-all
```
This will:
1. Start all databases + Kafka in Docker
2. Wait for them to be healthy
3. Launch Eureka discovery server (waits 30s for startup)
4. Launch all 6 microservices in separate terminal windows
5. Each window shows live logs for that service

**One-shot to start everything in Docker:**
```bash
make up
```
This will:
1. Build all 7 service JARs (`mvn clean package -DskipTests`)
2. Build Docker images from Dockerfiles
3. Start all containers (infra + services)

**After starting, verify:**
```bash
make health
```

---

### Manual Setup (Without Make)

There are **two ways** to run the platform manually. Pick one:

| | Option A (Recommended for Dev) | Option B (Fully Dockerized) |
|---|---|---|
| **Infrastructure** | Docker Compose | Docker Compose |
| **Spring Boot services** | Run locally (`mvn spring-boot:run`) | Run inside Docker containers |
| **Build step** | Not needed (Maven compiles on the fly) | Must run `mvn package` first for each service |
| **Debugging** | Easy (IDE breakpoints, hot-reload) | Harder (need to rebuild image each change) |

---

### Option A: Local Development (Recommended)

#### Step 1: Start ONLY Infrastructure via Docker Compose

This starts **only** the databases + Kafka — NOT the Spring Boot services:

```bash
cd "c:\fyp\fyp\testing codes\smart-gas-distribution-platform"
docker-compose up -d zookeeper kafka postgres-user postgres-inventory postgres-allocation postgres-queue postgres-notification
```

Wait for all containers to be healthy:

```bash
docker-compose ps
```

You should see these containers running:

| Container | Port | Status |
|---|---|---|
| `zookeeper` | 2181 | healthy |
| `kafka` | 9092 | healthy |
| `postgres-user` | 5432 | healthy |
| `postgres-inventory` | 5433 | healthy |
| `postgres-allocation` | 5434 | healthy |
| `postgres-queue` | 5435 | healthy |
| `postgres-notification` | 5436 | healthy |

#### Step 2: Start Discovery Server (Eureka)

```bash
cd discovery-server
mvn spring-boot:run
```

Wait until you see: `Started DiscoveryServerApplication` and verify at: http://localhost:8761

#### Step 3: Start Each Microservice (in separate terminals)

Open **6 separate terminals** from the project root directory:

**Terminal 1 — User Service (port 8081):**
```bash
cd user-service
mvn spring-boot:run
```

**Terminal 2 — Inventory Service (port 8082):**
```bash
cd inventory-service
mvn spring-boot:run
```

**Terminal 3 — Allocation Service (port 8083):**
```bash
cd allocation-service
mvn spring-boot:run
```

**Terminal 4 — Queue Service (port 8084):**
```bash
cd queue-service
mvn spring-boot:run
```

**Terminal 5 — Notification Service (port 8085):**
```bash
cd notification-service
mvn spring-boot:run
```

**Terminal 6 — API Gateway (port 8080):**
```bash
cd api-gateway
mvn spring-boot:run
```

> 💡 **Alternatively**, you can run each service from your **IDE** (IntelliJ / VS Code) by right-clicking the `*Application.java` main class and selecting "Run". This lets you use breakpoints and hot-reload.

---

### Option B: Fully Dockerized

> ⚠️ **Important:** The Dockerfiles just copy a pre-built JAR file — they do NOT build the code. You must build each service FIRST with Maven.

#### Step 1: Build ALL services (create JAR files)

```bash
cd "c:\fyp\fyp\testing codes\smart-gas-distribution-platform"

cd discovery-server && mvn clean package -DskipTests && cd ..
cd user-service && mvn clean package -DskipTests && cd ..
cd inventory-service && mvn clean package -DskipTests && cd ..
cd allocation-service && mvn clean package -DskipTests && cd ..
cd queue-service && mvn clean package -DskipTests && cd ..
cd notification-service && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
```

Each command creates a `target/*.jar` file that the Dockerfile copies:
```dockerfile
# What each Dockerfile does — it just copies the JAR, no compilation:
FROM eclipse-temurin:17-jdk-alpine
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Step 2: Start EVERYTHING via Docker Compose

```bash
docker-compose up -d --build
```

The `--build` flag forces Docker to rebuild images from the Dockerfiles (using the JARs you just built).

#### Step 3: Check all containers are running

```bash
docker-compose ps
```

> ⚠️ If you change any Java code, you need to repeat Step 1 (`mvn package`) and then `docker-compose up -d --build` for the changed service.

---

### Step 4: Verify All Services Registered

Open http://localhost:8761 — you should see all 5 services registered:

- `USER-SERVICE`
- `INVENTORY-SERVICE`
- `ALLOCATION-SERVICE`
- `QUEUE-SERVICE`
- `NOTIFICATION-SERVICE`
- `API-GATEWAY`

### Health Check (Quick Verification)

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8080/actuator/health
```

All should return `{"status":"UP"}`.

---

## 3. TablePlus Database Connections

Create **5 separate connections** in TablePlus:

| Connection Name | Host | Port | User | Password | Database |
|---|---|---|---|---|---|
| 🟦 UserDB | `localhost` | `5440` | `postgres` | `postgres` | `userdb` |
| 🟩 InventoryDB | `localhost` | `5433` | `postgres` | `postgres` | `inventorydb` |
| 🟧 AllocationDB | `localhost` | `5434` | `postgres` | `postgres` | `allocationdb` |
| 🟪 QueueDB | `localhost` | `5435` | `postgres` | `postgres` | `queuedb` |
| 🟥 NotificationDB | `localhost` | `5436` | `postgres` | `postgres` | `notificationdb` |

### How to Create a Connection in TablePlus

1. Open TablePlus → Click **"Create a new connection"**
2. Select **PostgreSQL**
3. Fill in: Name, Host (`localhost`), Port (see table), User (`postgres`), Password (`postgres`), Database (see table)
4. Click **"Test"** to verify → then **"Connect"**
5. Repeat for all 5 databases

### Tables You Should See in Each Database

| Database | Tables |
|---|---|
| **userdb** | `users`, `dealers` |
| **inventorydb** | `cylinder_types`, `inventory` |
| **allocationdb** | `allocations` |
| **queuedb** | `citizen_queues` |
| **notificationdb** | `notifications` |

> ⚠️ Tables are auto-created by Hibernate (`ddl-auto: update`) when each service starts. If tables are missing, make sure the corresponding service has started successfully.

---

## 4. Full Flow Testing

> All requests go through the **API Gateway** at `http://localhost:8080`.
> You can also call services directly on their ports.

### 🔐 Save These Variables

After each registration/login, save the JWT token. You'll need it for authenticated requests.

---

### FLOW 1: User Registration & Authentication

#### 1.1 Register a Citizen

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "nic": "200012345678",
    "email": "citizen1@test.com",
    "password": "password123",
    "name": "Kamal Perera",
    "phone": "0771234567"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "some-uuid",
    "nic": "200012345678",
    "email": "citizen1@test.com",
    "name": "Kamal Perera",
    "role": "CITIZEN",
    "phone": "0771234567",
    "dealer": null
  }
}
```

> 📋 **SAVE** the `token` as `CITIZEN_TOKEN`
> 📋 **SAVE** the `user.id` as `CITIZEN_ID`

**✅ TablePlus Check (UserDB):**
- Open `users` table → new row with role=`CITIZEN`
- Open `dealers` table → no new row (citizens don't have dealer records)

**✅ TablePlus Check (NotificationDB):**
- Open `notifications` table → new WELCOME notification for this user

---

#### 1.2 Register an Admin (if not seeded)

Login with the seeded admin:

```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "nic": "199001230001",
    "password": "Pass123!"
  }'
```

> 📋 **SAVE** the `token` as `ADMIN_TOKEN`

> ⚠️ The seeded password hash is for "password" — if it doesn't work, you may need to update the seed SQL with a hash matching your encoder.

---

#### 1.3 Admin Registers a Dealer

```bash
curl -X POST http://localhost:8080/api/v1/users/register/dealer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "nic": "956789012V",
    "email": "newdealer@test.com",
    "password": "password123",
    "name": "Nimal Fernando",
    "phone": "0779876543",
    "address": "78 Main Street, Galle",
    "businessName": "Galle Gas Mart",
    "businessRegNo": "REG-2026-099",
    "latitude": 6.0535,
    "longitude": 80.2210
  }'
```

**Expected Response:** AuthResponse with role=`DEALER` and nested `dealer` object.

> 📋 **SAVE** the `user.id` as `DEALER_USER_ID`
> 📋 **SAVE** the `user.dealer.dealerId` as `DEALER_ID`

**✅ TablePlus Check (UserDB):**
- `users` → new row with role=`DEALER`
- `dealers` → new row with `user_id` matching the user, `business_name`="Galle Gas Mart"

**✅ TablePlus Check (NotificationDB):**
- 2 new notifications: `WELCOME` + `DEALER_REGISTERED`

---

#### 1.4 Login as Dealer

```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "nic": "956789012V",
    "password": "password123"
  }'
```

> 📋 **SAVE** the `token` as `DEALER_TOKEN`

---

### FLOW 2: Cylinder Types & Inventory Setup

#### 2.1 Admin Creates Cylinder Types

```bash
curl -X POST http://localhost:8080/api/v1/cylinder-types \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "name": "12.5kg Domestic",
    "capacityKg": 12.5
  }'
```

```bash
curl -X POST http://localhost:8080/api/v1/cylinder-types \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "name": "5kg Domestic",
    "capacityKg": 5.0
  }'
```

> 📋 **SAVE** the `id` from the first response as `CYLINDER_TYPE_12KG`
> 📋 **SAVE** the `id` from the second response as `CYLINDER_TYPE_5KG`

**✅ TablePlus Check (InventoryDB):**
- `cylinder_types` → 2 new rows

#### 2.2 Get All Cylinder Types (public)

```bash
curl http://localhost:8080/api/v1/cylinder-types \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

---

#### 2.3 Admin Creates Inventory for the Dealer

```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "dealerId": "<DEALER_USER_ID>",
    "cylinderTypeId": "<CYLINDER_TYPE_12KG>",
    "availableStock": 50
  }'
```

```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "dealerId": "<DEALER_USER_ID>",
    "cylinderTypeId": "<CYLINDER_TYPE_5KG>",
    "availableStock": 100
  }'
```

**✅ TablePlus Check (InventoryDB):**
- `inventory` → 2 rows for this dealer, one per cylinder type

#### 2.4 Check Dealer's Inventory

```bash
curl http://localhost:8080/api/v1/inventory/dealer/<DEALER_USER_ID> \
  -H "Authorization: Bearer <DEALER_TOKEN>"
```

---

### FLOW 3: Allocation (Dealer → Admin → Delivery)

#### 3.1 Dealer Requests Allocation

```bash
curl -X POST http://localhost:8080/api/v1/allocations/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DEALER_TOKEN>" \
  -d '{
    "cylinderTypeId": "<CYLINDER_TYPE_12KG>",
    "requestedQuantity": 200
  }'
```

> 📋 **SAVE** the `id` as `ALLOCATION_ID`

**✅ TablePlus Check (AllocationDB):**
- `allocations` → new row with status=`PENDING`, `cylinder_type_id` set

**✅ TablePlus Check (NotificationDB):**
- New `ALLOCATION_REQUESTED` notification for the dealer

---

#### 3.2 Admin Approves Allocation

```bash
curl -X PUT http://localhost:8080/api/v1/allocations/<ALLOCATION_ID>/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "approvedQuantity": 150
  }'
```

**✅ TablePlus Check (AllocationDB):**
- `allocations` → status changed to `APPROVED`, `approved_quantity`=150, `resolved_at` set

**✅ TablePlus Check (NotificationDB):**
- New `ALLOCATION_APPROVED` notification

---

#### 3.3 Dealer Confirms Delivery

```bash
curl -X PUT http://localhost:8080/api/v1/allocations/<ALLOCATION_ID>/confirm \
  -H "Authorization: Bearer <DEALER_TOKEN>"
```

**✅ TablePlus Check (AllocationDB):**
- `allocations` → status=`DELIVERED`, `delivered_at` set

**✅ TablePlus Check (InventoryDB):**
- `inventory` → **`available_stock` increased by 150** for the 12.5kg row! (was 50, now 200)

**✅ TablePlus Check (NotificationDB):**
- New `ALLOCATION_CONFIRMED` notification

> 🎯 **KEY VERIFICATION**: The Kafka event `allocation.confirmed` was consumed by inventory-service, which automatically added 150 units to stock!

---

### FLOW 4: Queue (Citizen → Dealer → Pickup)

#### 4.1 Citizen Joins Queue

```bash
curl -X POST http://localhost:8080/api/v1/queue/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <CITIZEN_TOKEN>" \
  -d '{
    "dealerId": "<DEALER_USER_ID>",
    "cylinderTypeId": "<CYLINDER_TYPE_12KG>"
  }'
```

> 📋 **SAVE** the `id` as `QUEUE_ID`
> 📋 **NOTE** the `tokenNumber` (e.g., `TKN-ABCD1234`)

**✅ TablePlus Check (QueueDB):**
- `citizen_queues` → new row with status=`WAITING`

**✅ TablePlus Check (NotificationDB):**
- 2 new `QUEUE_JOINED` notifications: one for the citizen, one for the dealer

---

#### 4.2 Citizen Checks Their Queue Status

```bash
curl http://localhost:8080/api/v1/queue/my \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

---

#### 4.3 Dealer Views Their Queue

```bash
curl http://localhost:8080/api/v1/queue/dealer/<DEALER_USER_ID> \
  -H "Authorization: Bearer <DEALER_TOKEN>"
```

---

#### 4.4 Dealer Marks Citizen as Ready for Pickup

```bash
curl -X PUT http://localhost:8080/api/v1/queue/<QUEUE_ID>/ready \
  -H "Authorization: Bearer <DEALER_TOKEN>"
```

**✅ TablePlus Check (QueueDB):**
- status changed to `READY_FOR_PICKUP`

**✅ TablePlus Check (NotificationDB):**
- New `QUEUE_READY` notification for the citizen ("Your gas cylinder is ready for pickup!")

---

#### 4.5 Dealer Completes Pickup

```bash
curl -X PUT http://localhost:8080/api/v1/queue/<QUEUE_ID>/complete \
  -H "Authorization: Bearer <DEALER_TOKEN>"
```

**✅ TablePlus Check (QueueDB):**
- status=`COMPLETED`, `fulfilled_at` set

**✅ TablePlus Check (InventoryDB):**
- `inventory` → **`available_stock` decreased by 1** for the 12.5kg row! (was 200, now 199)

**✅ TablePlus Check (NotificationDB):**
- New `QUEUE_COMPLETED` notification

> 🎯 **KEY VERIFICATION**: The Kafka event `queue.completed` was consumed by inventory-service, which automatically subtracted 1 unit from stock!

---

### FLOW 5: Notifications

#### 5.1 Citizen Checks Notifications

```bash
curl http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

Should return notifications for: WELCOME, QUEUE_JOINED, QUEUE_READY, QUEUE_COMPLETED.

#### 5.2 Get Unread Count

```bash
curl http://localhost:8080/api/v1/notifications/count \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

#### 5.3 Mark One as Read

```bash
curl -X PUT http://localhost:8080/api/v1/notifications/<NOTIFICATION_ID>/read \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

#### 5.4 Mark All as Read

```bash
curl -X PUT http://localhost:8080/api/v1/notifications/read-all \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

**✅ TablePlus Check (NotificationDB):**
- `is_read`=true, `read_at` timestamp set for all citizen's notifications

---

### FLOW 6: Rejection Flow (Alternative Path)

#### 6.1 Dealer Requests Another Allocation

```bash
curl -X POST http://localhost:8080/api/v1/allocations/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <DEALER_TOKEN>" \
  -d '{
    "cylinderTypeId": "<CYLINDER_TYPE_5KG>",
    "requestedQuantity": 500
  }'
```

> 📋 **SAVE** the `id` as `ALLOCATION_ID_2`

#### 6.2 Admin Rejects It

```bash
curl -X PUT http://localhost:8080/api/v1/allocations/<ALLOCATION_ID_2>/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "reason": "Requested quantity exceeds monthly limit"
  }'
```

**✅ TablePlus Check (AllocationDB):**
- status=`REJECTED`, `rejection_reason` set

**✅ TablePlus Check (InventoryDB):**
- `inventory` → stock **NOT changed** (rejections don't affect inventory)

**✅ TablePlus Check (NotificationDB):**
- New `ALLOCATION_REJECTED` notification with the reason

---

### FLOW 7: Queue Cancellation

#### 7.1 Citizen Joins Another Queue

```bash
curl -X POST http://localhost:8080/api/v1/queue/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <CITIZEN_TOKEN>" \
  -d '{
    "dealerId": "<DEALER_USER_ID>",
    "cylinderTypeId": "<CYLINDER_TYPE_5KG>"
  }'
```

> 📋 **SAVE** the `id` as `QUEUE_ID_2`

#### 7.2 Citizen Cancels It

```bash
curl -X PUT http://localhost:8080/api/v1/queue/<QUEUE_ID_2>/cancel \
  -H "Authorization: Bearer <CITIZEN_TOKEN>"
```

**✅ TablePlus Check (QueueDB):**
- status=`CANCELLED`

**✅ TablePlus Check (InventoryDB):**
- stock **NOT changed** (cancellations don't affect inventory)

**✅ TablePlus Check (NotificationDB):**
- New `QUEUE_CANCELLED` notification

---

## 5. Database Verification at Each Step

### Quick SQL Queries for TablePlus

**UserDB — Check all users and dealers:**
```sql
SELECT u.id, u.name, u.role, u.email, d.business_name, d.address 
FROM users u 
LEFT JOIN dealers d ON d.user_id = u.id
ORDER BY u.created_at;
```

**InventoryDB — Check stock levels:**
```sql
SELECT i.id, i.dealer_id, ct.name as cylinder_type, i.available_stock, i.last_updated
FROM inventory i
JOIN cylinder_types ct ON ct.id = i.cylinder_type_id
ORDER BY i.dealer_id, ct.name;
```

**AllocationDB — Check allocation history:**
```sql
SELECT id, dealer_id, cylinder_type_id, requested_quantity, approved_quantity, 
       status, rejection_reason, requested_at, resolved_at, delivered_at
FROM allocations
ORDER BY requested_at DESC;
```

**QueueDB — Check active queues:**
```sql
SELECT id, user_id, dealer_id, cylinder_type_id, token_number, status, requested_at, fulfilled_at
FROM citizen_queues
ORDER BY requested_at DESC;
```

**NotificationDB — Check all notifications:**
```sql
SELECT id, user_id, type, title, 
       SUBSTRING(message, 1, 60) as message_preview, 
       reference_type, is_read, created_at
FROM notifications
ORDER BY created_at DESC;
```

---

## 6. Kafka Event Verification

### Check Kafka Topics Exist

```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Expected topics:
```
allocation.requested
allocation.approved
allocation.rejected
allocation.confirmed
queue.joined
queue.ready
queue.completed
queue.cancelled
user.registered
dealer.registered
```

### Watch Events in Real-Time

Open a separate terminal and listen to all events:

```bash
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic allocation.confirmed \
  --from-beginning
```

Replace the topic name to watch different events.

---

## 7. Troubleshooting

### Service Won't Start

| Problem | Solution |
|---|---|
| `Connection refused` to PostgreSQL | Check `docker-compose ps` — is the postgres container healthy? |
| `Connection refused` to Kafka | Wait 30s after `docker-compose up` for Kafka to initialize |
| `Eureka not found` | Start discovery-server first, wait for it to fully start |
| Port already in use | Kill the process: `netstat -ano | findstr :8081` then `taskkill /PID <PID> /F` |

### Kafka Events Not Working

| Problem | Solution |
|---|---|
| No notification created | Check notification-service logs for consumer errors |
| Stock not updating | Check inventory-service logs for `allocation.confirmed` consumer |
| `Deserialization error` | Ensure `spring.json.trusted.packages: "*"` in consumer YAML |

### Database Issues

| Problem | Solution |
|---|---|
| Tables not created | Verify service started without errors (Hibernate runs on startup) |
| Seed data conflicts | Drop and recreate: `docker-compose down -v` then `docker-compose up -d` |
| Foreign key errors in seeds | Run seeds in order: users → dealers → cylinder_types → inventory → allocations |

---

## Complete Flow Summary Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FULL FLOW SEQUENCE                            │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. REGISTER CITIZEN    ──→  users table  ──→  WELCOME notification  │
│                                                                      │
│  2. REGISTER DEALER     ──→  users + dealers  ──→  2 notifications   │
│                                                                      │
│  3. CREATE CYLINDER     ──→  cylinder_types table                    │
│     TYPES (Admin)                                                    │
│                                                                      │
│  4. CREATE INVENTORY    ──→  inventory table (per dealer per type)   │
│     (Admin)                                                          │
│                                                                      │
│  5. ALLOCATION FLOW:                                                 │
│     Dealer Request      ──→  allocations (PENDING)                   │
│          │                    └──→ Kafka: allocation.requested        │
│          │                         └──→ notification created          │
│     Admin Approve       ──→  allocations (APPROVED)                  │
│          │                    └──→ Kafka: allocation.approved         │
│          │                         └──→ notification created          │
│     Dealer Confirm      ──→  allocations (DELIVERED)                 │
│          │                    └──→ Kafka: allocation.confirmed        │
│          │                         ├──→ inventory STOCK ADDED ✅      │
│          │                         └──→ notification created          │
│                                                                      │
│  6. QUEUE FLOW:                                                      │
│     Citizen Join        ──→  citizen_queues (WAITING)                │
│          │                    └──→ Kafka: queue.joined                │
│          │                         └──→ 2 notifications created      │
│     Dealer Mark Ready   ──→  citizen_queues (READY_FOR_PICKUP)      │
│          │                    └──→ Kafka: queue.ready                 │
│          │                         └──→ notification (citizen)        │
│     Dealer Complete     ──→  citizen_queues (COMPLETED)              │
│          │                    └──→ Kafka: queue.completed             │
│          │                         ├──→ inventory STOCK REDUCED ✅    │
│          │                         └──→ notification created          │
│                                                                      │
│  7. CHECK NOTIFICATIONS ──→  All notifications for each user         │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

> **Tip:** Import all the cURL commands into **Postman** by creating a new collection. Set a `{{BASE_URL}}` variable to `http://localhost:8080` and `{{TOKEN}}` variable for each role.
