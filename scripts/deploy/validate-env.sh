#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${ALL_SECRETS_JSON:-}" ]]; then
  echo "::error::ALL_SECRETS_JSON is required"
  exit 1
fi

# Required production secrets — kept in sync with RequiredConfigValidator.REQUIRED_DEPLOY_ENV_VARS
cat > .required-envs <<'EOF'
KRAFT_DB_NAME
KRAFT_DB_USER
KRAFT_DB_PASSWORD
KRAFT_DB_ROOT_PASSWORD
KRAFT_SECURITY_OPS_REQUIRED_TOKEN
ALERTMANAGER_DISCORD_WEBHOOK_URL
EOF

while IFS= read -r name; do
  [[ -z "$name" ]] && continue
  value="$(printf '%s' "$ALL_SECRETS_JSON" | jq -r --arg key "$name" '.[$key] // ""')"
  if [[ -z "$value" ]]; then
    echo "::error::$name GitHub Secret is required for production deploy"
    exit 1
  fi
done < .required-envs
