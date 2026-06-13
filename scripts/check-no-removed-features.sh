#!/usr/bin/env bash
# scripts/check-no-removed-features.sh
# blueprint §17: 제거된 기능 잔재 검사 (자기참조 결함 수정판)
set -euo pipefail
FAIL=0

# 1) Flutter 부재
[ -d "app" ] && { echo "ERROR: app/ must not exist"; FAIL=1; }
find . -path ./.git -prune -o \( -name 'pubspec.yaml' -o -name '*.dart' \) -print | grep -q . \
  && { echo "ERROR: Flutter files remain"; FAIL=1; }

# 2) 푸시/FCM 부재 — 소스 한정, 자기 자신 제외
SRC_DIRS=(backend/src web/src infra scripts/deploy)
if grep -RIn --exclude="$(basename "$0")" -E 'feature/push|infra/fcm|firebase-admin|device_tokens|/api/v1/push' \
     "${SRC_DIRS[@]}" 2>/dev/null; then
  echo "ERROR: push/FCM references remain in source"; FAIL=1
fi

# 3) 뉴스 부재 — 단어 경계로 과매칭 방지
if grep -RInw --exclude="$(basename "$0")" -E 'news_articles|news_blocked_domain|news_blocked_keyword' \
     "${SRC_DIRS[@]}" 2>/dev/null; then
  echo "ERROR: news schema references remain"; FAIL=1
fi

exit "$FAIL"
