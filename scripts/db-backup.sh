#!/usr/bin/env bash
# Creates a gzip-compressed mariadb-dump of the kraft_lotto database.
# The dump is written to $BACKUP_DIR (default: /var/backups/kraft).
#
# The mariadb container only listens on the internal `app` Docker network
# (docker-compose.prod.yml has no `ports:` mapping for it), so the dump is
# taken via `docker compose exec` using the container's own MARIADB_*
# credentials rather than a host TCP connection.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${COMPOSE_PROJECT_DIR:-$(dirname "$SCRIPT_DIR")}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
COMPOSE_ENV_FILE="${COMPOSE_ENV_FILE:-.env.prod}"
COMPOSE=(docker compose --env-file "$PROJECT_DIR/$COMPOSE_ENV_FILE" -f "$PROJECT_DIR/$COMPOSE_FILE")

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft}"
DB_NAME="${MARIADB_DATABASE:-kraft_lotto}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="$BACKUP_DIR/kraft_${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

echo "==> Backing up $DB_NAME to $FILENAME"
"${COMPOSE[@]}" exec -T mariadb sh -c \
  'MYSQL_PWD="$MARIADB_PASSWORD" mariadb-dump --single-transaction --routines --triggers --set-gtid-purged=OFF -u"$MARIADB_USER" "$MARIADB_DATABASE"' \
  | gzip -9 > "$FILENAME"

echo "==> Backup complete: $FILENAME ($(du -sh "$FILENAME" | cut -f1))"

# Prune old backups
find "$BACKUP_DIR" -name "kraft_${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
echo "==> Pruned backups older than ${RETENTION_DAYS} days"
