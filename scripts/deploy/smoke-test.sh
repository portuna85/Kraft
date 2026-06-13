#!/usr/bin/env bash
# Post-deploy smoke test suite.
# Checks: /api/v1/rounds/latest returns valid JSON with a round number;
#         /api/v1/stats/frequency returns 200;
#         previously removed paths are 404.
set -euo pipefail

BASE="${KRAFT_PUBLIC_BASE_URL:-http://localhost}"
API="$BASE/api/v1"
FAIL=0

check_status() {
  local desc="$1" url="$2" expected="$3"
  actual=$(curl -o /dev/null -fsS -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
  if [[ "$actual" == "$expected" ]]; then
    echo "  OK  [$actual] $desc"
  else
    echo "  FAIL[$actual != $expected] $desc ($url)" >&2
    FAIL=1
  fi
}

check_json_field() {
  local desc="$1" url="$2" field="$3"
  body=$(curl -fsS --max-time 10 "$url" 2>/dev/null || true)
  if echo "$body" | grep -q "\"$field\""; then
    echo "  OK  [field:$field] $desc"
  else
    echo "  FAIL[missing field '$field'] $desc ($url)" >&2
    FAIL=1
  fi
}

echo "==> Smoke tests against $BASE"

# Core API
check_status "GET /api/v1/rounds/latest → 200"  "$API/rounds/latest"   "200"
check_json_field "rounds/latest has 'round'"     "$API/rounds/latest"   "round"
check_status "GET /api/v1/stats/frequency → 200" "$API/stats/frequency" "200"
check_status "GET /api/v1/stats/patterns → 200"  "$API/stats/patterns"  "200"
check_status "GET /api/v1/stats/companion → 200" "$API/stats/companion" "200"

# Removed paths (blueprint §17) must be 404
check_status "GET /api/v1/push/token → 404"   "$API/push/token"   "404"
check_status "GET /news → 404"                 "$BASE/news"        "404"
check_status "GET /data-source → 301"          "$BASE/data-source" "301"

# Admin must redirect unauthenticated to login
check_status "GET /admin/dashboard → 302"      "$BASE/admin/dashboard" "302"

if [[ $FAIL -ne 0 ]]; then
  echo "==> Smoke test FAILED" >&2
  exit 1
fi
echo "==> All smoke tests passed"
