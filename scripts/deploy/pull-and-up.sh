#!/usr/bin/env bash
# Pulls latest images and restarts only the services that changed.
# Usage: pull-and-up.sh [--env-file <path>]
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="$REPO_ROOT/docker-compose.prod.yml"

cd "$REPO_ROOT"

echo "==> Validating environment..."
bash scripts/deploy/validate-env.sh
echo "==> Admin allowlist CIDR: ${KRAFT_ADMIN_ALLOWED_CIDR:-<unset>}"

echo "==> Pulling images..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull --quiet

echo "==> Starting services (no-deps rolling update)..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --remove-orphans

# Compose only recreates a container when its own config (image/env/etc.) changes —
# it cannot detect content changes inside a bind-mounted file like Caddyfile, so a
# Caddyfile-only edit leaves the old container running untouched. `caddy reload`
# (even with --address/--force) repeatedly failed to pick up new config in
# production for reasons that don't reproduce locally, so instead we force a full
# container recreate every deploy: a fresh `caddy run` always reads the file fresh
# from disk, no reload mechanism involved. Costs a ~1s connection blip, trades
# reliability for that.
echo "==> Recreating Caddy to guarantee fresh config..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate --no-deps caddy
sleep 2

# alertmanager.yml(webhook 렌더링)과 prometheus.yml/rules(alert rule)도 같은 문제를 겪는다 —
# render-alertmanager.sh가 파일을 새로 써도 이미 떠 있는 컨테이너는 기동 시 로드한 예전 설정을
# 계속 메모리에 들고 있다. 예: webhook 값이 바뀌어도 재기동 전까지는 옛 값(더미 localhost 등)으로
# 계속 전송을 시도한다. Caddy와 동일하게 매 배포마다 강제 재생성한다.
echo "==> Recreating Alertmanager/Prometheus to guarantee fresh config..."
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate --no-deps alertmanager prometheus

echo "==> Fast local Caddy routing check (catches Caddyfile bugs before the slow external smoke test)..."
bash scripts/deploy/check-caddy-routes.sh

echo "==> Waiting for readiness..."
bash scripts/deploy/wait-readiness.sh

echo "==> Running smoke test..."
bash scripts/deploy/smoke-test.sh

echo "Deploy complete."
