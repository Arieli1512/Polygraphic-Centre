#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."

if [[ "$1" == "--watch" ]]; then
    podman-compose up postgres
else
    podman-compose up -d postgres
fi