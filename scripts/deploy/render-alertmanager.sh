#!/usr/bin/env bash
# Renders infra/alertmanager/alertmanager.yml from the .tmpl file.
# Requires: ALERTMANAGER_DISCORD_WEBHOOK_URL, unless ALERTING_DISABLED=true is
# explicitly set (로컬/스테이징 전용 — 운영 배포는 옵트아웃 없이 webhook을 요구한다).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMPL="$REPO_ROOT/infra/alertmanager/alertmanager.yml.tmpl"
OUT="$REPO_ROOT/infra/alertmanager/alertmanager.yml"

if [[ -z "${ALERTMANAGER_DISCORD_WEBHOOK_URL:-}" ]]; then
  if [[ "${ALERTING_DISABLED:-false}" != "true" ]]; then
    echo "ERROR: ALERTMANAGER_DISCORD_WEBHOOK_URL not set. Set it, or set ALERTING_DISABLED=true to explicitly opt out (로컬/스테이징 전용)." >&2
    exit 1
  fi
  echo "WARN: ALERTING_DISABLED=true — Discord 알림 비활성화(더미 webhook 사용)"
  # Dummy but URL-valid value: Alertmanager rejects an empty webhook_url at
  # startup ("unsupported scheme \"\" for URL") and crash-loops. http://localhost
  # parses fine, so the container stays up; alerts just silently fail to send.
  ALERTMANAGER_DISCORD_WEBHOOK_URL="http://localhost"
fi
# envsubst runs as a separate child process and only sees *exported* vars —
# a plain assignment isn't enough if the var was never exported in the caller's
# shell to begin with.
export ALERTMANAGER_DISCORD_WEBHOOK_URL

envsubst < "$TMPL" > "$OUT"
echo "OK: rendered $OUT"
