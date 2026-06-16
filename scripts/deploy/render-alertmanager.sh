#!/usr/bin/env bash
# Renders infra/alertmanager/alertmanager.yml from the .tmpl file.
# Requires: ALERTMANAGER_SLACK_WEBHOOK_URL, ALERTMANAGER_SLACK_CHANNEL (optional)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMPL="$REPO_ROOT/infra/alertmanager/alertmanager.yml.tmpl"
OUT="$REPO_ROOT/infra/alertmanager/alertmanager.yml"

if [[ -z "${ALERTMANAGER_SLACK_WEBHOOK_URL:-}" ]]; then
  echo "WARN: ALERTMANAGER_SLACK_WEBHOOK_URL not set — Slack 알림 비활성화"
  ALERTMANAGER_SLACK_WEBHOOK_URL="http://localhost"
fi

envsubst < "$TMPL" > "$OUT"
echo "OK: rendered $OUT"
