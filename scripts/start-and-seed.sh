#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT_DIR}"

echo "Packaging services"
mvn -q -DskipTests package -f user-service/pom.xml
mvn -q -DskipTests package -f inventory-service/pom.xml
mvn -q -DskipTests package -f allocation-service/pom.xml
mvn -q -DskipTests package -f discovery-server/pom.xml
mvn -q -DskipTests package -f api-gateway/pom.xml

echo "Starting Docker stack"
docker compose up -d --build postgres-user postgres-inventory postgres-allocation eureka-server user-service inventory-service allocation-service api-gateway

echo "Applying seed data"
"${ROOT_DIR}/scripts/seed-databases.sh"

echo
echo "System is up for testing."
