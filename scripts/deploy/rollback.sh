#!/usr/bin/env bash
# Rolls back to the previous image tag by restarting with the prior SHA tag.
# Usage: rollback.sh <service> <previous-image-ref>
# Example: rollback.sh backend ghcr.io/owner/kraft-backend:sha-abc1234
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"

SERVICE="${1:?Usage: rollback.sh <service> <image-ref>}"
IMAGE_REF="${2:?Usage: rollback.sh <service> <image-ref>}"

cd "$REPO_ROOT"

echo "==> Rolling back $SERVICE to $IMAGE_REF"

# Split full image ref (repo:tag) into separate env vars that docker-compose.prod.yml expects
IMAGE_TAG="${IMAGE_REF##*:}"
IMAGE_REPO="${IMAGE_REF%:*}"

case "$SERVICE" in
  backend)
    export KRAFT_BACKEND_IMAGE_REF="$IMAGE_REPO"
    export KRAFT_BACKEND_IMAGE_TAG="$IMAGE_TAG"
    ;;
  web)
    export KRAFT_WEB_IMAGE_REF="$IMAGE_REPO"
    export KRAFT_WEB_IMAGE_TAG="$IMAGE_TAG"
    ;;
  *)
    echo "Unknown service: $SERVICE (expected backend or web)" >&2
    exit 1
    ;;
esac

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" \
  up -d --no-deps "$SERVICE"

echo "==> Waiting for readiness after rollback..."
bash scripts/deploy/wait-readiness.sh

echo "==> Running smoke test..."
bash scripts/deploy/smoke-test.sh

echo "Rollback complete: $SERVICE → $IMAGE_REF"
