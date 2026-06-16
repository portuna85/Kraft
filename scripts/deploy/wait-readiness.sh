#!/usr/bin/env bash
# Polls docker inspect health status until healthy or timeout.
# Uses Docker's built-in healthcheck — no port publishing required.
set -euo pipefail

CONTAINER="${BACKEND_CONTAINER:-kraft-backend}"
MAX_WAIT="${DEPLOY_HEALTH_TIMEOUT:-120}"
INTERVAL=5

echo "Waiting for $CONTAINER to become healthy (timeout ${MAX_WAIT}s)..."
elapsed=0
while true; do
  health=$(docker inspect --format='{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "missing")
  if [[ "$health" == "healthy" ]]; then
    echo "Backend is healthy (${elapsed}s elapsed)"
    exit 0
  fi
  if (( elapsed >= MAX_WAIT )); then
    echo "ERROR: $CONTAINER did not become healthy within ${MAX_WAIT}s (last status: ${health})" >&2
    docker logs "$CONTAINER" --tail 30 2>&1 || true
    exit 1
  fi
  echo "  status=${health} (${elapsed}s elapsed)..."
  sleep "$INTERVAL"
  elapsed=$(( elapsed + INTERVAL ))
done
