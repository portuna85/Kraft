# 운영 가이드

## 서비스 상태 확인

```bash
cd /srv/kraft

# 전체 서비스 상태
docker compose -f docker-compose.prod.yml ps

# 실시간 로그 (전체)
docker compose -f docker-compose.prod.yml logs -f

# 특정 서비스 로그
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f web
docker compose -f docker-compose.prod.yml logs -f caddy
```

---

## 접속 URL

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | https://kraft.io.kr |
| 관리자 | https://admin.kraft.io.kr/admin |
| Grafana 대시보드 | https://admin.kraft.io.kr/grafana |

---

## 모니터링 (Grafana)

1. https://admin.kraft.io.kr/grafana 접속
2. 로그인: `admin` / `GRAFANA_ADMIN_PASSWORD` (GitHub Secret 값)
3. 주요 대시보드:
   - **Kraft Backend** — API 응답시간, 에러율, DB 커넥션
   - **Node Exporter** — 서버 CPU, 메모리, 디스크

---

## 알림 (Alertmanager)

`ALERTMANAGER_SLACK_WEBHOOK_URL` 이 설정된 경우 아래 상황에서 Slack 알림 발송:

- 서비스 다운 (healthcheck 실패)
- 응답 시간 임계치 초과
- 디스크 사용률 80% 초과

---

## DB 백업

```bash
# 수동 백업
bash /srv/kraft/scripts/db-backup.sh

# 백업 파일 위치 확인
ls -lh /srv/kraft/backups/
```

---

## DB 복구

```bash
# 복구 드릴 (실제 복구 전 확인)
bash /srv/kraft/scripts/db-restore-drill.sh <백업파일>

# 실제 복구
bash /srv/kraft/scripts/db-restore.sh <백업파일>
```

---

## 서비스 재시작

```bash
cd /srv/kraft

# 특정 서비스만 재시작
docker compose -f docker-compose.prod.yml restart backend
docker compose -f docker-compose.prod.yml restart web

# 전체 재시작 (DB 제외)
docker compose -f docker-compose.prod.yml up -d --no-deps backend web caddy
```

---

## 서버 재부팅 후 자동 시작

모든 컨테이너는 `restart: unless-stopped` 로 설정되어 있어  
서버 재부팅 후 Docker가 시작되면 자동으로 실행된다.

확인:
```bash
docker compose -f /srv/kraft/docker-compose.prod.yml ps
```

---

## 디스크 정리

```bash
# 사용하지 않는 이미지/컨테이너/볼륨 정리
docker system prune -f

# 이미지만 정리 (실행 중 컨테이너가 사용하는 이미지는 보존)
docker image prune -f

# 현재 디스크 사용량
docker system df
df -h
```

---

## SSL 인증서

Caddy가 Let's Encrypt에서 자동 발급 및 갱신한다.  
별도 작업 불필요. 만료 30일 전 자동 갱신.

인증서 상태 확인:
```bash
docker compose -f docker-compose.prod.yml exec caddy caddy list-certificates
```

---

## 긴급 전체 중지

```bash
cd /srv/kraft
docker compose -f docker-compose.prod.yml down
```

> **주의**: `down -v` 는 DB 볼륨까지 삭제하므로 절대 사용 금지.

---

## 트러블슈팅

### 502 Bad Gateway

Caddy는 떠 있지만 backend/web 헬스체크 실패 시 발생.

```bash
# 백엔드 상태 직접 확인
curl -s http://localhost:8080/actuator/health

# 로그 확인
docker compose -f docker-compose.prod.yml logs --tail=50 backend
```

### DB 연결 실패

```bash
# MariaDB 상태
docker compose -f docker-compose.prod.yml ps mariadb

# MariaDB 직접 접속
docker compose -f docker-compose.prod.yml exec mariadb \
  mariadb -u kraft_lotto -p kraft_lotto
```

### 배포 후 변경사항이 반영 안 됨

Next.js ISR 캐시 수동 재검증:

```bash
curl -X POST "https://kraft.io.kr/api/revalidate?secret=<KRAFT_REVALIDATE_SECRET>"
```
