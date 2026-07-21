#!/usr/bin/env bash
# Renders .env.prod from .env.prod.example by substituting ${VAR} references.
# Usage: render-env.sh <template> <output>
set -euo pipefail

TEMPLATE="${1:-.env.prod.example}"
OUTPUT="${2:-.env.prod}"

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
)

substitution_list=$(printf '${%s} ' "${ALLOWED_VARS[@]}")
# shellcheck disable=SC2086 # substitution_list는 envsubst가 요구하는 공백 구분 목록 형태라 unquoted 확장이 의도된 것
envsubst "$substitution_list" < "$TEMPLATE" > "$OUTPUT"
chmod 600 "$OUTPUT"
echo "OK: rendered $OUTPUT from $TEMPLATE"
