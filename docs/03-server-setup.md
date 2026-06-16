# 서버 초기화 (Ubuntu 24.04)

홈서버 또는 VPS 최초 세팅 가이드.

## 사전 조건

- Ubuntu Server 24.04 LTS
- 공인 IP: `49.143.105.191`
- 도메인 DNS A 레코드 등록 완료 (→ [도메인 설정](#도메인--dns))
- 공유기 포트포워딩: 80, 443 → 서버 내부 IP

---

## 1. 서버 초기화 스크립트 실행

```bash
sudo bash ~/apps/kraft/scripts/server/init-ubuntu.sh kraft https://github.com/portuna85/Kraft.git
```

스크립트가 자동으로 처리하는 항목:

| 단계 | 내용 |
|------|------|
| 1/8 | 시스템 패키지 업데이트 |
| 2/8 | Docker Engine 설치 |
| 3/8 | `kraft` 유저 생성 + docker 그룹 추가 |
| 4/8 | `/srv/kraft` 에 코드 클론 |
| 5/8 | UFW 방화벽 설정 (22/80/443만 허용) |
| 6/8 | fail2ban 활성화 (SSH 브루트포스 차단) |
| 7/8 | 스왑 메모리 2GB 추가 (RAM ≤ 2GB 시) |
| 8/8 | 로그 디렉토리 생성 |

> **주의**: 스크립트는 `/srv/kraft` 에 코드를 클론한다.  
> 기존에 `~/apps/kraft` 에 클론했다면 삭제 후 실행하거나 `/srv/kraft` 를 사용한다.

---

## 2. GitHub Actions SSH 키 생성

GitHub Actions가 서버에 접속할 전용 키를 만든다.

```bash
# 키 생성
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/kraft_deploy -N ""

# 서버가 이 키를 허용하도록 등록
cat ~/.ssh/kraft_deploy.pub >> ~/.ssh/authorized_keys

# 개인키 출력 → GitHub Secrets의 DEPLOY_SSH_KEY 값으로 사용
cat ~/.ssh/kraft_deploy
```

---

## 3. 방화벽 확인

```bash
sudo ufw status
```

정상 출력:
```
Status: active

To                         Action      From
--                         ------      ----
22/tcp                     ALLOW IN    Anywhere    # SSH
80/tcp                     ALLOW IN    Anywhere    # HTTP
443/tcp                    ALLOW IN    Anywhere    # HTTPS
```

---

## 도메인 / DNS

가비아(gabia.com) 기준:  
**My가비아 → 서비스 관리 → 도메인 관리 → kraft.io.kr → DNS 관리**

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | `@` | `49.143.105.191` | 300 |
| A | `www` | `49.143.105.191` | 300 |
| A | `admin` | `49.143.105.191` | 300 |

전파 확인:
```bash
nslookup kraft.io.kr
nslookup admin.kraft.io.kr
```

---

## 서버 디렉토리 구조

```
/srv/kraft/          # 프로젝트 루트 (배포 위치)
├── .env.prod        # 프로덕션 환경변수 (CD가 자동 생성, gitignore)
├── docker-compose.prod.yml
├── caddy/Caddyfile
├── infra/
│   ├── prometheus/
│   ├── grafana/
│   └── alertmanager/
├── scripts/deploy/  # 배포 스크립트
└── logs/            # 애플리케이션 로그

~/.ssh/
├── authorized_keys  # 허용된 공개키 목록
└── kraft_deploy     # GitHub Actions 전용 개인키 (로컬 보관)
```

---

## 유지 관리 명령어

```bash
# 실행 중인 컨테이너 확인
docker compose -f /srv/kraft/docker-compose.prod.yml ps

# 전체 로그 실시간 확인
docker compose -f /srv/kraft/docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker compose -f /srv/kraft/docker-compose.prod.yml logs -f backend

# 서버 리소스 확인
htop

# Docker 디스크 사용량 확인
docker system df
```
