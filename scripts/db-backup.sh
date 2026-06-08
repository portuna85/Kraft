#!/usr/bin/env bash
# Usage: DB_USER=... DB_PASSWORD=... [DB_HOST=mariadb] [DB_NAME=kraft_lotto] ./scripts/db-backup.sh
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft-lotto}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-kraft_lotto}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"          # e.g. "s3:my-bucket/kraft-lotto-backups"
METRICS_DIR="${METRICS_DIR:-/var/lib/node_exporter/textfile}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/kraft_lotto_${TIMESTAMP}.sql.gz"
START_TIME=$(date +%s)

if [[ -z "${DB_USER:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_USER and DB_PASSWORD must be set" >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"

TMP_CNF="$(mktemp)"
chmod 600 "$TMP_CNF"
cat > "$TMP_CNF" <<EOF
[client]
user=${DB_USER}
password=${DB_PASSWORD}
host=${DB_HOST}
port=${DB_PORT}
EOF
trap 'rm -f "$TMP_CNF"' EXIT

mysqldump --defaults-extra-file="$TMP_CNF" \
  --single-transaction \
  --routines \
  --triggers \
  "${DB_NAME}" \
  | gzip > "${BACKUP_FILE}"

echo "Backup created: ${BACKUP_FILE}"

# Retention: remove backups older than RETENTION_DAYS
find "${BACKUP_DIR}" -name "kraft_lotto_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "Old backups cleaned (retention: ${RETENTION_DAYS} days)"

BACKUP_SIZE=$(stat -c%s "${BACKUP_FILE}" 2>/dev/null || stat -f%z "${BACKUP_FILE}" 2>/dev/null || echo 0)

# Offsite sync via rclone (optional — skip if RCLONE_REMOTE is unset)
OFFSITE_SUCCESS=0
if [[ -n "${RCLONE_REMOTE}" ]]; then
  if command -v rclone >/dev/null 2>&1; then
    rclone copy "${BACKUP_FILE}" "${RCLONE_REMOTE}/" --quiet
    echo "Offsite sync complete: ${RCLONE_REMOTE}"
    OFFSITE_SUCCESS=1
  else
    echo "WARNING: RCLONE_REMOTE is set but rclone is not installed — skipping offsite sync" >&2
  fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Prometheus textfile metrics for node_exporter --collector.textfile.directory
if [[ -n "${METRICS_DIR}" ]]; then
  mkdir -p "${METRICS_DIR}"
  cat > "${METRICS_DIR}/kraft_backup.prom" <<EOF
# HELP kraft_backup_last_success_timestamp_seconds Unix timestamp of the last successful backup
# TYPE kraft_backup_last_success_timestamp_seconds gauge
kraft_backup_last_success_timestamp_seconds $(date +%s)
# HELP kraft_backup_duration_seconds Duration of the last backup in seconds
# TYPE kraft_backup_duration_seconds gauge
kraft_backup_duration_seconds ${DURATION}
# HELP kraft_backup_size_bytes Size of the last backup file in bytes
# TYPE kraft_backup_size_bytes gauge
kraft_backup_size_bytes ${BACKUP_SIZE}
# HELP kraft_backup_offsite_success Whether the last offsite sync succeeded (1=yes, 0=no/skipped)
# TYPE kraft_backup_offsite_success gauge
kraft_backup_offsite_success ${OFFSITE_SUCCESS}
EOF
  echo "Metrics written: ${METRICS_DIR}/kraft_backup.prom"
fi
