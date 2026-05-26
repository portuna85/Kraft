#!/usr/bin/env bash
set -euo pipefail

PORT="${KRAFT_E2E_PORT:-18080}"
HOST_BASE_URL="http://127.0.0.1:${PORT}"
CONTAINER_BASE_URL="${KRAFT_E2E_CONTAINER_BASE_URL:-http://host.docker.internal:${PORT}}"
IMAGE="${KRAFT_PLAYWRIGHT_IMAGE:-mcr.microsoft.com/playwright:v1.60.0-noble}"
APP_LOG="${KRAFT_E2E_APP_LOG:-build/e2e-app.log}"

mkdir -p "$(dirname "$APP_LOG")"

APP_ARGS=(
  "--server.port=${PORT}"
  "--spring.profiles.active=local"
  "--kraft.skip.required-config-validator=true"
  "--spring.datasource.url=jdbc:h2:mem:e2e;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
  "--spring.datasource.driver-class-name=org.h2.Driver"
  "--spring.datasource.username=sa"
  "--spring.datasource.password="
  "--spring.jpa.hibernate.ddl-auto=create-drop"
  "--spring.flyway.enabled=false"
  "--kraft.db.connectivity-check.enabled=false"
  "--kraft.security.ops.enabled=false"
)

cleanup() {
  if [ -n "${APP_PID:-}" ] && kill -0 "$APP_PID" >/dev/null 2>&1; then
    kill "$APP_PID" >/dev/null 2>&1 || true
    wait "$APP_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

./gradlew bootRun --args="${APP_ARGS[*]}" >"$APP_LOG" 2>&1 &
APP_PID="$!"

for _ in $(seq 1 120); do
  if curl -fsS "$HOST_BASE_URL" >/dev/null 2>&1; then
    break
  fi
  if ! kill -0 "$APP_PID" >/dev/null 2>&1; then
    cat "$APP_LOG" >&2 || true
    echo "E2E app server exited before readiness" >&2
    exit 1
  fi
  sleep 1
done

curl -fsS "$HOST_BASE_URL" >/dev/null

docker run --rm \
  --add-host=host.docker.internal:host-gateway \
  -e KRAFT_E2E_BASE_URL="$CONTAINER_BASE_URL" \
  -e KRAFT_E2E_EXTERNAL_SERVER=true \
  -v "$PWD:/work" \
  -w /work \
  "$IMAGE" \
  bash -lc "npm ci --prefer-offline --no-audit && npm run test:e2e"
