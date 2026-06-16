# GitHub Secrets 설정 가이드

## 등록 위치

**Repository secrets** (CI/CD 모두 사용):  
https://github.com/portuna85/Kraft/settings/secrets/actions → **New repository secret**

> Environment secrets (production)는 CD 워크플로우에서 읽을 수 있지만,  
> CI의 docker-publish 단계는 environment 없이 실행되므로 **repository secrets에 반드시 등록**해야 한다.

---

## 전체 Secrets 목록

### 서버 SSH 접속

| 이름 | 값 | 설명 |
|------|-----|------|
| `DEPLOY_HOST` | `49.143.105.191` | 서버 공인 IP |
| `DEPLOY_USER` | `kraft` | SSH 접속 유저 |
| `DEPLOY_PORT` | `22` | SSH 포트 |
| `DEPLOY_SSH_KEY` | 개인키 전체 내용 | `-----BEGIN OPENSSH PRIVATE KEY-----` 포함 |

SSH 키 생성 및 등록 방법:
```bash
# 서버에서 실행
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/kraft_deploy -N ""
cat ~/.ssh/kraft_deploy.pub >> ~/.ssh/authorized_keys
cat ~/.ssh/kraft_deploy   # 이 내용을 DEPLOY_SSH_KEY에 붙여넣기
```

---

### GHCR (이미지 레지스트리)

| 이름 | 값 | 설명 |
|------|-----|------|
| `GHCR_TOKEN` | GitHub PAT | `read:packages` 권한 필요 |

PAT 발급:  
GitHub → 우측 상단 프로필 → Settings → Developer settings  
→ Personal access tokens → Tokens (classic) → Generate new token  
→ 이름: `kraft-ghcr` / 권한: `read:packages` → Generate

---

### 데이터베이스

| 이름 | 값 | 설명 |
|------|-----|------|
| `MARIADB_ROOT_PASSWORD` | 랜덤 문자열 | MariaDB root 비밀번호 |
| `MARIADB_PASSWORD` | 랜덤 문자열 | `kraft_lotto` 유저 비밀번호 |
| `KRAFT_DB_URL` | 아래 참고 | JDBC 연결 문자열 |
| `KRAFT_DB_USERNAME` | `kraft_lotto` | DB 유저명 |
| `KRAFT_DB_PASSWORD` | `MARIADB_PASSWORD`와 동일 | DB 비밀번호 |

`KRAFT_DB_URL` 값:
```
jdbc:mariadb://mariadb:3306/kraft_lotto?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

---

### 애플리케이션

| 이름 | 값 | 설명 |
|------|-----|------|
| `KRAFT_OPS_TOKEN` | `openssl rand -hex 32` 결과 | Ops 대시보드 인증 토큰 |
| `KRAFT_REVALIDATE_SECRET` | `openssl rand -hex 32` 결과 | Next.js ISR 재검증 시크릿 |
| `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE` | (빈 값 가능) | 외부 로또 API URL 템플릿 |
| `KRAFT_ADMIN_BOOTSTRAP_PASSWORD` | 강력한 비밀번호 | 관리자 초기 계정 비밀번호 |

---

### 도메인 / 네트워크

| 이름 | 값 | 설명 |
|------|-----|------|
| `KRAFT_DOMAIN` | `kraft.io.kr` | 공개 도메인 |
| `KRAFT_ADMIN_DOMAIN` | `admin.kraft.io.kr` | 관리자 도메인 |
| `KRAFT_PUBLIC_BASE_URL` | `https://kraft.io.kr` | 공개 URL (https 포함) |
| `KRAFT_SECURITY_TRUSTED_PROXY_CIDR` | `172.28.0.0/16` | Docker 내부망 CIDR |

---

### 모니터링

| 이름 | 값 | 설명 |
|------|-----|------|
| `GRAFANA_ADMIN_PASSWORD` | 강력한 비밀번호 | Grafana 관리자 비밀번호 |
| `ALERTMANAGER_SLACK_WEBHOOK_URL` | Slack Webhook URL 또는 빈 값 | Slack 알림 Webhook |

---

## 현재 등록 상태 체크리스트

```
Repository secrets (https://github.com/portuna85/Kraft/settings/secrets/actions)
─────────────────────────────────────────────────────────────────────────────────
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
[ ] KRAFT_ADMIN_BOOTSTRAP_PASSWORD
[ ] KRAFT_DOMAIN
[ ] KRAFT_ADMIN_DOMAIN
[ ] KRAFT_PUBLIC_BASE_URL
[ ] KRAFT_SECURITY_TRUSTED_PROXY_CIDR
[ ] GRAFANA_ADMIN_PASSWORD
[ ] ALERTMANAGER_SLACK_WEBHOOK_URL
```

> **Environment secrets (production)** 는 삭제해도 무방하다.  
> CD 워크플로우가 `environment: production` 을 사용하므로 읽을 수 있지만,  
> repository secrets로 통일하는 것이 관리가 단순하다.

---

## 랜덤 값 한번에 생성

서버 또는 로컬 터미널에서:

```bash
echo "MARIADB_ROOT_PASSWORD: $(openssl rand -base64 24)"
echo "MARIADB_PASSWORD:      $(openssl rand -base64 24)"
echo "KRAFT_OPS_TOKEN:       $(openssl rand -hex 32)"
echo "KRAFT_REVALIDATE_SECRET: $(openssl rand -hex 32)"
echo "KRAFT_ADMIN_BOOTSTRAP_PASSWORD: $(openssl rand -base64 16)"
echo "GRAFANA_ADMIN_PASSWORD: $(openssl rand -base64 16)"
```

> `MARIADB_PASSWORD` 와 `KRAFT_DB_PASSWORD` 는 반드시 동일한 값 사용.
