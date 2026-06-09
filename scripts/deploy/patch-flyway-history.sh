#!/usr/bin/env bash
# flyway_schema_history 정합성 패치 — 배포 시 앱 기동 전에 실행된다.
#
# 처리 순서:
#   1. 실패(success=FALSE) 행 제거   — 앱의 flyway.repair() 보다 먼저 DB를 정리
#   2. 버전 번호 V15~V25 → V2~V12   — 57ca1a5 커밋의 마이그레이션 번호 정리에 대응
#      script·description·checksum 은 앱 기동 시 flyway.repair()가 자동 재정렬
#
# 이미 패치됐거나 신규 DB인 경우 UPDATE/DELETE 0건으로 정상 종료된다(idempotent).
set -euo pipefail

SQL="$(cat <<'SQL_EOF'
DELETE FROM flyway_schema_history WHERE success = FALSE;
UPDATE flyway_schema_history SET version = '2',  script = 'V2__trim_fetch_log_indexes.sql'           WHERE version = '15';
UPDATE flyway_schema_history SET version = '3',  script = 'V3__add_news_source_tier.sql'              WHERE version = '16';
UPDATE flyway_schema_history SET version = '4',  script = 'V4__add_pattern_stats_summary.sql'         WHERE version = '17';
UPDATE flyway_schema_history SET version = '5',  script = 'V5__add_companion_pair_summary.sql'        WHERE version = '18';
UPDATE flyway_schema_history SET version = '6',  script = 'V6__add_news_approved_column.sql'          WHERE version = '19';
UPDATE flyway_schema_history SET version = '7',  script = 'V7__add_winning_store_collected_at.sql'    WHERE version = '20';
UPDATE flyway_schema_history SET version = '8',  script = 'V8__add_news_source_domain.sql'            WHERE version = '21';
UPDATE flyway_schema_history SET version = '9',  script = 'V9__add_admin_tables.sql'                  WHERE version = '22';
UPDATE flyway_schema_history SET version = '10', script = 'V10__add_news_rejected_column.sql'         WHERE version = '23';
UPDATE flyway_schema_history SET version = '11', script = 'V11__add_winning_store_region_columns.sql' WHERE version = '24';
UPDATE flyway_schema_history SET version = '12', script = 'V12__add_winning_store_source.sql'         WHERE version = '25';
SQL_EOF
)"

echo "Patching flyway_schema_history: 실패 행 제거 + V15~V25 → V2~V12 버전 번호 변경"
docker compose exec -T mariadb \
  bash -c 'MYSQL_PWD="$MARIADB_PASSWORD" mariadb -u"$MARIADB_USER" "$MARIADB_DATABASE" -v' <<< "$SQL"
echo "Patch complete"
