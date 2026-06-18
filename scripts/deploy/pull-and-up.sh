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

# Compose only recreates a container when its own config (image/env/etc.) changes —
# it cannot detect content changes inside a bind-mounted file like Caddyfile.
# Force a graceful in-process reload so edits to caddy/Caddyfile always take effect.
# --address pins the admin API to 127.0.0.1 explicitly: "localhost" can resolve to
# ::1 first inside the alpine container, which the admin listener doesn't bind,
# silently turning the reload into a no-op.
# --force is required: by default `caddy reload` diffs the adapted config against
# what's running and silently no-ops if it looks "the same" — observed in production
# where repeated Caddyfile fixes never took effect despite reload reporting success.
# Retried because on a fresh container the admin API may not be listening yet.
echo "==> Reloading Caddy config..."
for attempt in 1 2 3 4 5; do
  if docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T caddy \
      caddy reload --config /etc/caddy/Caddyfile --address 127.0.0.1:2019 --force; then
    break
  fi
  if [[ "$attempt" -eq 5 ]]; then
    echo "ERROR: caddy reload failed after 5 attempts" >&2
    exit 1
  fi
  sleep 2
done

echo "==> DEBUG: live Caddy admin config (routes for \$KRAFT_DOMAIN) ==="
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T caddy \
  wget -qO- http://127.0.0.1:2019/config/ || echo "  (admin config fetch failed)"
echo

echo "==> Waiting for readiness..."
bash scripts/deploy/wait-readiness.sh

echo "==> Running smoke test..."
bash scripts/deploy/smoke-test.sh

echo "Deploy complete."
