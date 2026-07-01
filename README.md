# Smart Gas (LPG) Distribution Platform

## Introduction

The **Smart Gas Distribution Platform** is an event-driven microservices system that solves a real-world problem in Sri Lanka: LPG (cooking gas) shortages that force citizens to queue for hours outside distributors without knowing whether stock is even available.

The platform lets citizens check real-time cylinder stock at nearby dealers, join a **virtual queue** instead of a physical one, and get notified when their turn arrives. Dealers manage their inventory, request cylinder allocations from an admin-controlled central supply, and process payments online. Admins oversee dealer registration, approve/reject allocation requests, and manage cylinder pricing.

**Core features**
- Role-based accounts: **CITIZEN**, **DEALER**, **ADMIN** (JWT-secured)
- Real-time dealer stock lookup by GPS location ("nearby dealers")
- Virtual queue / token system so citizens don't have to wait physically
- Dealer-to-admin cylinder allocation request/approval workflow
- Stripe-based online payment for allocations
- Event-driven notifications (welcome, allocation status, queue status, payment confirmation)
- Dealer analytics (stock history, allocation fairness, queue throughput)

**Goal**: eliminate wasted time and uncertainty around LPG availability by digitizing the entire distribution chain — from central allocation to dealer stock to citizen pickup — as a set of independently deployable microservices.

---

## Architecture

### Architectural Diagram

```
                                   ┌──────────────────────┐
                                   │   Frontend (React /   │
                                   │   TanStack Start)      │
                                   │   Port 3000            │
                                   └───────────┬────────────┘
                                               │ /api/**
                                   ┌───────────▼────────────┐
                                   │      API Gateway        │
                                   │  Spring Cloud Gateway   │
                                   │      Port 8080           │
                                   └───────────┬────────────┘
                                               │ lb:// (via Eureka)
              ┌───────────────┬────────────────┼────────────────┬───────────────┐
              │               │                │                │               │
     ┌────────▼──────┐ ┌──────▼───────┐ ┌──────▼───────┐ ┌──────▼───────┐ ┌─────▼────────┐
     │ User Service   │ │ Inventory     │ │ Allocation    │ │ Queue        │ │ Notification  │
     │ Port 8081      │ │ Service       │ │ Service       │ │ Service      │ │ Service       │
     │                │ │ Port 8082     │ │ Port 8083     │ │ Port 8084    │ │ Port 8085     │
     └────────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬────────┘
              │                 │                 │                │                 │
       PostgreSQL         PostgreSQL         PostgreSQL         MongoDB          PostgreSQL
        (userdb)         (inventorydb)      (allocationdb)     (queuedb)      (notificationdb)

              ▲                 ▲                 ▲                ▲                 ▲
              └─────────────────┴────── Eureka Discovery Server (8761) ───────────────┘
                                               │
                                     ┌─────────▼─────────┐
                                     │   Kafka + Zookeeper │
                                     │  (event backbone)   │
                                     └─────────────────────┘
```

Every service registers with **Eureka** and is reachable through the **API Gateway** only — the frontend never talks to a service directly. Cross-service data needs (e.g. "does this dealer exist and is it a DEALER role?") are resolved synchronously via **OpenFeign** clients using Eureka's load-balanced (`lb://`) URIs. Asynchronous, fire-and-forget side effects (notifications) are decoupled through **Kafka** topics so the core services never block on notification delivery.

### Design Decisions

- **Domain-driven service boundaries.** Each service owns exactly one bounded context and its own database (database-per-service): identity/auth (`user-service`), stock (`inventory-service`), the dealer↔admin supply chain (`allocation-service`, includes payments), the citizen-facing pickup queue (`queue-service`), and cross-cutting alerts (`notification-service`). This keeps each service independently deployable and lets teams change, say, the queueing algorithm without touching billing.
- **Synchronous calls for consistency-critical reads, async events for side effects.** When `allocation-service` needs to know a dealer's current stock before approving a request, it calls `inventory-service` synchronously via Feign — the caller needs an immediate, consistent answer. In contrast, when an allocation is approved, a `notification-service` alert doesn't need to happen before the response returns to the user, so it travels over Kafka.
- **Database per service.** Each service has its own PostgreSQL schema (queue-service uses MongoDB since queue entries are document-like and high-churn) so services can evolve their schemas independently and are never coupled via shared tables.
- **Central discovery instead of hardcoded URLs.** All inter-service and gateway routing resolves service instances dynamically through Eureka, so services can scale horizontally or move hosts without reconfiguration.
- **A single public entry point.** The API Gateway is the only component exposed to the frontend, centralizing CORS, routing, and (in principle) cross-cutting concerns like rate limiting — internal service ports are not part of the public contract.
- **Payments live inside `allocation-service`.** Stripe payment is tightly coupled to the allocation lifecycle (a payment only exists for an approved allocation), so it was kept as a module of `allocation-service` rather than a separate microservice, avoiding a service whose entire API is "operate on someone else's aggregate."

---

## Microservices

### Implementation Methods

Built on **Java 17 + Spring Boot 3** using the **Netflix OSS stack** (via Spring Cloud):

| Concern | Technology |
|---|---|
| Service registry / discovery | **Netflix Eureka** (`spring-cloud-starter-netflix-eureka-server` / `-client`) |
| Edge routing | **Spring Cloud Gateway** (MVC/servlet variant, `spring-cloud-starter-gateway-server-webmvc`) |
| Client-side load balancing | Spring Cloud LoadBalancer (used implicitly by `lb://` Feign/Gateway URIs) |
| Inter-service REST calls | **OpenFeign** (`spring-cloud-starter-openfeign`) |
| Async messaging | Apache Kafka + Zookeeper |
| Persistence | Spring Data JPA/Hibernate (PostgreSQL); Spring Data MongoDB (queue-service) |
| Security | Spring Security + JWT (stateless, role-based `@PreAuthorize`) |
| Payments | Stripe Java SDK |
| Containerization | Docker + Docker Compose |

### Core Services

#### 1. User Service — port `8081`
Owns identity: registration, authentication (JWT issuance), profile management, role management, and dealer geolocation search.

**REST API** (`/api/v1/users`, `/api/v1/inventory`):
| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/users/register` | Citizen self-registration | Public |
| POST | `/api/v1/users/register/dealer` | Register a new dealer (business details) | ADMIN |
| POST | `/api/v1/users/login` | Authenticate, returns JWT | Public |
| GET | `/api/v1/users/me` | Get own profile | Authenticated |
| GET | `/api/v1/users/{id}` | Get user by ID | Own or ADMIN |
| PUT | `/api/v1/users/{id}` | Update profile | Own or ADMIN |
| GET | `/api/v1/users/role/{role}` | List users by role | ADMIN |
| GET | `/api/v1/users/dealers/nearby?lat&lng&radius` | Find dealers near a GPS point with stock | Authenticated |
| DELETE | `/api/v1/users/{id}` | Delete user | ADMIN |
| GET | `/api/v1/inventory/nearby?lat&lng&radius` | Frontend-shaped view combining dealer + stock data | Authenticated |

**Inter-service interaction:** calls `inventory-service` (via Feign `InventoryClient`) to enrich "nearby dealers" results with live stock; publishes `user.registered` and `dealer.registered` Kafka events consumed by `notification-service`.

#### 2. Inventory Service — port `8082`
Owns dealer stock levels, cylinder type catalogue/pricing, and stock-change history.

**REST API** (`/api/v1/inventory`, `/api/v1/cylinder-types`):
| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/inventory` | Create a dealer's inventory record | ADMIN |
| GET | `/api/v1/inventory/{id}` | Get inventory record by ID | Authenticated |
| GET | `/api/v1/inventory/dealer/{dealerId}` | Get a dealer's stock (per cylinder type) | Authenticated |
| GET | `/api/v1/inventory/available` | List all dealers with available stock | Authenticated |
| PUT | `/api/v1/inventory/{id}/stock` | Update stock level | DEALER |
| GET | `/api/v1/inventory/dealer/{dealerId}/stock-history?days=30` | Stock change history | DEALER (own) |
| POST | `/api/v1/cylinder-types` | Create a cylinder type | ADMIN |
| GET | `/api/v1/cylinder-types` | List cylinder types | Authenticated |
| GET | `/api/v1/cylinder-types/{id}` | Get cylinder type by ID | Authenticated |
| PATCH | `/api/v1/cylinder-types/{id}/price` | Update cylinder price | ADMIN |

**Inter-service interaction:** calls `user-service` (via Feign `UserClient`) to validate dealer identity/role before accepting stock updates; is called synchronously by `allocation-service` and `user-service`. Also consumes `allocation.confirmed` (add stock on confirmed delivery) and `queue.completed` (decrement stock on pickup) Kafka events — the same two topics `notification-service` consumes for user alerts, a standard Kafka fan-out to independent consumer groups.

#### 3. Allocation Service — port `8083`
Owns the dealer → admin cylinder supply workflow and payment processing for approved allocations.

**REST API** (`/api/v1/allocations`, `/api/v1/payments`):
| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/allocations/request` | Dealer requests a cylinder allocation | DEALER |
| GET | `/api/v1/allocations/pending` | List pending allocation requests | ADMIN |
| GET | `/api/v1/allocations?status=` | List all allocations, optional status filter | ADMIN |
| PUT | `/api/v1/allocations/{id}/approve` | Approve a request | ADMIN |
| PUT | `/api/v1/allocations/{id}/reject` | Reject a request | ADMIN |
| PUT | `/api/v1/allocations/{id}/confirm` | Dealer confirms physical delivery | DEALER |
| GET | `/api/v1/allocations/dealer/{dealerId}` | Dealer's own allocation history | DEALER (own) |
| GET | `/api/v1/allocations/dealer/{dealerId}/analytics` | Allocation analytics / fair-share comparison | DEALER (own) |
| GET | `/api/v1/allocations/{id}` | Get single allocation | Authenticated |
| POST | `/api/v1/payments/allocations/{allocationId}` | Create a Stripe PaymentIntent | DEALER |
| GET | `/api/v1/payments/allocations/{allocationId}` | Get payment status | Authenticated |
| POST | `/api/v1/payments/webhook` | Stripe webhook receiver (signature-verified) | Public (Stripe) |

**Inter-service interaction:** calls `inventory-service` (via Feign `InventoryClient`, `GET /api/v1/cylinder-types/{id}`) to price a requested cylinder type, wrapped in a **Resilience4j circuit breaker** so a slow/down `inventory-service` doesn't cascade-fail allocation requests; publishes `allocation.requested`, `allocation.approved`, `allocation.rejected`, `allocation.confirmed`, and `payment.confirmed` Kafka events. `allocation.confirmed` is consumed by both `inventory-service` (adds delivered stock) and `notification-service` (alerts the dealer).

#### 4. Queue Service — port `8084` (MongoDB)
Owns the virtual queue/token system citizens use instead of physically waiting at a dealer.

**REST API** (`/api/v1/queue`):
| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/v1/queue/join` | Citizen joins a dealer's queue | CITIZEN |
| GET | `/api/v1/queue/my` | Citizen's own queue entries | CITIZEN |
| GET | `/api/v1/queue/dealer/{dealerId}` | Dealer's current queue | DEALER (own) |
| GET | `/api/v1/queue/dealer/{dealerId}/analytics` | Queue throughput analytics | DEALER (own) |
| PUT | `/api/v1/queue/dealer/{dealerId}/call-next` | Call the next citizen in line | DEALER (own) |
| PUT | `/api/v1/queue/{id}/ready` | Mark a citizen as ready for pickup | DEALER |
| PUT | `/api/v1/queue/{id}/complete` | Mark pickup as completed | DEALER |
| PUT | `/api/v1/queue/{id}/cancel` | Cancel a queue entry | CITIZEN or DEALER |
| GET | `/api/v1/queue/{id}` | Get a single queue entry | Authenticated |

**Inter-service interaction:** publishes `queue.joined`, `queue.ready`, `queue.completed`, `queue.cancelled` Kafka events consumed by `notification-service` to alert both citizen and dealer.

#### 5. Notification Service — port `8085`
Owns user-facing alerts, generated purely by consuming Kafka events from the other four services (it exposes no producer-facing API of its own).

**REST API** (`/api/v1/notifications`):
| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/v1/notifications` | List all of the caller's notifications | Authenticated |
| GET | `/api/v1/notifications/unread` | List unread notifications | Authenticated |
| GET | `/api/v1/notifications/count` | Unread notification count | Authenticated |
| PUT | `/api/v1/notifications/{id}/read` | Mark one notification as read | Authenticated |
| PUT | `/api/v1/notifications/read-all` | Mark all as read | Authenticated |

**Inter-service interaction:** pure Kafka consumer — listens on `user.registered`, `dealer.registered`, `allocation.requested`, `allocation.approved`, `allocation.rejected`, `allocation.confirmed`, `payment.confirmed`, `queue.joined`, `queue.ready`, `queue.completed`, `queue.cancelled`, and writes a `Notification` row per event. This decouples every producing service from notification-delivery latency/availability.

### Discovery Server

`discovery-server` (port `8761`) is a **Netflix Eureka Server** (`register-with-eureka: false`, `fetch-registry: false` — it doesn't register itself). Every other service is a Eureka **client**: on startup each registers under its `spring.application.name` and sends periodic heartbeats; Eureka evicts an instance from the registry if heartbeats stop, and the API Gateway / Feign clients refresh their local view of the registry on an interval, so traffic automatically stops going to instances that go down. The Eureka dashboard at `http://localhost:8761` shows every registered instance and its status in real time.

### API Gateway

`api-gateway` (port `8080`) uses **Spring Cloud Gateway MVC** (`spring-cloud-starter-gateway-server-webmvc` — the servlet/Tomcat-based variant, not the reactive Netty one). Key configuration:
- **Manual routes** (auto-discovery locator is explicitly disabled) map each `/api/v1/**` path prefix to a service via `uri: lb://<service-name>`, letting Spring Cloud LoadBalancer pick a live instance from Eureka.
- A dedicated `inventory-nearby` route intentionally sits *above* the generic `inventory-service` catch-all so that `/api/v1/inventory/nearby` is routed to `user-service` (which composes the nearby-dealer view) instead of being swallowed by the broader `/api/v1/inventory/**` route to `inventory-service`.
- **Global CORS** is configured for the frontend origin (`http://localhost:3000`), allowing GET/POST/PUT/DELETE/OPTIONS with credentials.
- Actuator's `gateway` endpoint is exposed so `GET /actuator/gateway/routes` can be used to verify the live routing table.

---

## User Interface

### Implementation Details

The frontend is a **React + TypeScript** single-page app built on **TanStack Start** (TanStack Router + Vite), styled with **Tailwind CSS** and **Radix UI** primitives (shadcn/ui-style components), with **React Hook Form** + resolvers for form validation. It runs on port `3000` and talks exclusively to the API Gateway (`/api/...`, proxied to `api-gateway:8080` inside Docker Compose).

Key routes:
- `/` — landing page
- `/login`, `/register` — auth flows
- `/citizen` — dealer search by location, join queue, view own queue tokens and notifications
- `/dealer` — stock management, allocation requests, queue management (call next / ready / complete), analytics
- `/admin` — dealer registration, allocation approval/rejection, cylinder type & pricing management

### API Testing Tools

The backend was tested independently of the UI using **Postman** collections against both direct service ports and the gateway (`localhost:8080`), covering the full auth → register → login → JWT-protected flow for each role. See `backend/USER_SERVICE_POSTMAN_GUIDE.md` for the documented request/response examples and role-based test scenarios (citizen registration, dealer registration by admin, login, ownership-protected `GET/PUT /{id}`). The same approach — send request through the gateway with a `Bearer <JWT>` header, verify status code + Eureka-routed response — was used to validate inventory, allocation, payment, queue, and notification endpoints during development.

---

## Deployment

### Local (Docker Compose — recommended)

```bash
git clone <repo-url>
cd smart-gas-distribution-platform
docker compose -f backend/docker-compose.yml up --build
```

This brings up Zookeeper, Kafka, one PostgreSQL instance per relational service, MongoDB, `discovery-server`, `api-gateway`, all five business microservices, and the frontend, wired together on a shared `lpg-network` Docker network.

| URL | Purpose |
|---|---|
| http://localhost:3000 | Frontend |
| http://localhost:8761 | Eureka dashboard |
| http://localhost:8080 | API Gateway |
| http://localhost:8080/actuator/health | Gateway health |
| http://localhost:8080/actuator/gateway/routes | Live route table |

### Local (native, for fast iteration)

Start infra only, then run each service with Maven in its own terminal, in dependency order (Eureka → business services → Gateway):

```bash
docker compose -f backend/docker-compose.yml up postgres-user postgres-inventory postgres-allocation zookeeper kafka mongo
cd backend/discovery-server && ./mvnw spring-boot:run     # 8761
cd backend/user-service && ./mvnw spring-boot:run          # 8081
cd backend/inventory-service && ./mvnw spring-boot:run     # 8082
cd backend/allocation-service && ./mvnw spring-boot:run    # 8083
cd backend/queue-service && ./mvnw spring-boot:run         # 8084
cd backend/notification-service && ./mvnw spring-boot:run  # 8085
cd backend/api-gateway && ./mvnw spring-boot:run            # 8080
cd frontend && npm install && npm run dev                   # 3000
```

### Cloud / production-like deployment

The same Docker Compose file is the deployment artifact: build each service's image (`docker compose build`), push to a container registry, and run on any Docker host or orchestrator (e.g. a single cloud VM with `docker compose up -d`, or translate the compose file to Kubernetes manifests/Helm for a managed cluster). In a real production rollout, each stateful component (PostgreSQL instances, MongoDB, Kafka) would be replaced with managed equivalents (e.g. RDS, Atlas, MSK), `discovery-server` and `api-gateway` would run with multiple replicas behind a load balancer, and secrets (JWT signing key, Stripe keys, DB credentials) would move from `application.yaml`/Compose env vars into a secret manager rather than being baked into images.

---

## Source Code

**GitHub repository:** _add your repository link here before submitting_

### Development Challenges

- **Spring Cloud Gateway MVC route configuration.** The correct YAML namespace for this project's servlet-based gateway is `spring.cloud.gateway.server.webmvc.routes` — using the more commonly documented reactive (`spring.cloud.gateway.routes`) or legacy MVC (`spring.cloud.gateway.mvc.routes`) namespaces silently registers zero routes (no error, just 404s on every request). This cost significant debugging time until the routes were confirmed live via `GET /actuator/gateway/routes`.
- **Route ordering conflicts.** A generic `/api/v1/inventory/**` route to `inventory-service` was swallowing the more specific `/api/v1/inventory/nearby` request meant for `user-service`. Solved by disabling the discovery locator (which reorders routes non-deterministically) and explicitly ordering the specific route above the catch-all in the manual route list.
- **Keeping stock consistent across services without shared tables.** Inventory is owned by `inventory-service`, but both `user-service` (nearby search) and `allocation-service` (delivery confirmation) need to read/update it. This was resolved with synchronous Feign calls rather than duplicating stock data, at the cost of those two services now having a runtime dependency on `inventory-service`'s availability.
- **Decoupling notifications from core business transactions.** Early on, allocation/queue services considered calling `notification-service` synchronously, which would have made every business transaction fail if notifications were down. Moving to Kafka-based eventing (`allocation.*`, `queue.*`, `payment.*`, `user.*` topics) let `notification-service` fail or lag independently without affecting the rest of the system.
- **Role-based, ownership-aware authorization.** Several endpoints needed "ADMIN, or the resource owner" semantics (e.g. `GET /api/v1/users/{id}`). This was implemented with Spring Security SpEL in `@PreAuthorize` (`hasRole('ADMIN') or #id == authentication.principal`), with the JWT subject set as the security principal so ownership checks don't require an extra DB lookup.

### References

- [Spring Cloud Netflix (Eureka) Reference Docs](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
- [Spring Cloud Gateway Reference Docs](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud OpenFeign Reference Docs](https://docs.spring.io/spring-cloud-openfeign/reference/html/)
- [Spring Security Reference Docs](https://docs.spring.io/spring-security/reference/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Stripe API Reference](https://stripe.com/docs/api)
- [TanStack Start / TanStack Router Docs](https://tanstack.com/start)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
