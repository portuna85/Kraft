#!/usr/bin/env bash
set -euo pipefail

# 컨테이너 내부 kraft 사용자 UID/GID(Dockerfile에서 10001로 고정)에 맞춰
# 호스트 ./logs 디렉토리 소유권을 정렬해 바인드 마운트 권한 문제를 방지한다.
KRAFT_UID="${KRAFT_UID:-10001}"
KRAFT_GID="${KRAFT_GID:-10001}"

mkdir -p ./logs
sudo chown -R "${KRAFT_UID}:${KRAFT_GID}" ./logs
sudo chmod -R 755 ./logs

