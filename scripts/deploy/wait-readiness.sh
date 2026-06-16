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
    # Use {{if .State.Health}} to avoid nil-pointer template errors on containers
    # that have no healthcheck defined (e.g. caddy from a pre-healthcheck deploy).
    health=$(docker inspect \
      --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
      "$name" 2>/dev/null || echo "missing")

    case "$health" in
      healthy)
        echo "$name is healthy (${elapsed}s elapsed)"
        return 0
        ;;
      none)
        # Container is running but has no healthcheck — treat as ready.
        echo "$name is running (no healthcheck, ${elapsed}s elapsed)"
        return 0
        ;;
      missing)
        # docker inspect failed: container does not exist yet.
        ;;
      *)
        # starting | unhealthy — keep waiting.
        ;;
    esac

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
