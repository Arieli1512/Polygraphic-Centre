#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

FILE_NAME="${1:-dump}"

docker compose exec -T postgres \
    pg_dump -U postgres -d drobnyd > "db/backups/$FILE_NAME.sql"