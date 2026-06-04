#!/usr/bin/env bash
# CI에서 빌드·푸시된 GHCR 이미지를 pull하여 배포한다 (prod 호스트 재빌드 없음).
set -euo pipefail

mkdir -p deploy-state

image_ref="${KRAFT_APP_IMAGE_REF:?KRAFT_APP_IMAGE_REF is required}"
image_tag="${KRAFT_APP_IMAGE_TAG:?KRAFT_APP_IMAGE_TAG is required}"
deployed_ref="${image_ref}:${image_tag}"
previous_local_tag="${image_ref}:previous"

: > deploy-state/previous.env

current_image="$(docker inspect --format='{{.Config.Image}}' kraft-lotto-app 2>/dev/null || true)"
if [[ -n "$current_image" ]] && docker image inspect "$current_image" >/dev/null 2>&1; then
  docker tag "$current_image" "$previous_local_tag" || true
  current_repo_digest="$(docker image inspect --format='{{range .RepoDigests}}{{println .}}{{end}}' "$current_image" 2>/dev/null | head -n1 || true)"
  current_image_id="$(docker image inspect --format='{{.Id}}' "$current_image" 2>/dev/null || true)"
  {
    echo "PREVIOUS_IMAGE=$current_image"
    echo "PREVIOUS_LOCAL_TAG=$previous_local_tag"
    echo "PREVIOUS_REPO_DIGEST=$current_repo_digest"
    echo "PREVIOUS_IMAGE_ID=$current_image_id"
  } >> deploy-state/previous.env
fi

docker compose pull app

# app만 교체한다. mariadb·prometheus·grafana·alertmanager는 재시작하지 않는다.
# compose 정의에서 제거된 orphan 정리가 필요할 때는 별도로
#   docker compose up -d --remove-orphans --no-recreate
# 를 명시적으로 실행한다.
docker compose up -d --no-deps app
docker compose ps

deployed_image="$(docker inspect --format='{{.Config.Image}}' kraft-lotto-app 2>/dev/null || true)"
if [[ -n "$deployed_image" ]]; then
  docker tag "$deployed_image" "${image_ref}:latest" || true
  echo "Deployed image: $deployed_image"
  deployed_digest="$(docker image inspect --format='{{range .RepoDigests}}{{println .}}{{end}}' "$deployed_image" 2>/dev/null | head -n1 || true)"
  {
    echo "CURRENT_IMAGE=$deployed_image"
    echo "CURRENT_IMAGE_REF=${image_ref}"
    echo "CURRENT_IMAGE_TAG=${image_tag}"
    echo "CURRENT_IMAGE_LATEST=${image_ref}:latest"
    echo "CURRENT_IMAGE_VERSIONED=${deployed_ref}"
    echo "CURRENT_DIGEST=$deployed_digest"
  } > deploy-state/current.env
fi
