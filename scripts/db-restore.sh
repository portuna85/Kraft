#!/usr/bin/env bash
# Usage: DB_USER=... DB_PASSWORD=... ./scripts/db-restore.sh /path/to/kraft_lotto_YYYYMMDD_HHMMSS.sql.gz
set -euo pipefail

BACKUP_FILE="${1:-}"
DB_HOST="${DB_HOST:-mariadb}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-kraft_lotto}"

if [[ -z "${DB_USER:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_USER and DB_PASSWORD must be set" >&2
  exit 1
fi

if [[ -z "${BACKUP_FILE}" || ! -f "${BACKUP_FILE}" ]]; then
  echo "Usage: $0 <backup-file.sql.gz>" >&2
  exit 1
fi

echo "Restoring ${BACKUP_FILE} → ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
read -r -p "Continue? (yes/no): " confirm
if [[ "${confirm}" != "yes" ]]; then
  echo "Aborted."
  exit 0
fi

gunzip -c "${BACKUP_FILE}" | mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  "${DB_NAME}"

echo "Restore complete."
