#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-kraft-lotto-app}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-120}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

print_diagnostics() {
  echo "::group::docker compose ps"
  docker compose ps || true
  echo "::endgroup::"

  echo "::group::app container state"
  docker inspect --format='status={{.State.Status}} exit={{.State.ExitCode}} error={{.State.Error}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$CONTAINER_NAME" 2>/dev/null || true
  echo "::endgroup::"

  echo "::group::app logs"
  docker compose logs --tail=250 app || true
  echo "::endgroup::"

  echo "::group::mariadb logs"
  docker compose logs --tail=120 mariadb || true
  echo "::endgroup::"
}

echo "Waiting for app readiness via Docker health status (max $((MAX_ATTEMPTS * SLEEP_SECONDS))s)"
for i in $(seq 1 "$MAX_ATTEMPTS"); do
  STATUS=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "$CONTAINER_NAME" 2>/dev/null || echo "missing")
  STATE=$(docker inspect --format='{{.State.Status}}' "$CONTAINER_NAME" 2>/dev/null || echo "missing")
  EXIT_CODE=$(docker inspect --format='{{.State.ExitCode}}' "$CONTAINER_NAME" 2>/dev/null || echo "")

  echo "[$i/$MAX_ATTEMPTS] container=$STATE exit=$EXIT_CODE docker-health=$STATUS"

  if [[ "$STATUS" == "healthy" ]]; then
    echo "App is ready (docker health: healthy)"
    exit 0
  fi
  if [[ "$STATE" == "exited" || "$STATE" == "dead" ]]; then
    echo "::error::App container stopped before readiness succeeded"
    print_diagnostics
    exit 1
  fi
  sleep "$SLEEP_SECONDS"
done

echo "::error::Readiness check timed out"
print_diagnostics
exit 1
