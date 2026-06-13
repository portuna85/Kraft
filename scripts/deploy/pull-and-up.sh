#!/usr/bin/env bash
# Pulls latest images and restarts only the services that changed.
# Usage: pull-and-up.sh [--env-file <path>]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"

cd "$REPO_ROOT"

echo "==> Validating environment..."
bash scripts/deploy/validate-env.sh

echo "==> Pulling images..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull --quiet

echo "==> Starting services (no-deps rolling update)..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --remove-orphans

echo "==> Waiting for readiness..."
bash scripts/deploy/wait-readiness.sh

echo "==> Running smoke test..."
bash scripts/deploy/smoke-test.sh

echo "Deploy complete."
