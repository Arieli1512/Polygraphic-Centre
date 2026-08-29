#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

podman-compose exec postgres \
    psql -U postgres -d drobnyd