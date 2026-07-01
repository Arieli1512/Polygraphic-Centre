#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

podman-compose down -v
./db/scripts/start.sh "$@"