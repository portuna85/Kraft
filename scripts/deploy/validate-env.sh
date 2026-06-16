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
  KRAFT_SECURITY_TRUSTED_PROXY_CIDR
  GRAFANA_ADMIN_PASSWORD
)

OPTIONAL_VARS=(
  ALERTMANAGER_SLACK_WEBHOOK_URL
  KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE
  KRAFT_EXTERNAL_LOTTO_AUTO_COLLECT_CRON
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

[[ $error -eq 0 ]] && echo "OK: all required variables are set" || exit 1
