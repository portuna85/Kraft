#!/usr/bin/env bash
# 운영 데이터 이전 마스터 스크립트 (Phase 5 §8.5)
#
# ARCHIVED — 2026-06 옛 시스템 → KRAFT 데이터 이전은 이미 완료됨.
# 이 디렉터리의 스크립트는 그대로 재실행하지 말 것. 알려진 버그는
# scripts/archive/migrate-2026-06/README.md 참고.
#
# 사용법(당시 기준, 참고용):
#   1. cp scripts/archive/migrate-2026-06/env-migration.example.sh env-migration.sh
#   2. vi env-migration.sh  (접속 정보 입력)
#   3. source env-migration.sh
#   4. bash scripts/archive/migrate-2026-06/run-migration.sh
#
# 각 단계는 체크포인트 파일로 재시작을 지원한다.
# 특정 단계부터 재개: RESUME_STEP=3 bash scripts/archive/migrate-2026-06/run-migration.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
SCRIPT_DIR="$REPO_ROOT/scripts/archive/migrate-2026-06"
CHECKPOINT_DIR="${MIGRATE_DIR:-/tmp/kraft-migrate-$(date +%Y%m%d)}"
RESUME_STEP="${RESUME_STEP:-1}"
START_TIME=$(date +%s)

: "${OLD_DB_HOST:?source env-migration.sh before running this script}"

mkdir -p "$CHECKPOINT_DIR"
CHECKPOINT_FILE="$CHECKPOINT_DIR/.checkpoint"

checkpoint_done() {
  local step="$1"
  echo "$step" >> "$CHECKPOINT_FILE"
}

is_done() {
  local step="$1"
  grep -qxF "$step" "$CHECKPOINT_FILE" 2>/dev/null
}

run_step() {
  local step_num="$1" script="$2" label="$3"

  if (( step_num < RESUME_STEP )); then
    echo "[skip] Step $step_num: $label (resume mode)"
    return
  fi

  if is_done "step$step_num"; then
    echo "[done] Step $step_num: $label (already completed)"
    return
  fi

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo " Step $step_num / 6: $label"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  if bash "$SCRIPT_DIR/$script"; then
    checkpoint_done "step$step_num"
    echo " → Step $step_num complete."
  else
    echo ""
    echo "ERROR: Step $step_num failed. Fix the issue and re-run with:" >&2
    echo "  RESUME_STEP=$step_num bash scripts/migrate/run-migration.sh" >&2
    exit 1
  fi
}

cat <<HEADER

╔══════════════════════════════════════════════════════════════╗
║        KRAFT Lotto — Phase 5 운영 데이터 이전                ║
║        시작: $(date '+%Y-%m-%d %H:%M:%S')                        ║
╚══════════════════════════════════════════════════════════════╝

  구 DB: $OLD_DB_NAME @ $OLD_DB_HOST
  신 DB: $NEW_DB_NAME @ $NEW_DB_HOST
  작업 디렉터리: $MIGRATE_DIR

HEADER

# Safety confirmation
echo "WARNING: 이 스크립트는 신 DB에 데이터를 적재합니다."
echo "         신 DB가 Flyway V1–V7을 마쳤는지 확인하세요."
read -r -p "계속하려면 'migrate'를 입력하세요: " confirm
[[ "$confirm" == "migrate" ]] || { echo "Aborted."; exit 0; }

run_step 1 "01-dump-source.sh"           "구 DB 덤프"
run_step 2 "02-import-winning-numbers.sh" "winning_numbers / admin_audit_log 임포트"
run_step 3 "03-transform-saved-numbers.sh" "saved_numbers SHA2 변환 임포트"
run_step 4 "04-rebuild-stats.sh"           "통계 summary 리빌드"
run_step 5 "05-validate.sh"               "검증"
run_step 6 "06-cutover.sh"               "컷오버 (compose up + smoke test)"

ELAPSED=$(( $(date +%s) - START_TIME ))
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo " Phase 5 완료 — 소요 시간: ${ELAPSED}초"
echo " 체크포인트: $CHECKPOINT_FILE"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo " 잊지 말 것:"
echo "  □ GSC / Naver Search Advisor 사이트맵 재제출"
echo "  □ 복원 드릴 1회 실행: bash scripts/db-restore-drill.sh"
echo "  □ 구 저장소 GitHub archive"
