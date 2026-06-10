#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${ALL_SECRETS_JSON:-}" ]]; then
  echo "::error::ALL_SECRETS_JSON is required"
  exit 1
fi

if [[ -z "${KRAFT_DB_NAME:-}" ]]; then
  echo "::error::KRAFT_DB_NAME is required"
  exit 1
fi

umask 077
{
  printf 'SPRING_PROFILES_ACTIVE=prod\n'
  printf 'KRAFT_APP_IMAGE_REF=%s\n' "${KRAFT_APP_IMAGE_REF:-kraft-lotto-app}"
  printf 'KRAFT_APP_IMAGE_TAG=%s\n' "${KRAFT_APP_IMAGE_TAG:-${GITHUB_SHA:-local}}"
  printf 'KRAFT_APP_PORT=8080\n'
  printf 'KRAFT_DB_URL=jdbc:mariadb://mariadb:3306/%s?rewriteBatchedStatements=true&useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul&connectTimeout=3000\n' "${KRAFT_DB_NAME}"
  printf 'KRAFT_DB_LOCAL_HOST=localhost\n'
  printf 'KRAFT_IN_CONTAINER=true\n'
  printf 'KRAFT_DB_CONNECTIVITY_CHECK_ENABLED=false\n'
  printf 'KRAFT_API_CLIENT=%s\n'          "${KRAFT_API_CLIENT:-smok}"
  printf 'KRAFT_API_FALLBACK_CLIENT=%s\n' "${KRAFT_API_FALLBACK_CLIENT:-public-data}"
  printf 'KRAFT_API_URL=https://www.dhlottery.co.kr/common.do\n'
  printf 'KRAFT_API_CONNECT_TIMEOUT_MS=2000\n'
  printf 'KRAFT_API_READ_TIMEOUT_MS=3000\n'
  printf 'KRAFT_API_REQUEST_TIMEOUT_MS=10000\n'
  printf 'KRAFT_API_MAX_RETRIES=2\n'
  printf 'KRAFT_API_RETRY_BACKOFF_MS=200\n'
  printf 'KRAFT_RECOMMEND_MAX_ATTEMPTS=5000\n'
  printf 'KRAFT_SECURITY_TRUSTED_PROXIES=%s\n' "${KRAFT_SECURITY_TRUSTED_PROXIES:-172.18.0.0/16,127.0.0.1/32}"
  printf 'KRAFT_COLLECT_STOP_ON_FAILURE=true\n'
  printf 'KRAFT_COLLECT_LOG_RETENTION_ENABLED=true\n'
  printf 'KRAFT_HISTORY_INIT_ENABLED=false\n'
  printf 'KRAFT_COLLECT_LOG_RETENTION_DAYS=90\n'
  printf 'KRAFT_COLLECT_LOG_RETENTION_DELETE_BATCH_SIZE=1000\n'
  printf 'KRAFT_COLLECT_LOG_RETENTION_CRON=0 30 3 * * *\n'
  printf 'KRAFT_LOG_PATH=/app/logs\n'
  printf 'KRAFT_HEALTHCHECK_URL=http://localhost:8080/actuator/health/readiness\n'
  printf 'KRAFT_HEALTHCHECK_TIMEOUT_SECONDS=3\n'
  printf 'KRAFT_ALERTMANAGER_CONFIG_FILE=./deploy-state/alertmanager.yml\n'
  # 빈 값은 기록하지 않아 application.yml 기본값이 적용되도록 한다
  [[ -n "${KRAFT_DOMAIN:-}" ]]       && printf 'KRAFT_DOMAIN=%s\n'       "${KRAFT_DOMAIN}"
  [[ -n "${KRAFT_ADMIN_DOMAIN:-}" ]] && printf 'KRAFT_ADMIN_DOMAIN=%s\n' "${KRAFT_ADMIN_DOMAIN}"
  printf 'KRAFT_ADMIN_ENABLED=%s\n' "${KRAFT_ADMIN_ENABLED:-false}"
  # bcrypt 해시에 포함된 $ 가 Docker Compose 변수 치환 엔진에 의해 해석되지 않도록
  # 단일 따옴표로 감싼다 (env_file 파서는 따옴표를 벗겨 원본 값을 컨테이너에 전달)
  printf "KRAFT_ADMIN_PASSWORD_HASH='%s'\n" "${KRAFT_ADMIN_PASSWORD_HASH:-}"
} > .env

while IFS= read -r name; do
  [[ -z "$name" ]] && continue
  value="$(printf '%s' "$ALL_SECRETS_JSON" | jq -r --arg key "$name" '.[$key] // ""')"
  [[ -z "$value" ]] && continue
  if [[ "$value" == *$'\n'* ]]; then
    echo "::error::Secret '$name' contains a newline — refusing to write to .env"
    exit 1
  fi
  # 단일 따옴표로 감싸 $ 를 포함한 비밀값이 Docker Compose 변수 치환으로 훼손되지 않도록 한다
  printf "%s='%s'\n" "$name" "$value" >> .env
done < .required-envs

# optional secrets — 값이 있을 때만 .env에 추가
for opt_name in PUBLIC_DATA_API_KEY; do
  value="$(printf '%s' "$ALL_SECRETS_JSON" | jq -r --arg key "$opt_name" '.[$key] // ""')"
  [[ -z "$value" ]] && continue
  printf "%s='%s'\n" "$opt_name" "$value" >> .env
done

docker compose --env-file .env config -q
