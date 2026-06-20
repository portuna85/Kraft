#!/usr/bin/env bash
# Post-deploy smoke test suite.
# Checks: /api/v1/stats/* returns 200;
#         key public headers are present;
#         previously removed paths are 404;
#         /admin is blocked (403) on the public domain.
set -euo pipefail

BASE="${KRAFT_PUBLIC_BASE_URL:-http://localhost}"
API="$BASE/api/v1"
FAIL=0

check_status() {
  local desc="$1" url="$2" expected="$3"
  local actual attempt
  for attempt in 1 2 3; do
    actual=$(curl -o /dev/null -sS -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
    [[ "$actual" == "$expected" ]] && break
    [[ "$attempt" -lt 3 ]] && sleep 2
  done
  if [[ "$actual" == "$expected" ]]; then
    echo "  OK  [$actual] $desc"
  else
    echo "  FAIL[$actual != $expected] $desc ($url)" >&2
    FAIL=1
  fi
}

check_header_contains() {
  local desc="$1" url="$2" header_name="$3" expected="$4"
  local headers value
  headers=$(curl -sSI --max-time 10 "$url" 2>/dev/null || true)
  value=$(printf '%s\n' "$headers" | awk -F': ' -v key="$header_name" 'tolower($1) == tolower(key) {print $2}' | tr -d '\r')
  if [[ -z "$value" ]]; then
    echo "  FAIL[missing header '$header_name'] $desc ($url)" >&2
    FAIL=1
  elif [[ -n "$expected" && "$value" != *"$expected"* ]]; then
    echo "  FAIL[header '$header_name' != '$expected'] $desc ($url)" >&2
    FAIL=1
  else
    echo "  OK  [header:$header_name] $desc"
  fi
}

echo "==> Smoke tests against $BASE"

# Core API: stats work even with an empty DB.
check_status "GET /api/v1/stats/frequency -> 200" "$API/stats/frequency" "200"
check_status "GET /api/v1/stats/patterns -> 200" "$API/stats/patterns" "200"
check_status "GET /api/v1/stats/companion -> 200" "$API/stats/companion" "200"
check_header_contains "GET /api/v1/stats/frequency exposes Cache-Control" "$API/stats/frequency" "Cache-Control" "max-age=60"
check_header_contains "GET /api/v1/stats/frequency exposes X-Request-Id" "$API/stats/frequency" "X-Request-Id" ""
check_header_contains "GET /api/v1/rounds/latest exposes Cache-Control" "$API/rounds/latest" "Cache-Control" "max-age=60"

# Removed paths (blueprint 17) must be 404.
check_status "GET /api/v1/push/token -> 404" "$API/push/token" "404"
check_status "GET /news -> 404" "$BASE/news" "404"

# Next.js permanent:true redirects return 308 (not 301).
check_status "GET /data-source -> 308" "$BASE/data-source" "308"

# /admin* is blocked with 403 on the public domain (Caddyfile security rule).
check_status "GET /admin -> 403 on public domain" "$BASE/admin" "403"

# Admin UI must still be reachable on the admin domain.
if [[ -n "${KRAFT_ADMIN_DOMAIN:-}" ]]; then
  check_status "GET admin domain /admin/login -> 200" "https://${KRAFT_ADMIN_DOMAIN}/admin/login" "200"
fi

if [[ "${KRAFT_SMOKE_RATE_LIMIT:-false}" == "true" ]]; then
  RATE_LIMIT_URL="$API/rounds/latest"
  for _ in $(seq 1 "${KRAFT_SMOKE_RATE_LIMIT_BURST:-130}"); do
    actual=$(curl -o /dev/null -sS -w "%{http_code}" --max-time 10 "$RATE_LIMIT_URL" 2>/dev/null || echo "000")
    [[ "$actual" == "429" ]] && break
  done
  check_status "GET /api/v1/rounds/latest -> 429 after burst" "$RATE_LIMIT_URL" "429"
  check_header_contains "429 responses expose Retry-After" "$RATE_LIMIT_URL" "Retry-After" "60"
fi

if [[ $FAIL -ne 0 ]]; then
  echo "==> Smoke test FAILED" >&2
  exit 1
fi
echo "==> All smoke tests passed"
