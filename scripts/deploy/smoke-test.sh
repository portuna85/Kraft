#!/usr/bin/env bash
# Post-deploy smoke test suite.
# Checks: /api/v1/stats/* returns 200;
#         previously removed paths are 404;
#         /admin is blocked (403) on the public domain.
set -euo pipefail

BASE="${KRAFT_PUBLIC_BASE_URL:-http://localhost}"
API="$BASE/api/v1"
FAIL=0

check_status() {
  local desc="$1" url="$2" expected="$3"
  # No -f flag: curl exits 0 even for 4xx/5xx so %{http_code} is captured cleanly.
  # "000" only appears on network-level failures (DNS, timeout, connection refused).
  actual=$(curl -o /dev/null -sS -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
  if [[ "$actual" == "$expected" ]]; then
    echo "  OK  [$actual] $desc"
  else
    echo "  FAIL[$actual != $expected] $desc ($url)" >&2
    FAIL=1
  fi
}

check_json_field() {
  local desc="$1" url="$2" field="$3"
  body=$(curl -sS --max-time 10 "$url" 2>/dev/null || true)
  if echo "$body" | grep -q "\"$field\""; then
    echo "  OK  [field:$field] $desc"
  else
    echo "  FAIL[missing field '$field'] $desc ($url)" >&2
    FAIL=1
  fi
}

echo "==> Smoke tests against $BASE"

# Core API — stats work even with an empty DB
check_status "GET /api/v1/stats/frequency → 200" "$API/stats/frequency" "200"
check_status "GET /api/v1/stats/patterns → 200"  "$API/stats/patterns"  "200"
check_status "GET /api/v1/stats/companion → 200" "$API/stats/companion" "200"

# Removed paths (blueprint §17) must be 404
check_status "GET /api/v1/push/token → 404"   "$API/push/token"   "404"
check_status "GET /news → 404"                 "$BASE/news"        "404"

# Next.js permanent:true redirects return 308 (not 301)
check_status "GET /data-source → 308"          "$BASE/data-source" "308"

# /admin* is blocked with 403 on the public domain (Caddyfile security rule)
check_status "GET /admin → 403 (blocked on public domain)"  "$BASE/admin" "403"

if [[ $FAIL -ne 0 ]]; then
  echo "==> Smoke test FAILED" >&2
  exit 1
fi
echo "==> All smoke tests passed"
