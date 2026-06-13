#!/usr/bin/env bash
# Restoration drill: restores the latest backup into a temporary database,
# verifies required tables have rows, then drops the temp database.
# Validates the backup without touching production.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/kraft}"
DB_HOST="${MARIADB_HOST:-127.0.0.1}"
DB_PORT="${MARIADB_PORT:-3306}"
DB_ROOT_USER="${MARIADB_ROOT_USER:-root}"
DB_ROOT_PASS="${MARIADB_ROOT_PASSWORD:?MARIADB_ROOT_PASSWORD must be set}"
DB_NAME="${MARIADB_DATABASE:-kraft_lotto}"
DRILL_DB="kraft_drill_$$"

REQUIRED_TABLES=(winning_numbers saved_numbers admin_audit_log)

# Find latest backup
LATEST=$(find "$BACKUP_DIR" -name "kraft_${DB_NAME}_*.sql.gz" | sort | tail -1)
if [[ -z "$LATEST" ]]; then
  echo "ERROR: no backup found in $BACKUP_DIR" >&2
  exit 1
fi
echo "==> Drill restore from: $LATEST"

MYSQL="MYSQL_PWD=$DB_ROOT_PASS mysql --host=$DB_HOST --port=$DB_PORT --user=$DB_ROOT_USER"

cleanup() {
  echo "==> Dropping drill database $DRILL_DB"
  eval "$MYSQL" -e "DROP DATABASE IF EXISTS \`$DRILL_DB\`;" 2>/dev/null || true
}
trap cleanup EXIT

echo "==> Creating drill database $DRILL_DB"
eval "$MYSQL" -e "CREATE DATABASE \`$DRILL_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "==> Restoring dump..."
gunzip -c "$LATEST" | eval "$MYSQL" "$DRILL_DB"

FAIL=0
for table in "${REQUIRED_TABLES[@]}"; do
  count=$(eval "$MYSQL" --batch --skip-column-names -e \
    "SELECT COUNT(*) FROM \`$DRILL_DB\`.\`$table\`;" 2>/dev/null || echo "-1")
  if [[ "$count" -gt 0 ]]; then
    echo "  OK  $table: $count rows"
  else
    echo "  FAIL $table: $count rows (expected > 0)" >&2
    FAIL=1
  fi
done

[[ $FAIL -eq 0 ]] && echo "==> Drill PASSED" || { echo "==> Drill FAILED" >&2; exit 1; }
