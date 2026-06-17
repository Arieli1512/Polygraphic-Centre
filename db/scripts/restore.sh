#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

docker compose exec -T postgres psql -U postgres -d drobnyd -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
docker compose exec -T postgres psql -U postgres -d drobnyd < db/backups/dump.sql