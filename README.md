# Smart Gas Distribution Platform

A microservices-based platform that solves LPG (cooking gas) shortages and queue chaos in Sri Lanka. Citizens can search nearby gas dealers, see live stock and prices per cylinder type, and join a virtual pickup queue remotely — instead of physically waiting in line without knowing whether gas is even available. Dealers manage stock and serve their queue digitally, and admins oversee dealer onboarding, cylinder allocation to dealers, and pricing.

**Backend repository:** https://github.com/gnklakshan/smart-gas-distribution-platform
**Frontend repository:** https://github.com/sathmipeiris/gas-flow-hub

---

## Table of Contents

1. [Introduction](#introduction)
2. [Architecture](#architecture)
3. [Microservices](#microservices)
4. [User Interface](#user-interface)
5. [Deployment](#deployment)
6. [Source Code](#source-code)
7. [References](#references)

---

## Introduction

### Use case

In Sri Lanka, LPG cylinder shortages regularly force citizens to queue for hours at gas stations with no guarantee that stock will still be available by the time they reach the front. This platform digitizes that process end-to-end:

- **Citizens** find nearby dealers with live stock and pricing, and join a virtual queue instead of a physical one.
- **Dealers** manage their stock per cylinder type, serve their queue (call next / mark ready / mark completed), and request cylinder allocations from the central supply chain.
- **Admins** onboard dealers, approve or reject allocation requests, and set the platform-wide price for each cylinder type.

### Core features

| Feature | Description |
|---|---|
| Role-based accounts | `CITIZEN`, `DEALER`, `ADMIN` — JWT authentication, enforced server-side per endpoint |
| Geolocation search | Haversine-distance search for dealers within a radius, with live per-cylinder-type stock |
| Virtual queueing | Citizens join a queue for a specific cylinder type and dealer; dealers advance the queue with a live token system |
| Stock & allocation | Dealers request cylinder stock from admins; admins approve/reject; confirmed deliveries update dealer inventory automatically |
| Dynamic pricing | Admins set/change the price of each cylinder type; citizens see current prices when browsing dealers |
| Notifications | Event-driven notifications for registration, allocation status changes, and queue status changes |
| Analytics | Dealers see allocation/queue analytics; admins see platform-wide fulfilment stats |

### Goal

Reduce wasted travel and physical waiting time for citizens, give dealers digital tools to manage stock and serve customers fairly (first-come-first-served token queue), and give the government/admin layer visibility and control over gas distribution across dealers.

---

## Architecture

### Architectural Diagram

<img width="1541" height="772" alt="image" src="https://github.com/user-attachments/assets/33c52f8c-ec36-4e53-9f15-36e0bb64ac93" />


Everything above runs in Docker containers on a shared bridge network (`docker-compose.yml`), including the 5 PostgreSQL instances, Kafka + Zookeeper, all 6 Spring Boot services, the API Gateway, the Eureka server, and the frontend.

### Design decisions

**Why split into microservices?** Each service owns a distinct bounded context and its own database — no service reaches into another's tables directly:

| Service | Owns | Why separate |
|---|---|---|
| `user-service` | Identity, authentication, dealer profiles/geolocation | Auth is a cross-cutting concern every other service depends on (via JWT), but its data model (accounts, roles, NIC validation) is unrelated to stock or queueing |
| `inventory-service` | Stock levels per dealer/cylinder type, cylinder type catalog & pricing | Stock changes at high frequency (every pickup/delivery) and needed its own scaling/consistency boundary, separate from user identity |
| `allocation-service` | Dealer → admin cylinder supply requests and approval workflow | A distinct workflow/state machine (`PENDING → APPROVED/REJECTED → DELIVERED`) that doesn't belong in inventory or user logic |
| `queue-service` | Citizen pickup queue and token issuance | High write-throughput, ephemeral, workflow-driven (`WAITING → READY_FOR_PICKUP → COMPLETED/CANCELLED`) — isolating it means queue load never impacts stock or user queries |
| `notification-service` | User-facing notifications | Purely reactive to events from every other service; keeping it separate means notification logic/schema can evolve without touching producers |
| `discovery-server` + `api-gateway` | Service registry and single entry point | Standard Netflix OSS pattern — lets every backend service scale/move independently while clients (the frontend) only ever talk to one stable host:port |

**Communication style:** synchronous **OpenFeign** calls are used only where a request needs an immediate answer to build its response (e.g. enriching a "nearby dealers" result with live stock). Everything else — cross-service side effects like "update inventory when a delivery is confirmed" or "notify a citizen when their token is called" — goes through **Kafka** so services stay decoupled and don't fail if a downstream service is temporarily down.

**One database per service:** avoids hidden coupling through shared tables and lets each team member/service evolve its schema independently — a core microservices principle applied literally here (`userdb`, `inventorydb`, `allocationdb`, `queuedb`, `notificationdb`, all PostgreSQL 15).

---

## Microservices

### Implementation methods — Netflix OSS stack

| Concern | Technology |
|---|---|
| Service discovery/registry | **Netflix Eureka** (`spring-cloud-starter-netflix-eureka-server` / `-client`) |
| API Gateway / edge routing | **Spring Cloud Gateway** (servlet/MVC variant — `spring-cloud-starter-gateway-server-webmvc`) |
| Client-side load balancing | Spring Cloud LoadBalancer, resolving `lb://service-name` URIs via Eureka |
| Synchronous inter-service calls | **OpenFeign** (`spring-cloud-starter-openfeign`) declarative HTTP clients |
| Asynchronous inter-service events | **Apache Kafka** (`spring-kafka`) + Zookeeper |
| Auth | Spring Security + JWT (`jjwt`), stateless, validated per-request in each service |
| Persistence | Spring Data JPA / Hibernate over PostgreSQL 15 (one instance per service) |
| Build/runtime | Java 17, Spring Boot 3.5, Spring Cloud 2025.0.2, Maven, Docker |

Every service follows the same layered package structure (`controller → service → dao/{entity,repository} → dto/{request,response} → exception`), documented internally in [`guide.md`](guide.md).

---

### Discovery Server (Eureka)

**Role:** central registry all services register themselves with on startup, and query to resolve where a peer service currently lives (host/port can change — nothing is hardcoded).

- Runs standalone on port `8761`, does **not** register with itself (`register-with-eureka: false`, `fetch-registry: false`).
- Every other service declares `spring-cloud-starter-netflix-eureka-client` and points `eureka.client.service-url.defaultZone` at `http://eureka-server:8761/eureka/`.
- On startup each service sends a `POST /eureka/apps/{APP}` registration and then a heartbeat (`PUT`) every 30s by default to renew its lease; Eureka evicts instances that miss heartbeats, so the registry self-heals if a service crashes.
- Dashboard at `http://localhost:8761` shows every registered instance, its status (`UP`/`DOWN`), and metadata — used during the demo to show live service health.

### API Gateway

**Role:** the single entry point for the frontend (and any external client) — clients only ever call `http://localhost:8080`, never a backend service directly.

Configuration (`api-gateway/src/main/resources/application.yaml`):
- **Manual route table** (auto-discovery/`discovery.locator` deliberately disabled) — each route explicitly maps a path predicate to `lb://{service-name}`, so the more specific `/api/v1/inventory/nearby` route (→ `user-service`) can be declared *before* the catch-all `/api/v1/inventory/**` (→ `inventory-service`) and correctly take priority.
- **CORS** configured globally to allow the frontend origin (`http://localhost:3000`) with all standard methods and credentials.
- **Load balancing**: every route URI uses the `lb://` scheme, so Spring Cloud LoadBalancer resolves the actual instance via Eureka at request time.
- Exposes `/actuator/gateway/routes` and `/actuator/health` for inspecting the live route table and gateway health.

| Route ID | Path predicate | Forwards to |
|---|---|---|
| `user-service` | `/api/v1/users/**` | `user-service` |
| `inventory-nearby` | `/api/v1/inventory/nearby` (exact) | `user-service` (composite endpoint) |
| `inventory-service` | `/api/v1/inventory/**` | `inventory-service` |
| `cylinder-types` | `/api/v1/cylinder-types/**` | `inventory-service` |
| `allocation-service` | `/api/v1/allocations/**` | `allocation-service` |
| `queue-service` | `/api/v1/queue/**` | `queue-service` |
| `notification-service` | `/api/v1/notifications/**` | `notification-service` |

---

### Core services

#### 1. User Service (`:8081`)

**Functionality:** account registration/login, JWT issuance, profile management, dealer geolocation storage, and dealer discovery (nearby search).

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users/register` | Public | Citizen self-registration (NIC, email, password, name) |
| POST | `/api/v1/users/register/dealer` | ADMIN | Register a dealer, including business name/address and shop `latitude`/`longitude` |
| POST | `/api/v1/users/login` | Public | Authenticate with NIC + password, returns JWT + user profile |
| GET | `/api/v1/users/me` | Any authenticated | Get the caller's own profile |
| GET | `/api/v1/users/{id}` | ADMIN, self, or any authenticated user looking up a **dealer** | Get a user's profile (dealer profiles are treated as public storefront info) |
| PUT | `/api/v1/users/{id}` | ADMIN or self | Update a profile |
| GET | `/api/v1/users/role/{role}` | ADMIN | List all users of a given role |
| DELETE | `/api/v1/users/{id}` | ADMIN | Delete a user account |
| GET | `/api/v1/users/dealers/nearby?lat&lng&radius` | Any authenticated | Haversine SQL distance query over the `dealers` table, radius in km |
| GET | `/api/v1/inventory/nearby?lat&lng&radius` | Any authenticated | Composite endpoint: runs the same nearby-dealer query, then enriches each dealer with **live stock** via a Feign call to `inventory-service`, filtering out zero-stock dealers |

**Inter-service interactions:**
- **Consumes** `inventory-service` synchronously via `InventoryClient` (OpenFeign) to enrich nearby-search results with live stock (`GET /api/v1/inventory/dealer/{dealerId}`).
- **Publishes** `user.registered` and `dealer.registered` Kafka events (consumed by `notification-service` to send a welcome notification).

#### 2. Inventory Service (`:8082`)

**Functionality:** per-dealer, per-cylinder-type stock levels; the cylinder type catalog (name, capacity, price); stock change history/auditing.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/inventory` | ADMIN | Create an initial stock record for a dealer + cylinder type |
| GET | `/api/v1/inventory/{id}` | Any authenticated | Get a single stock record |
| GET | `/api/v1/inventory/dealer/{dealerId}` | Any authenticated | Get all stock records (per cylinder type) for a dealer |
| GET | `/api/v1/inventory/available` | Any authenticated | List all dealers that currently have stock |
| PUT | `/api/v1/inventory/{id}/stock` | DEALER | Update stock level for a record; logs a `StockHistory` entry |
| GET | `/api/v1/inventory/dealer/{dealerId}/stock-history` | DEALER (self) | Audit trail of stock changes — manual updates, allocation-confirmed additions, queue-completed deductions |
| POST | `/api/v1/cylinder-types` | ADMIN | Create a new cylinder type (name, capacity, price) |
| GET | `/api/v1/cylinder-types` | Any authenticated | List all cylinder types with current prices |
| GET | `/api/v1/cylinder-types/{id}` | Any authenticated | Get a single cylinder type |
| PUT | `/api/v1/cylinder-types/{id}/price` | ADMIN | Set/change the price of a cylinder type |

**Inter-service interactions:**
- **Consumes** `user-service` synchronously via `UserClient` (OpenFeign) where dealer identity needs verifying.
- **Consumes** Kafka events asynchronously: `allocation.confirmed` (adds stock when a dealer confirms a delivery) and `queue.completed` (deducts one unit of stock when a citizen's pickup is completed).

#### 3. Allocation Service (`:8083`)

**Functionality:** the dealer → admin cylinder supply request workflow (`PENDING → APPROVED/REJECTED`, then dealer-confirmed `DELIVERED`), plus fair-distribution analytics.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/allocations/request` | DEALER | Submit a new allocation request (cylinder type + quantity) |
| GET | `/api/v1/allocations/pending` | ADMIN | List all pending requests |
| GET | `/api/v1/allocations?status=` | ADMIN | List all requests, optionally filtered by status |
| PUT | `/api/v1/allocations/{id}/approve` | ADMIN | Approve a request with an approved quantity |
| PUT | `/api/v1/allocations/{id}/reject` | ADMIN | Reject a request with a reason |
| PUT | `/api/v1/allocations/{id}/confirm` | DEALER | Confirm physical delivery — triggers a Kafka event that credits inventory |
| GET | `/api/v1/allocations/dealer/{dealerId}` | DEALER (self) | View own allocation history |
| GET | `/api/v1/allocations/dealer/{dealerId}/analytics` | DEALER (self) | Fulfilment rate, totals, and comparison vs. platform-wide average |
| GET | `/api/v1/allocations/{id}` | Any authenticated | Get a single allocation |

**Inter-service interactions:**
- **Publishes** `allocation.requested`, `allocation.approved`, `allocation.rejected`, `allocation.confirmed` — consumed by `notification-service` (status updates to the dealer) and `inventory-service` (`allocation.confirmed` triggers a stock credit).

#### 4. Queue Service (`:8084`)

**Functionality:** the citizen virtual pickup queue — join, token issuance, dealer-side queue advancement, and queue analytics.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/queue/join` | CITIZEN | Join a dealer's queue for a specific cylinder type; issues a token number |
| GET | `/api/v1/queue/my` | CITIZEN | List the caller's own queue entries |
| GET | `/api/v1/queue/dealer/{dealerId}` | DEALER (self) | Live queue for the dealer's shop |
| GET | `/api/v1/queue/dealer/{dealerId}/analytics` | DEALER (self) | Waiting count, ready count, completed-today, avg. wait time |
| PUT | `/api/v1/queue/dealer/{dealerId}/call-next` | DEALER (self) | Advance the queue — calls the next waiting citizen |
| PUT | `/api/v1/queue/{id}/ready` | DEALER | Mark a citizen's order ready for pickup |
| PUT | `/api/v1/queue/{id}/complete` | DEALER | Mark a pickup completed — triggers a stock deduction event |
| PUT | `/api/v1/queue/{id}/cancel` | CITIZEN or DEALER | Cancel/leave a queue entry |
| GET | `/api/v1/queue/{id}` | Any authenticated | Get a single queue entry |

**Inter-service interactions:**
- **Publishes** `queue.joined`, `queue.ready`, `queue.completed`, `queue.cancelled` — consumed by `notification-service` (status updates to both citizen and dealer) and `inventory-service` (`queue.completed` deducts stock).

#### 5. Notification Service (`:8085`)

**Functionality:** purely event-driven — has no endpoints that *create* notifications directly; it listens to Kafka and materializes notifications, which users then read/dismiss via REST.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/notifications` | Any authenticated | List all of the caller's notifications |
| GET | `/api/v1/notifications/unread` | Any authenticated | List unread notifications |
| GET | `/api/v1/notifications/count` | Any authenticated | Unread count (for a badge/indicator) |
| PUT | `/api/v1/notifications/{id}/read` | Any authenticated | Mark one notification as read |
| PUT | `/api/v1/notifications/read-all` | Any authenticated | Mark all as read |

**Inter-service interactions:** consumes **9 Kafka topics** published by the other four services:

| Topic | Published by | Notifies |
|---|---|---|
| `user.registered` | user-service | New citizen — welcome message |
| `dealer.registered` | user-service | New dealer — account-created message |
| `allocation.requested` | allocation-service | Dealer — request submitted |
| `allocation.approved` | allocation-service | Dealer — request approved |
| `allocation.rejected` | allocation-service | Dealer — request rejected (with reason) |
| `allocation.confirmed` | allocation-service | Dealer — delivery confirmed |
| `queue.joined` | queue-service | Citizen (token issued) + Dealer (new entry) |
| `queue.ready` | queue-service | Citizen — ready for pickup |
| `queue.completed` | queue-service | Citizen — pickup completed |
| `queue.cancelled` | queue-service | Citizen — entry cancelled |

This is the clearest illustration of the platform's event-driven design: none of these five notification types required any direct coupling between the producing service and `notification-service` — they only share a topic name and a Kafka broker.

---

## User Interface
<img width="1181" height="807" alt="image" src="https://github.com/user-attachments/assets/d835a540-a2fe-486e-988d-2755b391fd8f" />
<img width="1156" height="797" alt="image" src="https://github.com/user-attachments/assets/e16e94a5-844b-477c-bd59-a532e5a7c4bd" />
<img width="1094" height="709" alt="image" src="https://github.com/user-attachments/assets/86f82b20-36cb-4638-9ac4-61a85245e267" />
<img width="1093" height="698" alt="image" src="https://github.com/user-attachments/assets/18da1384-0d3e-4fd8-8757-4b05d1ce1751" />
<img width="1178" height="760" alt="image" src="https://github.com/user-attachments/assets/67261a3f-8cd1-44f9-86fc-52a85706bca9" />
<img width="1113" height="723" alt="image" src="https://github.com/user-attachments/assets/eb43f0ff-524c-462d-9b47-cffce9cc0277" />





### Implementation details

- **Framework:** React 19 + TypeScript, built with **TanStack Start** (file-based routing via `@tanstack/react-router`) and **Vite 7**.
- **UI components:** shadcn/ui (Radix UI primitives) + Tailwind CSS v4 for styling.
- **State/data fetching:** a small typed `api()` fetch wrapper (`src/lib/api.ts`) that attaches the JWT `Authorization` header and normalizes error responses; no heavy client-state library needed given the app's size.
- **Maps:** `maplibre-gl` for the dealer map view on the citizen Discover page.
- **Forms/validation:** `react-hook-form` + `zod`.
- **Charts:** `recharts` for dealer/admin analytics.
- **Deployment target:** containerized with its own `Dockerfile`, served on port `3000`, calling the API Gateway at `:8080`.

### Pages / role dashboards

| Route | Role(s) | What it does |
|---|---|---|
| `/login`, `/register` | Public | Auth entry points |
| `/citizen` | CITIZEN | Discover nearby dealers (map + list, live stock & price, adjustable radius), join/leave queue, profile |
| `/dealer` | DEALER | Overview analytics, live queue management, stock management + history, allocation requests, profile |
| `/admin` | ADMIN | Allocation approvals, user management + dealer registration (with shop coordinates), inventory oversight, cylinder pricing |

Every dashboard talks to the backend exclusively through the API Gateway (`http://localhost:8080`) — the frontend never calls a backend service directly, matching the architecture's single-entry-point design.

### API testing tools

Alongside the running app, the backend was exercised directly with **Postman** (and `curl` for scripted checks) against the API Gateway:
- [`USER_SERVICE_POSTMAN_GUIDE.md`](USER_SERVICE_POSTMAN_GUIDE.md) — registration, login, profile, dealer management flows
- [`INVENTORY_SERVICE_POSTMAN_GUIDE.md`](INVENTORY_SERVICE_POSTMAN_GUIDE.md) — stock CRUD, cylinder types, nearby search
- [`FULL_FLOW_TESTING_GUIDE.md`](FULL_FLOW_TESTING_GUIDE.md) — end-to-end scenario covering registration → allocation → queue → pickup across every service

These were used both during development (verifying an endpoint in isolation before wiring the UI to it) and for authorization testing — e.g. confirming a `CITIZEN` token is rejected with `403` on an `ADMIN`-only endpoint, and that role checks (`@PreAuthorize`) are enforced by the backend itself, not just hidden in the UI.

---

## Deployment

### Local machine — Docker Compose (recommended)

The entire system — 5 PostgreSQL databases, Kafka + Zookeeper, Eureka, all 6 backend services, the API Gateway, and the frontend — is orchestrated by a single `docker-compose.yml`, driven by the included `Makefile`.

**Prerequisites:** Docker Desktop, Java 17+, Maven (for the initial JAR build).

```bash
git clone https://github.com/gnklakshan/smart-gas-distribution-platform.git
cd smart-gas-distribution-platform
git clone https://github.com/sathmipeiris/gas-flow-hub.git ../gas-flow-hub   # frontend, sibling directory

make up          # builds all service JARs, then docker compose up -d --build
make seed        # loads seed data into all 5 databases
make health       # verifies every service's /actuator/health
```

| URL | Service |
|---|---|
| http://localhost:3000 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:8081–8085 | Individual backend services (direct, for debugging) |

Other useful commands: `make ps` (container status), `make logs` (tail all logs), `make stop` (stop, keep data), `make clean` (stop + wipe volumes).

### Local machine — without Docker (development mode)

For faster iteration on a single service, start only its dependencies via Docker and run the service itself locally:

```bash
docker compose up -d postgres-user postgres-inventory eureka-server kafka zookeeper
cd user-service && ./mvnw spring-boot:run     # repeat per service, in dependency order:
                                                 # Eureka → User → Inventory → Allocation/Queue/Notification → Gateway
```

### Cloud deployment

Because every service is already containerized and stateless (aside from its own database), the same images deploy to any container platform without modification:

1. **Push images** to a registry (Docker Hub / AWS ECR / GCP Artifact Registry) — `docker compose build` produces one image per service, tagged e.g. `docker tag smart-gas-distribution-platform-user-service:latest <registry>/user-service:latest`.
2. **Managed databases**: replace the 5 containerized PostgreSQL instances with managed instances (e.g. AWS RDS / Cloud SQL), pointing each service's `SPRING_DATASOURCE_URL` env var at the managed endpoint instead of the Compose network alias.
3. **Managed Kafka**: replace the `confluentinc/cp-kafka` container with a managed broker (e.g. Confluent Cloud / AWS MSK), updating `SPRING_KAFKA_BOOTSTRAP_SERVERS`.
4. **Orchestration**: deploy the same containers to a Kubernetes cluster (or ECS/Cloud Run) — each service already reads all config from environment variables (see `docker-compose.yml`), so no code changes are needed, only a Deployment/Service manifest per container plus a ConfigMap/Secret for the env vars currently hardcoded in Compose.
5. **Discovery in the cloud**: Eureka continues to work unmodified across nodes as long as all instances can reach the Eureka server's address/port — update `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` to the cloud-hosted Eureka URL.
6. **Gateway as the public entry point**: only the API Gateway (and the frontend) need a public-facing load balancer / ingress; every backend service stays on a private network, matching the current architecture where clients never address a backend service directly.

---

## Source Code

- **Backend:** https://github.com/gnklakshan/smart-gas-distribution-platform
- **Frontend:** https://github.com/sathmipeiris/gas-flow-hub

### Development challenges

**Spring Cloud Gateway configuration namespace.** The MVC (servlet) variant of Spring Cloud Gateway (`spring-cloud-starter-gateway-server-webmvc`) uses a different YAML namespace (`spring.cloud.gateway.server.webmvc.routes`) than both the reactive gateway (`spring.cloud.gateway.routes`) and the older MVC starter (`spring.cloud.gateway.mvc.routes`). Using the wrong namespace doesn't error — it silently registers zero routes, and every request 404s. This cost significant debugging time until the correct prefix was identified.

**Route predicate ordering.** `/api/v1/inventory/nearby` needed to be served by `user-service` (a composite endpoint enriching dealer geolocation with live stock), while every other `/api/v1/inventory/**` path belongs to `inventory-service`. Spring Cloud Gateway's automatic discovery locator interleaved these unpredictably; the fix was disabling `discovery.locator.enabled` and declaring routes manually in priority order, with the specific exact-path route defined before the catch-all wildcard route.

**Eureka registry propagation delay.** After rebuilding/restarting a service (e.g. via `docker compose up --build <service>`, which also recreates its `depends_on` chain), the API Gateway's local Eureka client cache doesn't refresh instantly — there's a window of up to ~30 seconds (the default registry-fetch interval) where the gateway routes to a stale/dead instance and returns `500`s. Understanding this was key to not misdiagnosing genuine bugs during iterative development — several apparent "regressions" during testing were actually this propagation delay resolving itself seconds later.

**Inconsistent `dealerId` conventions across services.** `inventory-service` and `queue-service` treat `dealerId` as the **dealer's user ID** (the same ID a dealer authenticates with), not the separate primary key of the `dealers` table in `user-service`. This convention is consistent throughout (seed data, `queue-service`'s `#dealerId == authentication.principal` authorization checks, the admin UI's inventory-creation form) but isn't obvious from the `dealers` table's own schema — manually creating inventory records using the wrong ID silently produced zero-stock results in nearby search, since the Feign-based stock lookup simply found no matching rows.

**Over-restrictive cross-service authorization.** `GET /api/v1/users/{id}` was originally locked to `ADMIN` or the requester themselves. This is correct for citizen/admin records, but broke the citizen dashboard's ability to resolve a dealer's business name for queue entries (a citizen fetching a *different* user's — the dealer's — profile). The fix distinguished "dealer profiles are public storefront info" from "citizen/admin profiles are private," using a custom `@PreAuthorize` SpEL expression backed by a small `UserSecurity` bean, rather than opening the endpoint entirely. A related gap — a missing global exception handler for `AccessDeniedException` — meant authorization failures were surfacing as generic `500`s instead of `403`s, masking the real cause during debugging.

**Docker image build time for first-run.** The very first `docker compose up --build` pulls every base image (`eclipse-temurin:17-jdk-jammy` per service, `node:22-alpine` for the frontend) and runs `apt-get install curl` in each Dockerfile from scratch — on a slow connection this took several minutes per layer. Subsequent builds are dramatically faster since Docker's layer cache reuses the base image and package-install layers; only the `COPY`+build steps for actually-changed source re-run.

---

## References

- [Spring Cloud Gateway Server WebMVC Reference](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html)
- [Spring Cloud Netflix (Eureka) Reference](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
- [Spring Cloud OpenFeign Reference](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [TanStack Start Documentation](https://tanstack.com/start/latest)
- [TanStack Router Documentation](https://tanstack.com/router/latest)
- [shadcn/ui](https://ui.shadcn.com/)
- [MapLibre GL JS](https://maplibre.org/maplibre-gl-js/docs/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Haversine formula (great-circle distance)](https://en.wikipedia.org/wiki/Haversine_formula)
- Internal developer guide: [`guide.md`](guide.md) — layered service structure & conventions used across this codebase
