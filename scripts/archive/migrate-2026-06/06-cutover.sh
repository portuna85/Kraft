#!/usr/bin/env bash
# Step 6: 신규 스택을 기동하고 컷오버한다.
# - 신규 compose 스택 up
# - wait-readiness → smoke-test
# - 이후 DNS 절체 / GSC·Naver Search Advisor 재제출은 수동 작업
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"

: "${KRAFT_PUBLIC_BASE_URL:?KRAFT_PUBLIC_BASE_URL not set (e.g. https://kraft.io.kr)}"

cd "$REPO_ROOT"

echo "==> [06] Cutover — starting new stack"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  # Fall back to default compose file in dev/staging environments
  COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
  echo "  (using $COMPOSE_FILE)"
fi

echo "  Pulling images..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull --quiet

echo "  Starting services..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --remove-orphans

echo "==> [06] Waiting for backend readiness..."
bash "$REPO_ROOT/scripts/deploy/wait-readiness.sh"

echo "==> [06] Running smoke tests..."
bash "$REPO_ROOT/scripts/deploy/smoke-test.sh"

cat <<'NOTES'

==> [06] Stack is UP and smoke tests passed.

──────────────────────────────────────────────────────────────────────────────
  수동 후속 작업 (DNS 절체 전 마지막 확인 후 실행)
──────────────────────────────────────────────────────────────────────────────

1. DNS 절체 (도메인이 바뀌는 경우)
   - 기존 서버 IP → 신규 서버 IP 로 A 레코드 변경
   - TTL이 낮은 경우(300s) 5분 후 전파 완료

2. Google Search Console (GSC) 재제출
   - https://search.google.com/search-console
   - sitemap 제출: {KRAFT_PUBLIC_BASE_URL}/sitemap.xml
   - URL 검사 → 재크롤 요청: /, /latest, /frequency, /stats, /companion, /analysis

3. Naver Search Advisor 재제출
   - https://searchadvisor.naver.com
   - 사이트맵 제출: {KRAFT_PUBLIC_BASE_URL}/sitemap.xml
   - 검색 요청 → 수집 요청

4. 복원 드릴 실행 (§15.3 수정 검증)
   bash scripts/db-restore-drill.sh

5. 구 저장소 archive
   - GitHub → Settings → Archive this repository
   - kLo-main 는 archive 상태로 보존 (데이터 이전 원본 참조 용도)

NOTES
