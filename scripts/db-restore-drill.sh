#!/usr/bin/env bash
# DB restore drill: 최신 백업을 임시 DB로 복원 후 테이블 존재 여부를 검증한다.
# Usage:
#   DB_USER=... DB_PASSWORD=... ./scripts/db-restore-drill.sh
#   DB_USER=... DB_PASSWORD=... BACKUP_FILE=/path/to/backup.sql.gz ./scripts/db-restore-drill.sh
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft-lotto}"
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DRILL_DB="${DRILL_DB:-kraft_lotto_drill}"
METRICS_DIR="${METRICS_DIR:-/var/lib/node_exporter/textfile}"

if [[ -z "${DB_USER:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_USER and DB_PASSWORD must be set" >&2
  exit 1
fi

# 복원할 백업 파일 결정 (명시 지정 없으면 최신 파일)
if [[ -z "${BACKUP_FILE:-}" ]]; then
  BACKUP_FILE=$(ls -t "${BACKUP_DIR}"/kraft_lotto_*.sql.gz 2>/dev/null | head -1)
fi

if [[ -z "${BACKUP_FILE}" || ! -f "${BACKUP_FILE}" ]]; then
  echo "ERROR: No backup file found (BACKUP_DIR=${BACKUP_DIR})" >&2
  exit 1
fi

echo "Restore drill: ${BACKUP_FILE} → ${DRILL_DB}@${DB_HOST}:${DB_PORT}"

TMP_CNF="$(mktemp)"
chmod 600 "$TMP_CNF"
cat > "$TMP_CNF" <<EOF
[client]
user=${DB_USER}
password=${DB_PASSWORD}
host=${DB_HOST}
port=${DB_PORT}
EOF
trap 'rm -f "$TMP_CNF"; mysql --defaults-extra-file="$TMP_CNF" -e "DROP DATABASE IF EXISTS \`${DRILL_DB}\`;" 2>/dev/null || true' EXIT

mysql --defaults-extra-file="$TMP_CNF" -e "DROP DATABASE IF EXISTS \`${DRILL_DB}\`; CREATE DATABASE \`${DRILL_DB}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

gunzip -c "${BACKUP_FILE}" | mysql --defaults-extra-file="$TMP_CNF" "${DRILL_DB}"

# 핵심 테이블 존재 및 행 수 검증
REQUIRED_TABLES=(winning_numbers news_articles admin_audit_log flyway_schema_history)
DRILL_OK=1

for TABLE in "${REQUIRED_TABLES[@]}"; do
  COUNT=$(mysql --defaults-extra-file="$TMP_CNF" "${DRILL_DB}" -sN \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DRILL_DB}' AND table_name='${TABLE}';" 2>/dev/null || echo 0)
  if [[ "${COUNT}" -ne 1 ]]; then
    echo "FAIL: table '${TABLE}' not found in restored DB" >&2
    DRILL_OK=0
  else
    ROWS=$(mysql --defaults-extra-file="$TMP_CNF" "${DRILL_DB}" -sN -e "SELECT COUNT(*) FROM \`${TABLE}\`;" 2>/dev/null || echo "?")
    echo "OK: ${TABLE} — ${ROWS} rows"
  fi
done

TIMESTAMP=$(date +%s)

if [[ "${DRILL_OK}" -eq 1 ]]; then
  echo "Restore drill PASSED"
else
  echo "Restore drill FAILED — check errors above" >&2
fi

# Prometheus textfile 메트릭
if [[ -n "${METRICS_DIR}" ]]; then
  mkdir -p "${METRICS_DIR}"
  cat > "${METRICS_DIR}/kraft_restore_drill.prom" <<EOF
# HELP kraft_restore_drill_last_run_timestamp_seconds Unix timestamp of the last restore drill run
# TYPE kraft_restore_drill_last_run_timestamp_seconds gauge
kraft_restore_drill_last_run_timestamp_seconds ${TIMESTAMP}
# HELP kraft_restore_drill_success Whether the last restore drill succeeded (1=yes, 0=no)
# TYPE kraft_restore_drill_success gauge
kraft_restore_drill_success ${DRILL_OK}
EOF
  echo "Metrics written: ${METRICS_DIR}/kraft_restore_drill.prom"
fi

[[ "${DRILL_OK}" -eq 1 ]] || exit 1
