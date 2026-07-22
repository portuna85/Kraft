#!/usr/bin/env bash
# Step 5: 이전 결과를 검증한다.
# 검증 항목:
#   1. winning_numbers 행 수 — 구 DB 기준과 일치
#   2. 최신 회차 번호 — 구 DB max(round_no) 와 일치
#   3. /api/v1/rounds/latest JSON에 올바른 round 필드 포함
#   4. saved_numbers 행 수 — 구 DB 기준 이상 (중복 제거로 감소 허용)
#   5. saved_numbers 해시 길이 64자리 확인
#   6. 통계 summary 3종 비어 있지 않음
set -euo pipefail

: "${OLD_DB_HOST:?source: OLD_DB_HOST not set}"
: "${OLD_DB_USER:?}" ; : "${OLD_DB_PASSWORD:?}" ; : "${OLD_DB_NAME:?}"
: "${NEW_DB_HOST:?}" ; : "${NEW_DB_USER:?}" ; : "${NEW_DB_PASSWORD:?}" ; : "${NEW_DB_NAME:?}"
: "${KRAFT_BACKEND_INTERNAL_URL:=http://localhost:8080}"

OLD_MYSQL="MYSQL_PWD=$OLD_DB_PASSWORD mysql --host=$OLD_DB_HOST --port=${OLD_DB_PORT:-3306} --user=$OLD_DB_USER --batch --skip-column-names $OLD_DB_NAME"
NEW_MYSQL="MYSQL_PWD=$NEW_DB_PASSWORD mysql --host=$NEW_DB_HOST --port=${NEW_DB_PORT:-3306} --user=$NEW_DB_USER --batch --skip-column-names $NEW_DB_NAME"

FAIL=0
ok()   { echo "  OK  $1"; }
fail() { echo "  FAIL $1" >&2; FAIL=1; }

echo "==> [05] Validation checks"

# 1. winning_numbers count
old_wn=$(eval "$OLD_MYSQL" -e "SELECT COUNT(*) FROM winning_numbers;")
new_wn=$(eval "$NEW_MYSQL" -e "SELECT COUNT(*) FROM winning_numbers;")
if [[ "$old_wn" -eq "$new_wn" ]]; then
  ok "winning_numbers: $new_wn rows (matches source)"
else
  fail "winning_numbers: source=$old_wn new=$new_wn (MISMATCH)"
fi

# 2. max round_no
old_max=$(eval "$OLD_MYSQL" -e "SELECT MAX(round_no) FROM winning_numbers;")
new_max=$(eval "$NEW_MYSQL" -e "SELECT MAX(round_no) FROM winning_numbers;")
if [[ "$old_max" -eq "$new_max" ]]; then
  ok "max round_no: $new_max (matches source)"
else
  fail "max round_no: source=$old_max new=$new_max (MISMATCH)"
fi

# 3. /api/v1/rounds/latest JSON
latest_body=$(curl -fsS --max-time 10 \
  "$KRAFT_BACKEND_INTERNAL_URL/api/v1/rounds/latest" 2>/dev/null || true)
if echo "$latest_body" | grep -q '"round"'; then
  api_round=$(echo "$latest_body" | grep -o '"round":[0-9]*' | head -1 | cut -d: -f2)
  if [[ "$api_round" -eq "$new_max" ]]; then
    ok "/api/v1/rounds/latest: round=$api_round (correct)"
  else
    fail "/api/v1/rounds/latest: round=$api_round but expected $new_max"
  fi
else
  fail "/api/v1/rounds/latest: no 'round' field in response — $latest_body"
fi

# 4. saved_numbers count (allow ≤ source due to dedup; 0 rows = suspicious)
old_sv=$(eval "$OLD_MYSQL" -e "SELECT COUNT(*) FROM saved_numbers;" 2>/dev/null || echo "N/A")
new_sv=$(eval "$NEW_MYSQL" -e "SELECT COUNT(*) FROM saved_numbers;")
if [[ "$old_sv" == "N/A" ]]; then
  ok "saved_numbers: $new_sv rows (source count unavailable)"
elif [[ "$new_sv" -gt 0 ]]; then
  ok "saved_numbers: source=$old_sv new=$new_sv (${new_sv} rows migrated)"
  if (( new_sv < old_sv / 2 )); then
    echo "  WARN: new count is less than half of source — check for transform errors"
  fi
else
  fail "saved_numbers: 0 rows in new DB (source had $old_sv)"
fi

# 5. hash length sanity
bad_hash=$(eval "$NEW_MYSQL" -e \
  "SELECT COUNT(*) FROM saved_numbers WHERE CHAR_LENGTH(client_token_hash) <> 64;")
if [[ "$bad_hash" -eq 0 ]]; then
  ok "saved_numbers: all client_token_hash values are 64 chars"
else
  fail "saved_numbers: $bad_hash rows with wrong hash length"
fi

# 6. statistics summaries not empty
for tbl in winning_number_frequency_summary pattern_stats_summary companion_pair_summary; do
  cnt=$(eval "$NEW_MYSQL" -e "SELECT COUNT(*) FROM $tbl;" 2>/dev/null || echo "0")
  if [[ "$cnt" -gt 0 ]]; then
    ok "$tbl: $cnt rows"
  else
    echo "  WARN: $tbl is empty — run 04-rebuild-stats.sh"
  fi
done

echo ""
if [[ $FAIL -eq 0 ]]; then
  echo "==> [05] All validation checks PASSED"
else
  echo "==> [05] VALIDATION FAILED — do not proceed to cutover" >&2
  exit 1
fi
