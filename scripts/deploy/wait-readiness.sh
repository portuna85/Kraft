#!/usr/bin/env bash
# Polls docker inspect health status until the full stack (backend + caddy) is
# healthy or timeout. Caddy is the outermost entry point for smoke tests so it
# must be up before we run them.
set -euo pipefail

BACKEND_CONTAINER="${BACKEND_CONTAINER:-kraft-backend}"
CADDY_CONTAINER="${CADDY_CONTAINER:-kraft-caddy}"
MAX_WAIT="${DEPLOY_HEALTH_TIMEOUT:-180}"
INTERVAL=5

wait_healthy() {
  local name="$1"
  local elapsed=0
  echo "Waiting for $name to become healthy (timeout ${MAX_WAIT}s)..."
  while true; do
    health=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "missing")
    if [[ "$health" == "healthy" ]]; then
      echo "$name is healthy (${elapsed}s elapsed)"
      return 0
    fi
    if (( elapsed >= MAX_WAIT )); then
      echo "ERROR: $name did not become healthy within ${MAX_WAIT}s (last status: ${health})" >&2
      docker logs "$name" --tail 30 2>&1 || true
      return 1
    fi
    echo "  $name status=${health} (${elapsed}s elapsed)..."
    sleep "$INTERVAL"
    elapsed=$(( elapsed + INTERVAL ))
  done
}

wait_healthy "$BACKEND_CONTAINER"
wait_healthy "$CADDY_CONTAINER"
