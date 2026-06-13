#!/usr/bin/env bash
# Polls /actuator/health until status=UP or timeout.
set -euo pipefail

BASE_URL="${KRAFT_BACKEND_INTERNAL_URL:-http://localhost:8080}"
HEALTH_URL="$BASE_URL/actuator/health"
MAX_WAIT="${DEPLOY_HEALTH_TIMEOUT:-120}"
INTERVAL=5

echo "Waiting for backend at $HEALTH_URL (timeout ${MAX_WAIT}s)..."
elapsed=0
while true; do
  status=$(curl -fsS --max-time 5 "$HEALTH_URL" 2>/dev/null | grep -o '"status":"[A-Z]*"' | head -1 | cut -d'"' -f4 || true)
  if [[ "$status" == "UP" ]]; then
    echo "Backend is UP (${elapsed}s elapsed)"
    break
  fi
  if (( elapsed >= MAX_WAIT )); then
    echo "ERROR: backend did not become healthy within ${MAX_WAIT}s" >&2
    exit 1
  fi
  sleep "$INTERVAL"
  elapsed=$(( elapsed + INTERVAL ))
done
