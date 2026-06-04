#!/usr/bin/env bash
set -euo pipefail

target_ref="$(grep '^KRAFT_APP_IMAGE_REF=' .env 2>/dev/null | head -n1 | cut -d'=' -f2-)"
target_tag="$(grep '^KRAFT_APP_IMAGE_TAG=' .env 2>/dev/null | head -n1 | cut -d'=' -f2-)"
if [[ -z "$target_ref" ]]; then
  target_ref="kraft-lotto-app"
fi
if [[ -z "$target_tag" ]]; then
  target_tag="local"
fi

if [[ -f deploy-state/previous.env ]]; then
  # shellcheck disable=SC1091
  source deploy-state/previous.env
  if [[ -n "${PREVIOUS_REPO_DIGEST:-}" ]]; then
    echo "Rolling back using repo digest: ${PREVIOUS_REPO_DIGEST}"
    docker pull "${PREVIOUS_REPO_DIGEST}" || true
    docker tag "${PREVIOUS_REPO_DIGEST}" "${target_ref}:${target_tag}" || true
  elif [[ -n "${PREVIOUS_LOCAL_TAG:-}" ]] && docker image inspect "${PREVIOUS_LOCAL_TAG}" >/dev/null 2>&1; then
    echo "Rolling back using local tag: ${PREVIOUS_LOCAL_TAG}"
    docker tag "${PREVIOUS_LOCAL_TAG}" "${target_ref}:${target_tag}" || true
  elif [[ -n "${PREVIOUS_IMAGE:-}" ]]; then
    echo "Rolling back using image: ${PREVIOUS_IMAGE}"
    docker tag "${PREVIOUS_IMAGE}" "${target_ref}:${target_tag}" || true
  fi
fi

if docker image inspect "${target_ref}:${target_tag}" >/dev/null 2>&1; then
  docker compose up -d --no-build --no-deps app || true
  docker compose ps || true
  exit 0
fi

if docker image inspect kraft-lotto-app:previous >/dev/null 2>&1; then
  docker tag kraft-lotto-app:previous "${target_ref}:${target_tag}" || true
  docker compose up -d --no-build --no-deps app || true
  docker compose ps || true
  exit 0
fi

echo "::warning::No rollback image found."
