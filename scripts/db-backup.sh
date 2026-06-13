#!/usr/bin/env bash
# Creates a gzip-compressed mysqldump of the kraft_lotto database.
# The dump is written to $BACKUP_DIR (default: /var/backups/kraft).
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft}"
DB_HOST="${MARIADB_HOST:-127.0.0.1}"
DB_PORT="${MARIADB_PORT:-3306}"
DB_USER="${MARIADB_USER:-kraft_lotto}"
DB_PASS="${MARIADB_PASSWORD:?MARIADB_PASSWORD must be set}"
DB_NAME="${MARIADB_DATABASE:-kraft_lotto}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="$BACKUP_DIR/kraft_${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

echo "==> Backing up $DB_NAME to $FILENAME"
MYSQL_PWD="$DB_PASS" mysqldump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USER" \
  --single-transaction \
  --routines \
  --triggers \
  --set-gtid-purged=OFF \
  "$DB_NAME" | gzip -9 > "$FILENAME"

echo "==> Backup complete: $FILENAME ($(du -sh "$FILENAME" | cut -f1))"

# Prune old backups
find "$BACKUP_DIR" -name "kraft_${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
echo "==> Pruned backups older than ${RETENTION_DAYS} days"
