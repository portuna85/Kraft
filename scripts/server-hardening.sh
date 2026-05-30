#!/usr/bin/env bash
# 서버 보안 초기 설정 스크립트 (Ubuntu 서버 최초 세팅 시 실행)
set -euo pipefail

echo "=== SSH 보안 설정 ==="

# PasswordAuthentication no
sudo sed -i 's/^PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config.d/50-cloud-init.conf 2>/dev/null || true
grep -q '^PasswordAuthentication' /etc/ssh/sshd_config \
  || echo 'PasswordAuthentication no' | sudo tee -a /etc/ssh/sshd_config

# PermitRootLogin no
grep -q '^PermitRootLogin' /etc/ssh/sshd_config \
  || echo 'PermitRootLogin no' | sudo tee -a /etc/ssh/sshd_config

# MaxAuthTries 3
grep -q '^MaxAuthTries' /etc/ssh/sshd_config \
  || echo 'MaxAuthTries 3' | sudo tee -a /etc/ssh/sshd_config

sudo sshd -t && echo "SSH 설정 문법 OK"
sudo systemctl reload sshd

echo ""
echo "=== fail2ban 설치 및 설정 ==="

sudo apt-get install -y fail2ban

sudo tee /etc/fail2ban/jail.local << 'EOF'
[DEFAULT]
bantime  = 86400
findtime = 300
maxretry = 5
backend  = systemd

[sshd]
enabled = true
port    = ssh
EOF

sudo systemctl enable --now fail2ban

echo ""
echo "=== 최종 상태 확인 ==="
sudo grep -E '^(PasswordAuthentication|PermitRootLogin|MaxAuthTries)' /etc/ssh/sshd_config
sudo fail2ban-client status sshd
