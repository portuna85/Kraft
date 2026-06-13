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

# Use envsubst to replace ${VAR} in template
envsubst < "$TEMPLATE" > "$OUTPUT"
chmod 600 "$OUTPUT"
echo "OK: rendered $OUTPUT from $TEMPLATE"
