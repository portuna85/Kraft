#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${ALERTMANAGER_DISCORD_WEBHOOK_URL:-}" ]]; then
  echo "::warning::ALERTMANAGER_DISCORD_WEBHOOK_URL not set — skipping Alertmanager config render"
  exit 0
fi

mkdir -p deploy-state

python3 - <<'PY'
from pathlib import Path
import os
import sys

webhook = os.environ.get("ALERTMANAGER_DISCORD_WEBHOOK_URL", "").strip()
if not webhook:
    print("::warning::ALERTMANAGER_DISCORD_WEBHOOK_URL not set — skipping", file=sys.stderr)
    raise SystemExit(0)
if "\n" in webhook or "\r" in webhook:
    print("::error::ALERTMANAGER_DISCORD_WEBHOOK_URL contains a newline", file=sys.stderr)
    raise SystemExit(1)

template_path = Path("docker/alertmanager/alertmanager.yml")
target_path = Path("deploy-state/alertmanager.yml")
template = template_path.read_text(encoding="utf-8")
rendered = template.replace("${ALERTMANAGER_DISCORD_WEBHOOK_URL}", webhook)
target_path.write_text(rendered, encoding="utf-8")
PY

echo "Rendered Alertmanager config: deploy-state/alertmanager.yml"
