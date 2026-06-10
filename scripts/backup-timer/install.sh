#!/usr/bin/env bash
# 서버에서 1회 실행: systemd backup timer 설치
# Usage: sudo ./scripts/backup-timer/install.sh /opt/kraft-lotto
set -euo pipefail

DEPLOY_DIR="${1:-/opt/kraft-lotto}"
ENV_FILE="/etc/kraft-lotto/backup.env"
SYSTEMD_DIR="/etc/systemd/system"
METRICS_DIR="/var/lib/node_exporter/textfile"

echo "=== kraft-lotto backup timer 설치 ==="

# 1. 배포 디렉터리에 스크립트 복사
mkdir -p "${DEPLOY_DIR}/scripts"
cp "$(dirname "$0")/../db-backup.sh"        "${DEPLOY_DIR}/scripts/db-backup.sh"
cp "$(dirname "$0")/../db-restore-drill.sh" "${DEPLOY_DIR}/scripts/db-restore-drill.sh"
chmod +x "${DEPLOY_DIR}/scripts/db-backup.sh"
chmod +x "${DEPLOY_DIR}/scripts/db-restore-drill.sh"

# 2. 환경 변수 파일 생성 (최초 1회 — 이미 존재하면 덮어쓰지 않음)
mkdir -p /etc/kraft-lotto
if [[ ! -f "${ENV_FILE}" ]]; then
  cat > "${ENV_FILE}" <<'EOF'
# kraft-lotto backup 환경 변수
DB_USER=
DB_PASSWORD=
DB_NAME=kraft_lotto
DB_PORT=3306
BACKUP_DIR=/var/backups/kraft-lotto
RETENTION_DAYS=7
# rclone 오프사이트 설정 (선택 — 비워두면 로컬 백업만 수행)
# RCLONE_REMOTE=s3:my-bucket/kraft-lotto-backups
RCLONE_REMOTE=
# 리모트 파일 보관 기간 (일, 기본 30)
REMOTE_RETENTION_DAYS=30
# GPG 암호화 수신자 (선택 — 비워두면 평문 압축만 수행)
# BACKUP_GPG_RECIPIENT=ops@example.com
BACKUP_GPG_RECIPIENT=
# 복원 드릴 주기 (일, 기본 90 = 분기)
DRILL_INTERVAL_DAYS=90
METRICS_DIR=/var/lib/node_exporter/textfile
EOF
  chmod 600 "${ENV_FILE}"
  echo "환경 변수 파일 생성됨: ${ENV_FILE}"
  echo ">>> DB_USER, DB_PASSWORD를 설정하세요: ${ENV_FILE}"
else
  echo "환경 변수 파일 이미 존재: ${ENV_FILE} (덮어쓰지 않음)"
fi

# 3. 백업 디렉터리 + 메트릭 디렉터리 생성
mkdir -p /var/backups/kraft-lotto
mkdir -p "${METRICS_DIR}"

# 4. systemd unit 파일 설치
cp "$(dirname "$0")/kraft-lotto-backup.service"        "${SYSTEMD_DIR}/"
cp "$(dirname "$0")/kraft-lotto-backup.timer"          "${SYSTEMD_DIR}/"
cp "$(dirname "$0")/kraft-lotto-restore-drill.service" "${SYSTEMD_DIR}/"
cp "$(dirname "$0")/kraft-lotto-restore-drill.timer"   "${SYSTEMD_DIR}/"

# 5. 배포 경로를 service 파일에 반영
sed -i "s|/opt/kraft-lotto|${DEPLOY_DIR}|g" "${SYSTEMD_DIR}/kraft-lotto-backup.service"
sed -i "s|/opt/kraft-lotto|${DEPLOY_DIR}|g" "${SYSTEMD_DIR}/kraft-lotto-restore-drill.service"

# 6. timer 활성화
systemctl daemon-reload
systemctl enable --now kraft-lotto-backup.timer
systemctl enable --now kraft-lotto-restore-drill.timer

echo ""
echo "=== 설치 완료 ==="
systemctl status kraft-lotto-backup.timer        --no-pager
systemctl status kraft-lotto-restore-drill.timer --no-pager
echo ""
echo "다음 실행 예정:"
systemctl list-timers kraft-lotto-backup.timer kraft-lotto-restore-drill.timer --no-pager
