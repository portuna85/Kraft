#!/usr/bin/env bash
# Reverts caddy/Caddyfile to a previous commit and force-recreates the caddy
# container so the reverted config actually takes effect.
# Usage: rollback-caddy.sh <previous-sha>
#
# Image rollback (rollback.sh) only swaps the backend/web image tags — it never
# touched caddy/Caddyfile, which git reset --hard had already moved to the new
# (possibly broken) commit. So a bad Caddyfile deploy could never self-heal via
# the existing rollback path. This script closes that gap.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"

PREV_SHA="${1:?Usage: rollback-caddy.sh <previous-sha>}"

cd "$REPO_ROOT"

echo "==> Reverting caddy/Caddyfile to $PREV_SHA"
git checkout "$PREV_SHA" -- caddy/Caddyfile

echo "==> Recreating Caddy with reverted config..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate --no-deps caddy
sleep 2

bash scripts/deploy/check-caddy-routes.sh
