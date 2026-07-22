#!/usr/bin/env bash
# Copy this file to env-migration.sh, fill in values, then source it before running scripts.
# NEVER commit the filled-in version — it contains credentials.

# ── 구(원본) DB 접속 정보 ────────────────────────────────────────────────────
export OLD_DB_HOST="127.0.0.1"
export OLD_DB_PORT="3306"
export OLD_DB_USER="kraft_lotto"
export OLD_DB_PASSWORD=""
export OLD_DB_NAME="kraft_lotto"

# 구 DB의 saved_numbers 테이블 컬럼명 (기본값은 kLo-main 실측 기준)
# device_token 컬럼명이 다르면 수정
export OLD_SAVED_TOKEN_COL="device_token"
# 날짜 컬럼명 (saved_at 또는 created_at)
export OLD_SAVED_DATE_COL="saved_at"

# ── 신(대상) DB 접속 정보 ────────────────────────────────────────────────────
export NEW_DB_HOST="127.0.0.1"
export NEW_DB_PORT="3306"
export NEW_DB_USER="kraft_lotto"
export NEW_DB_PASSWORD=""
export NEW_DB_NAME="kraft_lotto"

# ── 이전 작업 디렉터리 ───────────────────────────────────────────────────────
export MIGRATE_DIR="${MIGRATE_DIR:-/tmp/kraft-migrate-$(date +%Y%m%d)}"

# ── 신규 백엔드 내부 URL (검증 단계에서 API 호출에 사용) ─────────────────────
export KRAFT_BACKEND_INTERNAL_URL="${KRAFT_BACKEND_INTERNAL_URL:-http://localhost:8080}"

# ── lotto_fetch_logs 포함 여부 (90일 보존 데이터, 기본 false) ────────────────
export MIGRATE_FETCH_LOGS="${MIGRATE_FETCH_LOGS:-false}"
