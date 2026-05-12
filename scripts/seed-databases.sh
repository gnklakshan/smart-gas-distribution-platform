#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

run_seed() {
    local container="$1"
    local database="$2"
    local sql_file="$3"

    echo "Seeding ${database} from ${sql_file}"
    docker exec -i "${container}" psql -U postgres -d "${database}" < "${ROOT_DIR}/${sql_file}"
}

run_seed "postgres-user" "userdb" "db/seed/userdb.sql"
run_seed "postgres-inventory" "inventorydb" "db/seed/inventorydb.sql"

if docker ps --format '{{.Names}}' | rg -x 'postgres-allocation' >/dev/null 2>&1; then
    run_seed "postgres-allocation" "allocationdb" "db/seed/allocationdb.sql"
fi

echo
echo "Seed complete."
echo "Shared password for all seeded users: Pass123!"
echo "Admin NIC: 199001230001"
echo "Dealer 1 NIC: 987654321V"
echo "Dealer 2 NIC: 876543210V"
echo "Citizen NIC: 123456789V"
