#!/usr/bin/env bash
# flyway_schema_history 정합성 패치 — 배포 시 앱 기동 전에 실행된다.
#
# 처리 내용:
#   - 실패(success=FALSE) 행 제거
#     앱의 flyway.repair() 보다 먼저 DB를 정리해 기동 오류를 방지한다.
#
# 이전에 적용하던 V15~V25 → V2~V12 버전 번호 변경은 제거됐다.
# 해당 번호 변경은 db/migration/V2~V12 와 db/vendor/V15~V17 이 완전히
# 다른 내용의 마이그레이션임에도 같은 버전으로 매핑해 혼선을 일으켰다.
# repairV1SquashIfNeeded 가 구환경 이력을 처리하며, vendor 파일은
# IF NOT EXISTS 로 idempotent하게 변경했으므로 재실행해도 안전하다.
set -euo pipefail

SQL="$(cat <<'SQL_EOF'
DELETE FROM flyway_schema_history WHERE success = FALSE;
SQL_EOF
)"

echo "Patching flyway_schema_history: 실패 행 제거"
docker compose exec -T mariadb \
  bash -c 'MYSQL_PWD="$MARIADB_PASSWORD" mariadb -u"$MARIADB_USER" "$MARIADB_DATABASE" -v' <<< "$SQL"
echo "Patch complete"
