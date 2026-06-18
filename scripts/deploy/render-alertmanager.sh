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
# envsubst doesn't understand bash's ${VAR:-default} syntax — it only does
# plain $VAR/${VAR} substitution, leaving anything else as literal text. So
# the default has to be resolved here, not in the .tmpl file.
ALERTMANAGER_SLACK_CHANNEL="${ALERTMANAGER_SLACK_CHANNEL:-#alerts}"
# envsubst runs as a separate child process and only sees *exported* vars —
# a plain assignment above isn't enough if the var was never exported in the
# caller's shell to begin with (observed in production: rendered slack_api_url
# came out as "", which Alertmanager rejects at startup with "unsupported
# scheme \"\" for URL", crash-looping the container).
export ALERTMANAGER_SLACK_WEBHOOK_URL ALERTMANAGER_SLACK_CHANNEL

envsubst < "$TMPL" > "$OUT"
echo "OK: rendered $OUT"
