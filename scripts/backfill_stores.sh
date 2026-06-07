#!/usr/bin/env bash
# 당첨 판매점 백필 스크립트 — 지정 회차 범위를 순차 수집
# 사용법: ./scripts/backfill_stores.sh <from_round> <to_round> <ops_token>
# 예시:   ./scripts/backfill_stores.sh 1176 1227 <token>

set -euo pipefail

FROM="${1:?사용법: $0 <from_round> <to_round> <ops_token>}"
TO="${2:?사용법: $0 <from_round> <to_round> <ops_token>}"
TOKEN="${3:?사용법: $0 <from_round> <to_round> <ops_token>}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
DELAY_SEC="${DELAY_SEC:-10}"

total=$(( TO - FROM + 1 ))
success=0
fail=0

echo "백필 시작: ${FROM}회 ~ ${TO}회 (총 ${total}회차), 간격 ${DELAY_SEC}s"

for round in $(seq "$FROM" "$TO"); do
  response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${BASE_URL}/ops/collect/stores?round=${round}" \
    -H "X-Ops-Token: ${TOKEN}")

  if [ "$response" = "200" ]; then
    echo "  [OK]   ${round}회"
    (( success++ )) || true
  else
    echo "  [FAIL] ${round}회 (HTTP ${response})"
    (( fail++ )) || true
  fi

  if [ "$round" -lt "$TO" ]; then
    sleep "$DELAY_SEC"
  fi
done

echo ""
echo "완료: 성공 ${success} / 실패 ${fail} / 전체 ${total}"
