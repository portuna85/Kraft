#!/usr/bin/env bash
# 배포 성공(smoke-test 통과) 직후 호출. 재기동·재부팅 시 참조할 known-good 상태
# (SHA, backend/web 이미지)를 원자적으로 기록한다. rollback.sh가 이 파일을 읽어
# 롤백 시 git HEAD와 .env.prod를 이 시점으로 되돌린다.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MANIFEST="${KNOWN_GOOD_MANIFEST:-$REPO_ROOT/deploy/known-good.env}"

mkdir -p "$(dirname "$MANIFEST")"

SHA="$(git -C "$REPO_ROOT" rev-parse HEAD)"
BACKEND_IMAGE="$(docker inspect --format '{{.Config.Image}}' kraft-backend)"
WEB_IMAGE="$(docker inspect --format '{{.Config.Image}}' kraft-web)"

TMP="$(mktemp "$(dirname "$MANIFEST")/.known-good.XXXXXX")"
{
  echo "KNOWN_GOOD_SHA=$SHA"
  echo "KNOWN_GOOD_BACKEND_IMAGE=$BACKEND_IMAGE"
  echo "KNOWN_GOOD_WEB_IMAGE=$WEB_IMAGE"
  echo "KNOWN_GOOD_SAVED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$TMP"
mv "$TMP" "$MANIFEST"

echo "OK: known-good manifest saved ($MANIFEST): SHA=$SHA backend=$BACKEND_IMAGE web=$WEB_IMAGE"
