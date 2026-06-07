#!/usr/bin/env bash
# flyway_schema_history 버전 번호를 V15~V25 → V2~V12 로 일괄 업데이트한다.
# 이미 패치됐거나 신규 DB인 경우(V15 행 없음) UPDATE 0건으로 정상 종료된다.
set -euo pipefail

SQL="$(cat <<'SQL_EOF'
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

echo "Patching flyway_schema_history: V15~V25 → V2~V12"
docker compose exec -T mariadb \
  bash -c 'mariadb -u"$MARIADB_USER" -p"$MARIADB_PASSWORD" "$MARIADB_DATABASE" -v' <<< "$SQL"
echo "Patch complete"
