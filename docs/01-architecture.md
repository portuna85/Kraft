# 아키텍처 개요

## 서비스 구성

```
인터넷
  │
  ▼
[Caddy]  — HTTPS 자동 발급, 리버스 프록시, 보안 헤더
  ├── kraft.io.kr          → Next.js (web:3000)
  ├── www.kraft.io.kr      → kraft.io.kr 리다이렉트
  └── admin.kraft.io.kr    → Spring Boot (backend:8080/admin)
                              └── /api/v1/* → backend:8080
                              └── /grafana/* → grafana:3000

[Next.js]  — 프론트엔드 (포트 3000, SSR + ISR)
  └── SSR/ISR 캐시 재검증 → backend:8080/api/v1/revalidate

[Spring Boot]  — 백엔드 API + 관리자 화면 (포트 8080)
  └── MariaDB (mariadb:3306)

[모니터링]
  ├── Prometheus  — 메트릭 수집 (backend, node-exporter, caddy)
  ├── Grafana     — 대시보드 (admin.kraft.io.kr/grafana)
  ├── Alertmanager — 알림 (Slack)
  └── Node Exporter — 서버 리소스 메트릭
```

## 기술 스택

| 계층 | 기술 | 버전 |
|------|------|------|
| 프론트엔드 | Next.js (App Router) | 16.x |
| 백엔드 | Spring Boot | 4.1.0 |
| 런타임 | Java (Temurin) | 25 |
| Node.js | LTS | 24 |
| DB | MariaDB | 11.7 |
| 리버스 프록시 | Caddy | 2 |
| 컨테이너 | Docker Compose | v2 |
| 이미지 레지스트리 | GHCR (GitHub Container Registry) | — |
| 모니터링 | Prometheus + Grafana | 3.4 / 11.6 |
| 알림 | Alertmanager | 0.28 |

## Docker 네트워크

모든 컨테이너는 `kraft-net` (subnet: `172.28.0.0/16`) 내부 네트워크로 통신한다.  
외부에 노출되는 포트는 Caddy의 80/443뿐이다.

## 환경 분리

| 환경 | 실행 방법 | DB | 비고 |
|------|-----------|-----|------|
| `local` | IntelliJ / `./gradlew bootRun` | H2 인메모리 | Docker 불필요 |
| `local` (MariaDB) | `docker-compose.dev.yml` + bootRun | MariaDB (Docker) | 핫리로드 |
| `prod` | GitHub Actions CI/CD | MariaDB (Docker) | GHCR 이미지 사용 |
