#!/usr/bin/env bash
# Usage: DB_USER=... DB_PASSWORD=... [DB_HOST=mariadb] [DB_NAME=kraft_lotto] ./scripts/db-backup.sh
#
# Optional env vars:
#   RCLONE_REMOTE            — rclone destination, e.g. "s3:my-bucket/kraft-lotto-backups"
#   REMOTE_RETENTION_DAYS    — how many days to keep files on the remote (default: 30)
#   BACKUP_GPG_RECIPIENT     — GPG key ID / email to encrypt the dump; leave unset to skip
#   DRILL_INTERVAL_DAYS      — days between required restore drills (default: 90)
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft-lotto}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
REMOTE_RETENTION_DAYS="${REMOTE_RETENTION_DAYS:-30}"
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-kraft_lotto}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"          # e.g. "s3:my-bucket/kraft-lotto-backups"
BACKUP_GPG_RECIPIENT="${BACKUP_GPG_RECIPIENT:-}"  # GPG key ID or email; empty = no encryption
DRILL_INTERVAL_DAYS="${DRILL_INTERVAL_DAYS:-90}"
DRILL_STATE_FILE="${BACKUP_DIR}/.last_restore_drill"
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

# Optional GPG encryption — set BACKUP_GPG_RECIPIENT to enable
# Example: BACKUP_GPG_RECIPIENT=ops@example.com ./scripts/db-backup.sh
# The recipient's public key must be present in the gpg keyring on this host.
if [[ -n "${BACKUP_GPG_RECIPIENT}" ]]; then
  if ! command -v gpg >/dev/null 2>&1; then
    echo "ERROR: BACKUP_GPG_RECIPIENT is set but gpg is not installed" >&2
    exit 1
  fi
  gpg --batch --yes --encrypt --recipient "${BACKUP_GPG_RECIPIENT}" "${BACKUP_FILE}"
  rm -f "${BACKUP_FILE}"
  BACKUP_FILE="${BACKUP_FILE}.gpg"
  echo "Backup encrypted: ${BACKUP_FILE}"
fi

# Retention: remove local backups older than RETENTION_DAYS
find "${BACKUP_DIR}" -name "kraft_lotto_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
find "${BACKUP_DIR}" -name "kraft_lotto_*.sql.gz.gpg" -mtime "+${RETENTION_DAYS}" -delete
echo "Old local backups cleaned (retention: ${RETENTION_DAYS} days)"

BACKUP_SIZE=$(stat -c%s "${BACKUP_FILE}" 2>/dev/null || stat -f%z "${BACKUP_FILE}" 2>/dev/null || echo 0)

# Offsite sync via rclone (optional — skip if RCLONE_REMOTE is unset)
OFFSITE_SUCCESS=0
if [[ -n "${RCLONE_REMOTE}" ]]; then
  if command -v rclone >/dev/null 2>&1; then
    rclone copy "${BACKUP_FILE}" "${RCLONE_REMOTE}/" --quiet
    echo "Offsite sync complete: ${RCLONE_REMOTE}"
    # Remove remote files older than REMOTE_RETENTION_DAYS to enforce lifecycle
    rclone delete --min-age "${REMOTE_RETENTION_DAYS}d" "${RCLONE_REMOTE}/" --quiet
    echo "Remote files older than ${REMOTE_RETENTION_DAYS} days removed from ${RCLONE_REMOTE}"
    OFFSITE_SUCCESS=1
  else
    echo "WARNING: RCLONE_REMOTE is set but rclone is not installed — skipping offsite sync" >&2
  fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Restore drill freshness check
# Run db-restore-drill.sh quarterly to keep this metric green.
DRILL_OVERDUE=0
if [[ -f "${DRILL_STATE_FILE}" ]]; then
  LAST_DRILL=$(cat "${DRILL_STATE_FILE}")
  NOW=$(date +%s)
  DAYS_SINCE=$(( (NOW - LAST_DRILL) / 86400 ))
  if (( DAYS_SINCE > DRILL_INTERVAL_DAYS )); then
    DRILL_OVERDUE=1
    echo "WARNING: Restore drill overdue — last drill was ${DAYS_SINCE} days ago (threshold: ${DRILL_INTERVAL_DAYS})" >&2
  fi
else
  DRILL_OVERDUE=1
  echo "WARNING: No restore drill on record — run db-restore-drill.sh and a drill entry will be created" >&2
fi

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
# HELP kraft_backup_restore_drill_overdue 1 if a restore drill has not been completed within DRILL_INTERVAL_DAYS
# TYPE kraft_backup_restore_drill_overdue gauge
kraft_backup_restore_drill_overdue ${DRILL_OVERDUE}
EOF
  echo "Metrics written: ${METRICS_DIR}/kraft_backup.prom"
fi
