# ============================================================================
# Smart Gas Distribution Platform — Makefile
# ============================================================================
# Mac / Linux : run directly      →  make up
# Windows CMD : run directly      →  make up
# Windows Git Bash / WSL          →  make up
# ============================================================================

# ── OS detection ─────────────────────────────────────────────────────────────
# On Windows, the OS environment variable is always "Windows_NT"
ifeq ($(OS),Windows_NT)
    SHELL       := cmd.exe
    .SHELLFLAGS := /c
    SLEEP_15    := timeout /t 15 /nobreak > nul 2>&1
    DEV_NULL    := nul
else
    SLEEP_15    := sleep 15
    DEV_NULL    := /dev/null
endif

# ── Service list ─────────────────────────────────────────────────────────────
SERVICES = discovery-server user-service inventory-service allocation-service \
           queue-service notification-service api-gateway

INFRA = zookeeper kafka postgres-user postgres-inventory postgres-allocation \
        postgres-queue postgres-notification

# ── Build ────────────────────────────────────────────────────────────────────
# Uses make pattern targets instead of a shell loop, so it works on every OS.
# "cd X && mvn ..." runs as a single command in both cmd.exe and /bin/sh.

BUILD_TARGETS := $(addprefix build-svc-,$(SERVICES))

.PHONY: build
build: $(BUILD_TARGETS)
	@echo All services built successfully!

.PHONY: $(BUILD_TARGETS)
$(BUILD_TARGETS):
	cd $(patsubst build-svc-%,%,$@) && mvn clean package -DskipTests -q

# Individual service builds
.PHONY: build-user
build-user:
	cd user-service && mvn clean package -DskipTests

.PHONY: build-inventory
build-inventory:
	cd inventory-service && mvn clean package -DskipTests

.PHONY: build-allocation
build-allocation:
	cd allocation-service && mvn clean package -DskipTests

.PHONY: build-queue
build-queue:
	cd queue-service && mvn clean package -DskipTests

.PHONY: build-notification
build-notification:
	cd notification-service && mvn clean package -DskipTests

.PHONY: build-gateway
build-gateway:
	cd api-gateway && mvn clean package -DskipTests

# ── Infrastructure ───────────────────────────────────────────────────────────

.PHONY: infra
infra:
	docker compose up -d $(INFRA)
	$(SLEEP_15)
	docker compose ps

.PHONY: up
up: build
	docker compose up -d --build
	@echo All services starting. Wait ~60s then check http://localhost:8761

.PHONY: up-no-build
up-no-build:
	docker compose up -d --build

.PHONY: restart
restart: stop up

.PHONY: stop
stop:
	docker compose down

.PHONY: clean
clean:
	docker compose down -v

.PHONY: ps
ps:
	docker compose ps

.PHONY: logs
logs:
	docker compose logs -f --tail=50

# ── Health Checks ────────────────────────────────────────────────────────────

.PHONY: health
health:
	@echo Eureka:
	@curl -sf http://localhost:8761/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo User Service:
	@curl -sf http://localhost:8081/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo Inventory Service:
	@curl -sf http://localhost:8082/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo Allocation Service:
	@curl -sf http://localhost:8083/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo Queue Service:
	@curl -sf http://localhost:8084/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo Notification Service:
	@curl -sf http://localhost:8085/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE
	@echo API Gateway:
	@curl -sf http://localhost:8080/actuator/health 2>$(DEV_NULL) || echo UNREACHABLE

# ── Database Seeding ─────────────────────────────────────────────────────────

.PHONY: seed
seed:
	docker exec -i postgres-user psql -U postgres -d userdb < db/seed/userdb.sql
	docker exec -i postgres-inventory psql -U postgres -d inventorydb < db/seed/inventorydb.sql
	docker exec -i postgres-allocation psql -U postgres -d allocationdb < db/seed/allocationdb.sql
	docker exec -i postgres-queue psql -U postgres -d queuedb < db/seed/queuedb.sql
	docker exec -i postgres-notification psql -U postgres -d notificationdb < db/seed/notificationdb.sql
	@echo All databases seeded!

# ── Kafka ────────────────────────────────────────────────────────────────────

.PHONY: kafka-topics
kafka-topics:
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

.PHONY: kafka-listen
kafka-listen:
	docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic $(TOPIC) --from-beginning

# ── Help ─────────────────────────────────────────────────────────────────────

.PHONY: help
help:
	@echo.
	@echo Available commands:
	@echo   make build          Build all service JARs
	@echo   make up             Build JARs + start everything in Docker
	@echo   make up-no-build    Start in Docker without rebuilding JARs
	@echo   make infra          Start databases + Kafka only
	@echo   make seed           Seed all databases with test data
	@echo   make stop           Stop all containers
	@echo   make clean          Stop + delete all data volumes
	@echo   make health         Check health of all services
	@echo   make ps             Show running containers
	@echo   make logs           Tail Docker logs
	@echo   make restart        Full stop + rebuild + start
	@echo.

.DEFAULT_GOAL := help
