#!/usr/bin/env bash
# Validates that all required environment variables are set before deploy.
set -euo pipefail

# KRAFT_DB_URL and KRAFT_DB_USERNAME are hardcoded in .env.prod.example — not shell env secrets
REQUIRED_VARS=(
  MARIADB_ROOT_PASSWORD
  MARIADB_PASSWORD
  KRAFT_DB_PASSWORD
  KRAFT_OPS_TOKEN
  KRAFT_REVALIDATE_SECRET
  KRAFT_PUBLIC_BASE_URL
  GRAFANA_ADMIN_PASSWORD
  KRAFT_ADMIN_ALLOWED_CIDR
)

OPTIONAL_VARS=(
  KRAFT_ADMIN_BOOTSTRAP_USERNAME
  KRAFT_ADMIN_BOOTSTRAP_PASSWORD
  ALERTMANAGER_DISCORD_WEBHOOK_URL
  KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE
  KRAFT_EXTERNAL_LOTTO_AUTO_COLLECT_CRON
  KRAFT_SECURITY_TRUSTED_PROXY_CIDR
  KRAFT_SECURITY_RATE_LIMIT_PER_MINUTE
  KRAFT_SECURITY_RATE_LIMIT_MAX_KEYS
  KRAFT_SAVED_MAX_PER_CLIENT
)

error=0
for var in "${REQUIRED_VARS[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "ERROR: required variable not set: $var" >&2
    error=1
  fi
done

for var in "${OPTIONAL_VARS[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "WARN:  optional variable not set (will use default): $var"
  fi
done

if [[ "${KRAFT_ADMIN_ALLOWED_CIDR:-}" == *"0.0.0.0/0"* && "${KRAFT_ALLOW_WORLD_OPEN_ADMIN:-}" != "true" ]]; then
  echo "ERROR: KRAFT_ADMIN_ALLOWED_CIDR가 전체 개방(0.0.0.0/0)입니다. 의도라면 KRAFT_ALLOW_WORLD_OPEN_ADMIN=true를 명시하세요." >&2
  error=1
fi

[[ $error -eq 0 ]] && echo "OK: all required variables are set" || exit 1
