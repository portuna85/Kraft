#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"

check_http() {
  local url="$1"
  local expected="$2"
  local label="$3"
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}${url}")
  if [[ "$code" != "$expected" ]]; then
    echo "::error::${label}: expected HTTP ${expected}, got ${code} (${url})"
    return 1
  fi
}

check_body_contains() {
  local url="$1"
  local pattern="$2"
  local label="$3"
  local body
  body=$(curl -fsS "${BASE_URL}${url}" 2>/dev/null || true)
  if ! grep -q "$pattern" <<< "$body"; then
    echo "::error::${label}: '${pattern}' not found in response (${url})"
    return 1
  fi
}

# Readiness
BODY=$(curl -fsS "${BASE_URL}/actuator/health/readiness")
printf '%s\n' "$BODY"
printf '%s' "$BODY" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' || {
  echo "::error::Readiness check failed"
  exit 1
}

# 공개 페이지
check_http "/" "200" "홈 페이지"
check_http "/latest" "200" "최신 회차 페이지"
check_http "/rounds" "200" "회차 목록 페이지"
check_http "/frequency" "200" "빈도 분석 페이지"
check_http "/news" "200" "뉴스 페이지"
check_http "/data-source" "200" "데이터 출처 페이지"
# th:if 조건과 무관하게 항상 렌더링되는 텍스트로 검증
check_body_contains "/data-source" "당첨번호 데이터" "data-source 콘텐츠 확인"

# 보안 접근 제어 검증: 외부에서 보호 경로가 노출되지 않아야 함
# /actuator 는 localhost(자기 자신)에서 접근 허용이 정상이므로 체크 제외
check_admin() {
  local path="$1"
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' "${BASE_URL}${path}")
  if [[ "$code" == "200" ]]; then
    echo "::error::보안 경고: ${path} 가 HTTP 200을 반환했습니다. 외부에 노출되면 안 됩니다."
    return 1
  fi
  echo "OK: ${path} → HTTP ${code} (노출 안 됨)"
}
check_admin "/admin"
check_admin "/ops"

# Prometheus 메트릭
METRICS=$(curl -fsS "${BASE_URL}/actuator/prometheus" 2>/dev/null || true)
if [[ -n "$METRICS" ]]; then
  require_metric() {
    local pattern="$1"
    local label="$2"
    if ! grep -Eq "$pattern" <<< "$METRICS"; then
      echo "::error::Prometheus metric missing: $label"
      return 1
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
fi

if docker compose ps alertmanager 2>/dev/null | grep -Eq '(running|Up|healthy)'; then
  docker compose exec -T alertmanager amtool check-config /etc/alertmanager/alertmanager.yml
fi

# 데이터 freshness 점검
OPS_TOKEN="$(docker compose exec -T app sh -lc 'printenv KRAFT_SECURITY_OPS_REQUIRED_TOKEN' 2>/dev/null || true)"
if [[ -n "$OPS_TOKEN" ]]; then
  FRESHNESS=$(curl -fsS -H "X-Ops-Token: ${OPS_TOKEN}" "${BASE_URL}/ops/data-freshness" 2>/dev/null || true)
  if [[ -n "$FRESHNESS" ]]; then
    printf 'data-freshness: %s\n' "$FRESHNESS"
    FRESHNESS_STATUS=$(printf '%s' "$FRESHNESS" | grep -oP '"status"\s*:\s*"\K[^"]+' || true)
    if [[ "$FRESHNESS_STATUS" == "STALE" ]]; then
      echo "::warning::데이터 freshness 경고: DB 최신 회차가 예상 회차와 불일치합니다. $FRESHNESS"
    fi
    # expected vs stored gap 경고 (gap > 1)
    EXPECTED=$(printf '%s' "$FRESHNESS" | grep -oP '"expectedRound"\s*:\s*\K[0-9]+' || true)
    STORED=$(printf '%s' "$FRESHNESS" | grep -oP '"latestStoredRound"\s*:\s*\K[0-9]+' || true)
    if [[ -n "$EXPECTED" && -n "$STORED" ]]; then
      GAP=$(( EXPECTED - STORED ))
      if [[ $GAP -gt 1 ]]; then
        echo "::warning::회차 gap 경고: 예상 회차=${EXPECTED}, 저장 회차=${STORED}, gap=${GAP}"
      fi
    fi
    # /rounds maxRound 괴리 검증: UI 상한이 DB 저장 회차보다 HEADROOM(10) 이상 높으면 경고
    if [[ -n "$STORED" ]]; then
      ROUNDS_BODY=$(curl -fsS "${BASE_URL}/rounds" 2>/dev/null || true)
      MAX_ROUND_IN_UI=$(printf '%s' "$ROUNDS_BODY" | grep -oP '(?<=max=")[0-9]+' | head -1 || true)
      if [[ -n "$MAX_ROUND_IN_UI" ]]; then
        UI_GAP=$(( MAX_ROUND_IN_UI - STORED ))
        if [[ $UI_GAP -gt 10 ]]; then
          echo "::warning::/rounds maxRound=${MAX_ROUND_IN_UI} 이 DB 최신 회차=${STORED} 보다 ${UI_GAP}회 높습니다."
        else
          echo "OK: /rounds maxRound=${MAX_ROUND_IN_UI}, DB 최신 회차=${STORED}, gap=${UI_GAP}"
        fi
      fi
    fi
  fi
fi

echo "Smoke test passed"
