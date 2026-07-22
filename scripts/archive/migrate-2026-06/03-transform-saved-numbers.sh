#!/usr/bin/env bash
# Step 3: saved_numbers 변환 — device_token(평문)을 SHA2(device_token, 256)로 해시하여
#         신 스키마(client_token_hash CHAR(64))로 이전한다.
#
# 처리 순서:
#   a) 신 DB에 임시 스테이징 테이블 생성
#   b) 덤프 파일(device_token 포함 원본)을 스테이징에 적재
#   c) 스테이징 → saved_numbers 변환 INSERT (중복 numbers는 IGNORE)
#   d) 스테이징 테이블 삭제
#   e) 행 수 검증
set -euo pipefail

: "${NEW_DB_HOST:?source: NEW_DB_HOST not set}"
: "${NEW_DB_USER:?}"
: "${NEW_DB_PASSWORD:?}"
: "${NEW_DB_NAME:?}"
: "${MIGRATE_DIR:?}"
: "${OLD_SAVED_TOKEN_COL:=device_token}"
: "${OLD_SAVED_DATE_COL:=saved_at}"

DUMP_FILE="$MIGRATE_DIR/saved_numbers_raw.sql.gz"
if [[ ! -f "$DUMP_FILE" ]]; then
  echo "ERROR: $DUMP_FILE not found — run 01-dump-source.sh first" >&2
  exit 1
fi

NEW_MYSQL="MYSQL_PWD=$NEW_DB_PASSWORD mysql \
  --host=$NEW_DB_HOST \
  --port=${NEW_DB_PORT:-3306} \
  --user=$NEW_DB_USER \
  $NEW_DB_NAME"

echo "==> [03] Creating staging table _saved_numbers_staging ..."
eval "$NEW_MYSQL" <<SQL
DROP TABLE IF EXISTS _saved_numbers_staging;
CREATE TABLE _saved_numbers_staging LIKE saved_numbers;
ALTER TABLE _saved_numbers_staging
  DROP INDEX uk_saved_client_numbers,
  DROP INDEX idx_saved_client_created,
  ADD COLUMN raw_device_token VARCHAR(255) NULL;
ALTER TABLE _saved_numbers_staging
  MODIFY client_token_hash VARCHAR(255) NULL;
SQL

echo "==> [03] Loading raw dump into staging table ..."
# The dump may reference old column names; we load it into staging as-is
# then remap via SQL SELECT.
# To handle old schema column names, we use a Python one-liner to rewrite
# the INSERT column list if it references 'saved_numbers'.
STAGING_SQL=$(mktemp /tmp/saved_staging_XXXXXX.sql)
trap 'rm -f "$STAGING_SQL"' EXIT

gunzip -c "$DUMP_FILE" \
  | sed "s/INSERT INTO \`saved_numbers\`/INSERT INTO \`_saved_numbers_staging\`/g" \
  | sed "s/INSERT INTO saved_numbers/INSERT INTO _saved_numbers_staging/g" \
  > "$STAGING_SQL"

eval "$NEW_MYSQL" < "$STAGING_SQL"

echo "==> [03] Staging row count: $(eval "$NEW_MYSQL" --batch --skip-column-names -e 'SELECT COUNT(*) FROM _saved_numbers_staging')"

# Determine which column holds the token (auto-detect if staging has raw_device_token populated,
# else fall back to client_token_hash which was set by the dump to the old token value)
echo "==> [03] Transforming: SHA2(token, 256) → client_token_hash ..."
eval "$NEW_MYSQL" <<SQL
INSERT IGNORE INTO saved_numbers (client_token_hash, numbers, label, source, created_at)
SELECT
  SHA2(
    COALESCE(
      NULLIF(raw_device_token, ''),
      NULLIF(client_token_hash, '')
    ), 256
  ) AS client_token_hash,
  numbers,
  label,
  COALESCE(source, 'MANUAL') AS source,
  created_at
FROM _saved_numbers_staging
WHERE
  COALESCE(NULLIF(raw_device_token, ''), NULLIF(client_token_hash, '')) IS NOT NULL
  AND numbers IS NOT NULL
  AND numbers <> '';
SQL

NEW_COUNT=$(eval "$NEW_MYSQL" --batch --skip-column-names -e 'SELECT COUNT(*) FROM saved_numbers')
echo "==> [03] saved_numbers after transform: $NEW_COUNT rows"

echo "==> [03] Dropping staging table ..."
eval "$NEW_MYSQL" -e "DROP TABLE IF EXISTS _saved_numbers_staging;"

echo "==> [03] saved_numbers transform complete."
