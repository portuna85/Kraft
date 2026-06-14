# GitHub Secrets 등록 가이드

리포지토리: `https://github.com/portuna85/Kraft`  
경로: Settings → Secrets and variables → Actions → New repository secret

---

## 서버 접속 (SSH)

| Secret 이름 | 값 | 비고 |
|------------|---|------|
| `DEPLOY_HOST` | 서버 IP (예: `1.2.3.4`) | |
| `DEPLOY_USER` | `deploy` | init-ubuntu.sh 기본값 |
| `DEPLOY_PORT` | `22` | |
| `DEPLOY_SSH_KEY` | ed25519 개인키 전체 내용 | `-----BEGIN OPENSSH PRIVATE KEY-----` 포함 |

### SSH 키 생성 (로컬 PC)

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/kraft_deploy
# 공개키 → 서버 authorized_keys에 추가
cat ~/.ssh/kraft_deploy.pub | ssh deploy@<서버IP> "cat >> ~/.ssh/authorized_keys"
# 개인키 내용 → DEPLOY_SSH_KEY Secret에 붙여넣기
cat ~/.ssh/kraft_deploy
```

---

## GHCR (컨테이너 이미지 Pull)

| Secret 이름 | 값 | 비고 |
|------------|---|------|
| `GHCR_TOKEN` | GitHub Personal Access Token | `read:packages` 권한 필요 |

### PAT 생성
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)  
스코프: `read:packages`

---

## 데이터베이스

| Secret 이름 | 값 | 비고 |
|------------|---|------|
| `MARIADB_ROOT_PASSWORD` | MariaDB root 비밀번호 | 강력한 랜덤 문자열 |
| `MARIADB_PASSWORD` | `kraft_lotto` 유저 비밀번호 | |
| `KRAFT_DB_URL` | `jdbc:mariadb://mariadb:3306/kraft_lotto?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul` | |
| `KRAFT_DB_USERNAME` | `kraft_lotto` | |
| `KRAFT_DB_PASSWORD` | MariaDB 유저 비밀번호 (위와 동일) | |

---

## 애플리케이션

| Secret 이름 | 값 | 비고 |
|------------|---|------|
| `KRAFT_OPS_TOKEN` | 랜덤 토큰 (32자+) | ops 대시보드 인증 |
| `KRAFT_REVALIDATE_SECRET` | 랜덤 토큰 (32자+) | ISR on-demand 인증 |
| `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE` | 외부 로또 API URL 템플릿 | `{round}` 플레이스홀더 포함 |

### 랜덤 토큰 생성
```bash
openssl rand -hex 32
```

---

## 도메인 / 네트워크

| Secret 이름 | 값 | 예시 |
|------------|---|------|
| `KRAFT_DOMAIN` | 공개 도메인 | `lotto.example.com` |
| `KRAFT_ADMIN_DOMAIN` | 관리자 도메인 | `admin.lotto.example.com` |
| `KRAFT_PUBLIC_BASE_URL` | 공개 URL (https 포함) | `https://lotto.example.com` |
| `KRAFT_SECURITY_TRUSTED_PROXY_CIDR` | Docker 내부망 CIDR | `172.28.0.0/16` |

---

## 모니터링

| Secret 이름 | 값 | 비고 |
|------------|---|------|
| `GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 | |
| `ALERTMANAGER_SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL | 알림 없으면 빈 값 가능 |

---

## 등록 완료 체크리스트

```
[ ] DEPLOY_HOST
[ ] DEPLOY_USER
[ ] DEPLOY_PORT
[ ] DEPLOY_SSH_KEY
[ ] GHCR_TOKEN
[ ] MARIADB_ROOT_PASSWORD
[ ] MARIADB_PASSWORD
[ ] KRAFT_DB_URL
[ ] KRAFT_DB_USERNAME
[ ] KRAFT_DB_PASSWORD
[ ] KRAFT_OPS_TOKEN
[ ] KRAFT_REVALIDATE_SECRET
[ ] KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE
[ ] KRAFT_DOMAIN
[ ] KRAFT_ADMIN_DOMAIN
[ ] KRAFT_PUBLIC_BASE_URL
[ ] KRAFT_SECURITY_TRUSTED_PROXY_CIDR
[ ] GRAFANA_ADMIN_PASSWORD
[ ] ALERTMANAGER_SLACK_WEBHOOK_URL  (선택)
```

모든 Secret 등록 후 `main` 브랜치에 커밋을 push하면 CI → CD 파이프라인이 자동 실행됩니다.
