#!/usr/bin/env bash
# 앱 컨테이너 기동 대기 — Docker health status 와 직접 HTTP 폴링을 병행한다.
#
# Docker healthcheck 는 interval=15s 주기로만 갱신되므로, 앱이 이미 올라왔어도
# 다음 체크 사이클까지 최대 15s 지연이 생긴다.  직접 HTTP 폴링을 fast-path 로
# 추가해 이 지연을 제거한다.
#
# 환경 변수(선택):
#   CONTAINER_NAME   — 폴링 대상 컨테이너 이름 (기본: kraft-lotto-app)
#   MAX_ATTEMPTS     — 최대 시도 횟수 (기본: 60, 5s 간격 → 300s)
#   SLEEP_SECONDS    — 시도 사이 대기 초 (기본: 5)
#   READINESS_URL    — 직접 HTTP 폴링 URL (기본: http://localhost:8080/actuator/health/readiness)
set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-kraft-lotto-app}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-60}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"
READINESS_URL="${READINESS_URL:-http://localhost:8080/actuator/health/readiness}"

print_diagnostics() {
  echo "::group::docker compose ps"
  docker compose ps || true
  echo "::endgroup::"

  echo "::group::app container state"
  docker inspect \
    --format='status={{.State.Status}} exit={{.State.ExitCode}} error={{.State.Error}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' \
    "$CONTAINER_NAME" 2>/dev/null || true
  echo "::endgroup::"

  echo "::group::app logs (last 300 lines)"
  docker compose logs --tail=300 app || true
  echo "::endgroup::"

  echo "::group::mariadb logs"
  docker compose logs --tail=120 mariadb || true
  echo "::endgroup::"
}

echo "Waiting for app readiness — max $((MAX_ATTEMPTS * SLEEP_SECONDS))s (attempts=$MAX_ATTEMPTS, interval=${SLEEP_SECONDS}s)"
for i in $(seq 1 "$MAX_ATTEMPTS"); do
  # docker inspect 한 번 호출로 필요한 필드를 모두 읽는다 (3회→1회, 불일치 방지)
  raw="$(docker inspect \
    --format='{{.State.Status}}|{{.State.ExitCode}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' \
    "$CONTAINER_NAME" 2>/dev/null || echo "missing||missing")"
  STATE="${raw%%|*}"; _rest="${raw#*|}"
  EXIT_CODE="${_rest%%|*}"
  DOCKER_HEALTH="${_rest#*|}"

  echo "[$i/$MAX_ATTEMPTS] container=$STATE exit=$EXIT_CODE docker-health=$DOCKER_HEALTH"

  # 컨테이너가 종료된 경우 즉시 실패 처리
  if [[ "$STATE" == "exited" || "$STATE" == "dead" ]]; then
    echo "::error::App container stopped (exit=$EXIT_CODE) before readiness"
    print_diagnostics
    exit 1
  fi

  # Docker health status fast-path
  if [[ "$DOCKER_HEALTH" == "healthy" ]]; then
    echo "App is ready (docker health: healthy)"
    exit 0
  fi

  # 직접 HTTP 폴링 — Docker healthcheck 주기(15s) 보다 빠른 성공 감지
  if HTTP_CODE="$(curl -s --connect-timeout 2 --max-time 3 -o /dev/null \
       -w '%{http_code}' "$READINESS_URL" 2>/dev/null)" \
     && [[ "$HTTP_CODE" == "200" ]]; then
    echo "App is ready (HTTP 200 from $READINESS_URL)"
    exit 0
  fi

  sleep "$SLEEP_SECONDS"
done

echo "::error::Readiness check timed out after $((MAX_ATTEMPTS * SLEEP_SECONDS))s"
print_diagnostics
exit 1
