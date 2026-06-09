#!/usr/bin/env bash
# DEPRECATED: 환경 동기화 완료 후 이 스크립트는 더 이상 필요하지 않습니다.
# 모든 환경이 V2~V12 기준으로 이미 패치됐습니다. 신규 DB는 패치 없이 정상 기동됩니다.
#
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
-- description 패치: 버전 번호 변경 시 script 만 갱신하고 description 을 누락한 탓에 불일치 발생
-- 이미 올바른 값이면 동일 값 재기록이므로 무해(idempotent)
UPDATE flyway_schema_history SET description = 'trim fetch log indexes'          WHERE version = '2';
UPDATE flyway_schema_history SET description = 'add news source tier'             WHERE version = '3';
UPDATE flyway_schema_history SET description = 'add pattern stats summary'        WHERE version = '4';
UPDATE flyway_schema_history SET description = 'add companion pair summary'       WHERE version = '5';
UPDATE flyway_schema_history SET description = 'add news approved column'         WHERE version = '6';
UPDATE flyway_schema_history SET description = 'add winning store collected at'   WHERE version = '7';
UPDATE flyway_schema_history SET description = 'add news source domain'           WHERE version = '8';
UPDATE flyway_schema_history SET description = 'add admin tables'                 WHERE version = '9';
UPDATE flyway_schema_history SET description = 'add news rejected column'         WHERE version = '10';
UPDATE flyway_schema_history SET description = 'add winning store region columns' WHERE version = '11';
UPDATE flyway_schema_history SET description = 'add winning store source'         WHERE version = '12';
-- V2~V4 체크섬 패치: 57ca1a5 커밋에서 파일 내용이 변경되어 DB 체크섬과 불일치 발생
UPDATE flyway_schema_history SET checksum =  1392555576 WHERE version = '2'  AND checksum = 543663862;
UPDATE flyway_schema_history SET checksum = -1576893811 WHERE version = '3'  AND checksum = 845976414;
UPDATE flyway_schema_history SET checksum =  593166103  WHERE version = '4'  AND checksum = -1739479475;
SQL_EOF
)"

echo "Patching flyway_schema_history: V15~V25 → V2~V12, description·체크섬 정정"
docker compose exec -T mariadb \
  bash -c 'MYSQL_PWD="$MARIADB_PASSWORD" mariadb -u"$MARIADB_USER" "$MARIADB_DATABASE" -v' <<< "$SQL"
echo "Patch complete"
