#!/usr/bin/env bash
set -euo pipefail

BODY=$(curl -fsS http://localhost:8080/actuator/health/readiness)
printf '%s\n' "$BODY"
printf '%s' "$BODY" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'

HTTP=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/)
if [[ "$HTTP" != "200" ]]; then
  echo "::error::Home page smoke test failed: HTTP $HTTP"
  exit 1
fi
echo "Smoke test passed"
