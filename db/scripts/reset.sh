#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

docker compose down -v
./db/scripts/start.sh "$@"