#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/../.."



if [[ "$1" == "--watch" ]]; then
    docker compose up postgres
else
    docker compose up -d postgres
fi