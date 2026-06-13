#!/usr/bin/env bash
# Restores a gzip-compressed mysqldump into the kraft_lotto database.
# Usage: db-restore.sh <backup-file.sql.gz>
set -euo pipefail

BACKUP_FILE="${1:?Usage: db-restore.sh <backup-file.sql.gz>}"
DB_HOST="${MARIADB_HOST:-127.0.0.1}"
DB_PORT="${MARIADB_PORT:-3306}"
DB_USER="${MARIADB_USER:-kraft_lotto}"
DB_PASS="${MARIADB_PASSWORD:?MARIADB_PASSWORD must be set}"
DB_NAME="${MARIADB_DATABASE:-kraft_lotto}"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "ERROR: backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

echo "WARNING: This will OVERWRITE the $DB_NAME database on $DB_HOST."
read -r -p "Type 'yes' to continue: " confirm
[[ "$confirm" == "yes" ]] || { echo "Aborted."; exit 0; }

echo "==> Restoring $BACKUP_FILE into $DB_NAME ..."
gunzip -c "$BACKUP_FILE" | MYSQL_PWD="$DB_PASS" mysql \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  "$DB_NAME"

echo "==> Restore complete."
