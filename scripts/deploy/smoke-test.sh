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

METRICS=$(curl -fsS http://localhost:8080/actuator/prometheus)
require_metric() {
  local pattern="$1"
  local label="$2"
  if ! grep -Eq "$pattern" <<< "$METRICS"; then
    echo "::error::Prometheus metric missing: $label"
    exit 1
  fi
}

optional_metric() {
  local pattern="$1"
  local label="$2"
  if ! grep -Eq "$pattern" <<< "$METRICS"; then
    echo "::warning::Prometheus event metric not present yet: $label"
  fi
}

require_metric '^kraft_api_circuit_breaker_state\{' 'kraft_api_circuit_breaker_state'
require_metric '^cache_size\{.*cache="winningNumberFrequency"' 'cache_size{cache="winningNumberFrequency"}'
optional_metric '^kraft_collect_auto_run_total\{' 'kraft_collect_auto_run_total'
optional_metric '^kraft_api_fallback_exhausted_total' 'kraft_api_fallback_exhausted_total'

if docker compose ps alertmanager 2>/dev/null | grep -Eq '(running|Up|healthy)'; then
  docker compose exec -T alertmanager amtool check-config /etc/alertmanager/alertmanager.yml
fi

# 데이터 freshness 점검: DB 최신 회차가 예상 회차와 일치하는지 확인
OPS_TOKEN="$(docker compose exec -T app sh -lc 'printenv KRAFT_SECURITY_OPS_REQUIRED_TOKEN' 2>/dev/null || true)"
if [[ -n "$OPS_TOKEN" ]]; then
  FRESHNESS=$(curl -fsS -H "X-Ops-Token: ${OPS_TOKEN}" http://localhost:8080/ops/data-freshness 2>/dev/null || true)
  if [[ -n "$FRESHNESS" ]]; then
    printf 'data-freshness: %s\n' "$FRESHNESS"
    FRESHNESS_STATUS=$(printf '%s' "$FRESHNESS" | grep -oP '"status"\s*:\s*"\K[^"]+' || true)
    if [[ "$FRESHNESS_STATUS" == "STALE" ]]; then
      echo "::warning::데이터 freshness 경고: DB 최신 회차가 예상 회차와 불일치합니다. $FRESHNESS"
    fi
  fi
fi

echo "Smoke test passed"
