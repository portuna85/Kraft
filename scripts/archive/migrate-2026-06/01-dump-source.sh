#!/usr/bin/env bash
# Step 1: 구 DB에서 필요한 테이블을 덤프한다.
# 덤프 파일:
#   winning_numbers.sql.gz     — 회차 데이터 (complete-insert, 신 스키마 호환)
#   admin_audit_log.sql.gz     — 감사 로그
#   saved_numbers_raw.sql.gz   — saved_numbers 원본 (03 단계에서 변환)
#   [선택] fetch_logs.sql.gz   — 수집 로그 (MIGRATE_FETCH_LOGS=true 시)
set -euo pipefail

: "${OLD_DB_HOST:?source: OLD_DB_HOST not set — source env-migration.sh first}"
: "${OLD_DB_USER:?}"
: "${OLD_DB_PASSWORD:?}"
: "${OLD_DB_NAME:?}"
: "${MIGRATE_DIR:?}"

mkdir -p "$MIGRATE_DIR"
chmod 700 "$MIGRATE_DIR"

OLD="MYSQL_PWD=$OLD_DB_PASSWORD mysqldump \
  --host=$OLD_DB_HOST \
  --port=${OLD_DB_PORT:-3306} \
  --user=$OLD_DB_USER \
  --single-transaction \
  --no-create-info \
  --complete-insert \
  --set-gtid-purged=OFF \
  $OLD_DB_NAME"

dump_table() {
  local table="$1" file="$2"
  echo "  Dumping $table → $file ..."
  eval "$OLD $table" | gzip -9 > "$MIGRATE_DIR/$file"
  local rows
  rows=$(gunzip -c "$MIGRATE_DIR/$file" | grep -c "^INSERT" || true)
  echo "  Done: $rows INSERT statements"
}

echo "==> [01] Dumping from $OLD_DB_NAME@$OLD_DB_HOST"
dump_table "winning_numbers"   "winning_numbers.sql.gz"
dump_table "admin_audit_log"   "admin_audit_log.sql.gz"
dump_table "saved_numbers"     "saved_numbers_raw.sql.gz"

if [[ "${MIGRATE_FETCH_LOGS:-false}" == "true" ]]; then
  dump_table "winning_number_operation_logs" "fetch_logs.sql.gz"
fi

echo "==> [01] Dump complete. Files in $MIGRATE_DIR:"
ls -lh "$MIGRATE_DIR"/*.sql.gz
