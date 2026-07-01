@echo off
echo Building all services...

cd discovery-server
call mvnw clean package -DskipTests
cd ..

cd user-service
call mvnw clean package -DskipTests
cd ..

cd api-gateway
call mvnw clean package -DskipTests
cd ..

cd inventory-service
call mvnw clean package -DskipTests
cd ..

cd queue-service
call mvnw clean package -DskipTests
cd ..

echo All services built successfully!
echo Run: docker compose up --build