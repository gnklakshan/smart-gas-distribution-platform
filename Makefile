# ============================================================================
# Smart Gas Distribution Platform — Makefile
# ============================================================================
# Usage:
#   make infra          → Start databases + Kafka (Docker)
#   make build          → Build all service JARs
#   make up             → Build JARs + start everything in Docker
#   make dev            → Start infra + run all services locally
#   make stop           → Stop all Docker containers
#   make clean          → Stop containers + delete volumes (fresh DB)
#   make logs           → Tail logs for all Docker services
#   make health         → Check health of all services
#   make ps             → Show running containers
# ============================================================================

SERVICES = discovery-server user-service inventory-service allocation-service queue-service notification-service api-gateway

INFRA = zookeeper kafka postgres-user postgres-inventory postgres-allocation postgres-queue postgres-notification

# ── Infrastructure ───────────────────────────────────────────────────────────

.PHONY: infra
infra: ## Start databases + Kafka only
	docker-compose up -d $(INFRA)
	@echo.
	@echo ✅ Infrastructure started. Waiting for health checks...
	@timeout /t 15 /nobreak > nul 2>&1 || sleep 15
	docker-compose ps

.PHONY: ps
ps: ## Show running containers
	docker-compose ps

.PHONY: stop
stop: ## Stop all containers
	docker-compose down
	@echo ✅ All containers stopped.

.PHONY: clean
clean: ## Stop containers + delete all data volumes (fresh start)
	docker-compose down -v
	@echo ✅ All containers stopped and volumes deleted.

.PHONY: logs
logs: ## Tail logs for all containers
	docker-compose logs -f --tail=50

# ── Build ────────────────────────────────────────────────────────────────────

.PHONY: build
build: ## Build all service JARs (mvn package)
	@echo ============================================
	@echo   Building all services...
	@echo ============================================
	@for %%s in ($(SERVICES)) do ( \
		echo. & echo 🔨 Building %%s... & \
		cd %%s && mvn clean package -DskipTests -q && cd .. \
	)
	@echo.
	@echo ✅ All services built successfully!

.PHONY: build-user
build-user: ## Build user-service only
	cd user-service && mvn clean package -DskipTests

.PHONY: build-inventory
build-inventory: ## Build inventory-service only
	cd inventory-service && mvn clean package -DskipTests

.PHONY: build-allocation
build-allocation: ## Build allocation-service only
	cd allocation-service && mvn clean package -DskipTests

.PHONY: build-queue
build-queue: ## Build queue-service only
	cd queue-service && mvn clean package -DskipTests

.PHONY: build-notification
build-notification: ## Build notification-service only
	cd notification-service && mvn clean package -DskipTests

.PHONY: build-gateway
build-gateway: ## Build api-gateway only
	cd api-gateway && mvn clean package -DskipTests

# ── Docker (Full Stack) ─────────────────────────────────────────────────────

.PHONY: up
up: build ## Build all JARs + start everything in Docker
	docker-compose up -d --build
	@echo.
	@echo ✅ All services starting in Docker...
	@echo    Wait ~60s for services to register with Eureka.
	@echo    Check: http://localhost:8761

.PHONY: up-no-build
up-no-build: ## Start everything in Docker (skip JAR build)
	docker-compose up -d --build
	@echo.
	@echo ✅ All services starting in Docker...

.PHONY: restart
restart: stop up ## Rebuild and restart everything

# ── Local Development ────────────────────────────────────────────────────────

.PHONY: dev
dev: infra ## Start infra + print instructions to run services locally
	@echo.
	@echo ============================================
	@echo   Infrastructure is ready!
	@echo ============================================
	@echo.
	@echo Now open separate terminals and run:
	@echo.
	@echo   Terminal 1:  cd discovery-server   ^&^& mvn spring-boot:run
	@echo   Terminal 2:  cd user-service        ^&^& mvn spring-boot:run
	@echo   Terminal 3:  cd inventory-service   ^&^& mvn spring-boot:run
	@echo   Terminal 4:  cd allocation-service  ^&^& mvn spring-boot:run
	@echo   Terminal 5:  cd queue-service       ^&^& mvn spring-boot:run
	@echo   Terminal 6:  cd notification-service ^&^& mvn spring-boot:run
	@echo   Terminal 7:  cd api-gateway         ^&^& mvn spring-boot:run
	@echo.
	@echo Or use: make run-all  (starts all in background)
	@echo.

.PHONY: run-all
run-all: infra ## Start infra + all services locally (background processes)
	@echo ============================================
	@echo   Starting all services locally...
	@echo ============================================
	@echo.
	@echo Starting Discovery Server...
	start "Eureka" cmd /c "cd discovery-server && mvn spring-boot:run"
	@timeout /t 30 /nobreak > nul 2>&1 || sleep 30
	@echo Starting User Service...
	start "User-Service" cmd /c "cd user-service && mvn spring-boot:run"
	@echo Starting Inventory Service...
	start "Inventory-Service" cmd /c "cd inventory-service && mvn spring-boot:run"
	@echo Starting Allocation Service...
	start "Allocation-Service" cmd /c "cd allocation-service && mvn spring-boot:run"
	@echo Starting Queue Service...
	start "Queue-Service" cmd /c "cd queue-service && mvn spring-boot:run"
	@echo Starting Notification Service...
	start "Notification-Service" cmd /c "cd notification-service && mvn spring-boot:run"
	@timeout /t 10 /nobreak > nul 2>&1 || sleep 10
	@echo Starting API Gateway...
	start "API-Gateway" cmd /c "cd api-gateway && mvn spring-boot:run"
	@echo.
	@echo ✅ All services launching in separate windows!
	@echo    Wait ~60s then check: http://localhost:8761
	@echo    API Gateway: http://localhost:8080

# ── Health Checks ────────────────────────────────────────────────────────────

.PHONY: health
health: ## Check health of all services
	@echo ============================================
	@echo   Health Check — All Services
	@echo ============================================
	@echo.
	@echo Eureka:         & curl -s http://localhost:8761/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo User Service:   & curl -s http://localhost:8081/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo Inventory:      & curl -s http://localhost:8082/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo Allocation:     & curl -s http://localhost:8083/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo Queue:          & curl -s http://localhost:8084/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo Notification:   & curl -s http://localhost:8085/actuator/health 2>nul || echo UNREACHABLE
	@echo.
	@echo API Gateway:    & curl -s http://localhost:8080/actuator/health 2>nul || echo UNREACHABLE
	@echo.

# ── Kafka ────────────────────────────────────────────────────────────────────

.PHONY: kafka-topics
kafka-topics: ## List all Kafka topics
	docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

.PHONY: kafka-listen
kafka-listen: ## Listen to a Kafka topic (use: make kafka-listen TOPIC=allocation.confirmed)
	docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic $(TOPIC) --from-beginning

# ── Help ─────────────────────────────────────────────────────────────────────

.PHONY: help
help: ## Show this help
	@echo.
	@echo Available commands:
	@echo.
	@echo   make infra          Start databases + Kafka
	@echo   make dev            Start infra + show local run instructions
	@echo   make run-all        Start infra + all services locally (separate windows)
	@echo   make build          Build all service JARs
	@echo   make up             Build JARs + start everything in Docker
	@echo   make stop           Stop all Docker containers
	@echo   make clean          Stop + delete all data (fresh start)
	@echo   make health         Check health of all services
	@echo   make ps             Show running containers
	@echo   make logs           Tail Docker logs
	@echo   make kafka-topics   List Kafka topics
	@echo   make restart        Full restart (stop + rebuild + start)
	@echo.

.DEFAULT_GOAL := help
