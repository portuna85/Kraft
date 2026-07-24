#!/usr/bin/env bash
# Renders .env.prod from .env.prod.example by substituting ${VAR} references.
# Usage: render-env.sh <template> <output>
set -euo pipefail

TEMPLATE="${1:-.env.prod.example}"
OUTPUT="${2:-.env.prod}"
# .env.prod와 별도 파일로 분리한 이유: docker-compose.prod.yml의 backend가 이 파일만
# env_file로 읽는다 — .env.prod 전체를 읽게 하면 MARIADB_ROOT_PASSWORD·
# GRAFANA_ADMIN_PASSWORD처럼 backend가 전혀 필요 없는 시크릿까지 컨테이너 환경변수로
# 새어 들어간다(최소 권한 원칙 위반).
OAUTH_FLAGS_OUTPUT="$(dirname "$OUTPUT")/.env.community-oauth-flags"

if [[ ! -f "$TEMPLATE" ]]; then
  echo "ERROR: template not found: $TEMPLATE" >&2
  exit 1
fi

# 치환 대상을 명시적으로 지정 — 인자 없이 envsubst를 쓰면 템플릿에 있는 ${VAR} 전부를
# 대상으로 삼아, 시크릿 값에 우연히 포함된 '$'나 주석 속 "${VAR}" 같은 문구까지 건드리고,
# 미설정 변수는 조용히 빈 문자열이 된다. 필수/선택 여부 검증은 이 스크립트 다음에 실행되는
# validate-env.sh(REQUIRED_VARS/OPTIONAL_VARS 구분)의 책임이라 여기서는 중복 검증하지 않는다.
ALLOWED_VARS=(
  KRAFT_DOMAIN KRAFT_ADMIN_DOMAIN KRAFT_PUBLIC_BASE_URL KRAFT_OPS_ALLOWED_HOST
  KRAFT_ADMIN_ALLOWED_CIDR KRAFT_SECURITY_TRUSTED_PROXY_CIDR
  KRAFT_BACKEND_IMAGE_REF KRAFT_BACKEND_IMAGE_TAG KRAFT_WEB_IMAGE_REF KRAFT_WEB_IMAGE_TAG
  KRAFT_DB_PASSWORD MARIADB_ROOT_PASSWORD MARIADB_PASSWORD GRAFANA_ADMIN_PASSWORD
  KRAFT_OPS_TOKEN KRAFT_REVALIDATE_SECRET KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE
  KRAFT_ADMIN_BOOTSTRAP_USERNAME KRAFT_ADMIN_BOOTSTRAP_PASSWORD
  ALERTMANAGER_DISCORD_WEBHOOK_URL
  KRAFT_COMMUNITY_GOOGLE_CLIENT_ID KRAFT_COMMUNITY_GOOGLE_CLIENT_SECRET
  KRAFT_COMMUNITY_NAVER_CLIENT_ID KRAFT_COMMUNITY_NAVER_CLIENT_SECRET
)

substitution_list=$(printf '${%s} ' "${ALLOWED_VARS[@]}")
# shellcheck disable=SC2086 # substitution_list는 envsubst가 요구하는 공백 구분 목록 형태라 unquoted 확장이 의도된 것
envsubst "$substitution_list" < "$TEMPLATE" > "$OUTPUT"

# application.yml의 google/naver OAuth2 등록은 이 플래그(on-property)로만 활성화된다.
# 반드시 "값이 있을 때만 이 줄 자체를 추가"해야 한다 — docker-compose의 environment:
# 블록처럼 ${VAR:-}로 빈 문자열을 강제로 채우면 on-property가 "존재하지만 빈 문자열"을
# 여전히 활성으로 판정해 목적을 못 이룬다. 이 줄이 없으면 컨테이너 환경변수 자체가 생기지 않는다.
: > "$OAUTH_FLAGS_OUTPUT"
if [[ -n "${KRAFT_COMMUNITY_GOOGLE_CLIENT_ID:-}" ]]; then
  echo "KRAFT_COMMUNITY_GOOGLE_OAUTH_ENABLED=true" >> "$OAUTH_FLAGS_OUTPUT"
fi
if [[ -n "${KRAFT_COMMUNITY_NAVER_CLIENT_ID:-}" ]]; then
  echo "KRAFT_COMMUNITY_NAVER_OAUTH_ENABLED=true" >> "$OAUTH_FLAGS_OUTPUT"
fi
chmod 600 "$OAUTH_FLAGS_OUTPUT"

chmod 600 "$OUTPUT"
echo "OK: rendered $OUTPUT from $TEMPLATE"
