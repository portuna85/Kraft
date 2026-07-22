#!/usr/bin/env bash
# Step 2: winning_numbers와 admin_audit_log를 신 DB로 임포트한다.
# winning_numbers의 신규 컬럼(second_prize 등)은 DEFAULT(0/NULL)로 채워진다.
# 완료 후 외부 API로 누락 회차를 보충 수집한다(선택).
set -euo pipefail

: "${NEW_DB_HOST:?source: NEW_DB_HOST not set}"
: "${NEW_DB_USER:?}"
: "${NEW_DB_PASSWORD:?}"
: "${NEW_DB_NAME:?}"
: "${MIGRATE_DIR:?}"

NEW_MYSQL="MYSQL_PWD=$NEW_DB_PASSWORD mysql \
  --host=$NEW_DB_HOST \
  --port=${NEW_DB_PORT:-3306} \
  --user=$NEW_DB_USER \
  $NEW_DB_NAME"

load_file() {
  local label="$1" file="$MIGRATE_DIR/$2"
  if [[ ! -f "$file" ]]; then
    echo "  SKIP: $file not found"
    return
  fi
  echo "  Loading $label from $file ..."
  gunzip -c "$file" | eval "$NEW_MYSQL"
  echo "  Done."
}

echo "==> [02] Importing core data into $NEW_DB_NAME@$NEW_DB_HOST"

# Disable FK checks during bulk load
eval "$NEW_MYSQL" <<'SQL'
SET foreign_key_checks = 0;
SET unique_checks = 0;
SET sql_log_bin = 0;
SQL

load_file "winning_numbers"   "winning_numbers.sql.gz"
load_file "admin_audit_log"   "admin_audit_log.sql.gz"

if [[ "${MIGRATE_FETCH_LOGS:-false}" == "true" ]]; then
  load_file "fetch_logs" "fetch_logs.sql.gz"
fi

eval "$NEW_MYSQL" <<'SQL'
SET foreign_key_checks = 1;
SET unique_checks = 1;
SQL

# Print row counts for verification
eval "$NEW_MYSQL" --batch --skip-column-names <<'SQL'
SELECT
  'winning_numbers'   AS tbl, COUNT(*) AS cnt FROM winning_numbers
UNION ALL
SELECT 'admin_audit_log', COUNT(*) FROM admin_audit_log;
SQL

echo "==> [02] Import complete."
