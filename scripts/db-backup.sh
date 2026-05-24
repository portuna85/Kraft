#!/usr/bin/env bash
# Usage: DB_USER=... DB_PASSWORD=... [DB_HOST=mariadb] [DB_NAME=kraft_lotto] ./scripts/db-backup.sh
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft-lotto}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-kraft_lotto}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/kraft_lotto_${TIMESTAMP}.sql.gz"

if [[ -z "${DB_USER:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_USER and DB_PASSWORD must be set" >&2
  exit 1
fi

mkdir -p "${BACKUP_DIR}"

mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --routines \
  --triggers \
  "${DB_NAME}" \
  | gzip > "${BACKUP_FILE}"

echo "Backup created: ${BACKUP_FILE}"

# Retention: remove backups older than RETENTION_DAYS
find "${BACKUP_DIR}" -name "kraft_lotto_*.sql.gz" -mtime "+${RETENTION_DAYS}" -delete
echo "Old backups cleaned (retention: ${RETENTION_DAYS} days)"
