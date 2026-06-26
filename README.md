# KRAFT Lotto

KRAFT Lotto는 로또 6/45 당첨 결과 조회, 번호 추천, 통계 분석, 저장 번호 관리, 운영 대시보드까지 포함한 웹 서비스입니다. 백엔드는 Spring Boot, 프론트엔드는 Next.js App Router로 구성되어 있고, 운영 환경은 Docker Compose, Caddy, MariaDB, Prometheus, Grafana, Alertmanager를 기준으로 배포됩니다.

[![Live](https://img.shields.io/badge/Live-kraft.io.kr-2348d8?style=flat-square)](https://kraft.io.kr/)
[![CI](https://github.com/portuna85/Kraft/actions/workflows/ci.yml/badge.svg)](https://github.com/portuna85/Kraft/actions/workflows/ci.yml)
[![CodeQL](https://github.com/portuna85/Kraft/actions/workflows/codeql.yml/badge.svg)](https://github.com/portuna85/Kraft/actions/workflows/codeql.yml)

## 핵심 기능

| 영역 | 기능 |
| --- | --- |
| 당첨 결과 | 최신 회차, 회차 목록, 특정 회차 상세, 당첨금/판매액 조회 |
| 번호 추천 | 제외 번호와 추천 개수 설정, 과거 1등 조합 중복 확인, 저장 연계 |
| 저장 번호 | 브라우저 기기 토큰 기반 저장/조회/삭제, 서버에는 토큰 해시만 보관 |
| 통계 | 번호별 출현 빈도, 홀짝/고저/합계 패턴, 동반 출현 조합, 임의 조합 분석 |
| 상태 페이지 | 데이터 최신성, 최근 30일 수집/보정 이력 공개 |
| 운영 | 회차 직접 입력, 최신/특정 회차 외부 수집, 운영 로그 조회 |
| 관리자 | Thymeleaf 기반 관리자 로그인, 회차 수집, 전체 백필, 감사 로그 |
| 배포/운영 | Caddy 라우팅, TLS, 내부망 분리, 모니터링, 알림, DB 백업/복구 드릴 |

> 번호 추천은 통계 기반 참고 기능입니다. 당첨을 보장하지 않습니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1, Spring Web, Validation, Data JPA, Security, Actuator, Thymeleaf |
| Data | MariaDB 11.7, Flyway, H2(local/test), Caffeine cache |
| Batch/Resilience | ShedLock, Resilience4j CircuitBreaker, Spring Scheduler, Async event listener |
| Frontend | Next.js 16 App Router, React 19, TypeScript, CSS, Server Components, ISR |
| Test | JUnit 5, Spring Test, Testcontainers MariaDB, JaCoCo, Vitest, Testing Library, Playwright |
| Static/Security | Checkstyle, SpotBugs, CodeQL, Trivy, Dependabot |
| Infra | Docker Compose, Caddy, Prometheus, Grafana, Alertmanager, node-exporter, GHCR |

## 아키텍처

```text
Browser
  |
  | HTTPS
  v
Caddy
  |-- public domain
  |     |-- /api/v1/*      -> Spring Boot backend:8080
  |     |-- /*             -> Next.js web:3000
  |     `-- /admin*, /ops*, /actuator*, /api/revalidate 차단
  |
  `-- admin domain
        |-- /admin*        -> Spring Boot Thymeleaf admin
        |-- /ops-api/*     -> Spring Boot /ops/*
        |-- /grafana/*     -> Grafana
        `-- IP allowlist 적용 가능

Spring Boot
  |-- MariaDB app network
  |-- external lotto API, optional
  |-- Prometheus metrics
  `-- Next.js /api/revalidate webhook

Next.js
  |-- SSR/ISR public pages
  |-- local/E2E API route handler fallback
  `-- CSP nonce middleware
```

운영용 `docker-compose.prod.yml`은 `edge`, `app`, `monitoring` 네트워크를 분리합니다. MariaDB, Prometheus, Alertmanager, node-exporter는 외부 포트를 열지 않고 내부 네트워크에서만 접근합니다.

## 저장소 구조

```text
Kraft/
|-- src/main/java/com/kraft/
|   |-- admin/          관리자 SSR, 로그인, 잠금, 감사 로그
|   |-- common/         공통 설정, 보안 필터, 오류 응답, 로또 번호 codec
|   |-- ops/            운영 API와 운영 요약
|   |-- operationlog/   수집/보정 작업 로그와 공개 상태 API
|   |-- recommend/      번호 추천과 과거 1등 조합 검사
|   |-- saved/          기기 토큰 기반 저장 번호
|   |-- statistics/     빈도, 패턴, 동반 출현, 조합 분석
|   `-- winningnumber/  회차 조회, 외부 수집, 백필, 최신성, ISR 이벤트
|-- src/main/resources/
|   |-- db/migration/   Flyway SQL
|   |-- templates/admin/관리자 Thymeleaf 템플릿
|   `-- application*.yml
|-- src/test/           백엔드 단위/통합 테스트
|-- web/
|   |-- src/app/        Next.js 페이지와 Route Handler
|   |-- src/components/ UI 컴포넌트
|   |-- src/lib/        API, 포맷, 검증, 토큰, 로깅 유틸
|   |-- src/__tests__/  Vitest 테스트
|   `-- e2e/            Playwright 테스트
|-- caddy/              Caddy 라우팅과 보안 헤더
|-- infra/              Prometheus, Grafana, Alertmanager
|-- scripts/            배포, 백업/복구, 마이그레이션, 서버 초기화
|-- .github/            CI, CD, CodeQL, Dependabot
`-- docker-compose*.yml
```

## 빠른 시작

### 요구 사항

- JDK 25
- Node.js 24 이상
- npm
- Docker Desktop 또는 Docker Engine, MariaDB를 함께 실행할 때 필요

### 1. 백엔드만 바로 실행, H2 메모리 DB

가장 빠른 로컬 실행 방식입니다. MariaDB 없이 Spring Boot가 H2 메모리 DB로 시작합니다.

```powershell
copy .env.local.example .env.local
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

접속:

| 항목 | URL |
| --- | --- |
| Backend API | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| H2 Console | http://localhost:8080/h2-console |
| Admin Login | http://localhost:8080/admin/login |

`.env.local.example`의 기본 관리자 계정은 로컬 개발용 `admin / admin`입니다. 실제 환경에서는 사용하지 마세요.

### 2. 프론트엔드 실행

Next.js 서버 컴포넌트가 백엔드를 직접 호출하므로 로컬에서는 `KRAFT_BACKEND_INTERNAL_URL`을 `localhost:8080`으로 맞춥니다.

```powershell
cd web
copy .env.example .env.local
```

`web/.env.local`에서 아래 값을 로컬용으로 수정합니다.

```properties
KRAFT_BACKEND_INTERNAL_URL=http://localhost:8080
KRAFT_PUBLIC_BASE_URL=http://localhost:3000
```

실행:

```powershell
npm ci
npm run dev
```

접속:

| 항목 | URL |
| --- | --- |
| Web | http://localhost:3000 |
| Ops Page | http://localhost:3000/ops |

### 3. MariaDB 개발 DB만 실행

H2 대신 MariaDB로 개발하려면 DB 컨테이너를 먼저 띄우고 `.env.local`의 DB 주석을 해제합니다.

```powershell
docker compose -f docker-compose.dev.yml up -d
```

`.env.local` 예시:

```properties
KRAFT_DB_URL=jdbc:mariadb://localhost:3306/kraft_lotto?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
KRAFT_DB_USERNAME=kraft_lotto
KRAFT_DB_PASSWORD=devpass
KRAFT_FLYWAY_ENABLED=true
KRAFT_JPA_DDL_AUTO=validate
```

### 4. Docker로 전체 로컬 스택 실행

백엔드, 웹, MariaDB, Prometheus, Grafana, Alertmanager, node-exporter를 한 번에 실행합니다.

```powershell
copy .env.example .env
```

최소한 아래 값은 `.env`에 채웁니다.

```properties
MARIADB_ROOT_PASSWORD=devroot
MARIADB_PASSWORD=devpass
KRAFT_DB_PASSWORD=devpass
KRAFT_OPS_TOKEN=local-dev-ops-token
KRAFT_PUBLIC_BASE_URL=http://localhost
GRAFANA_ADMIN_PASSWORD=admin
```

실행:

```powershell
docker compose up -d --build
```

접속:

| 항목 | URL |
| --- | --- |
| Web | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| MariaDB | localhost:3306 |

## 환경 파일

| 파일 | 용도 |
| --- | --- |
| `.env.local.example` | Spring Boot를 로컬에서 직접 실행할 때 읽는 값 |
| `.env.example` | 로컬 Docker Compose용 템플릿 |
| `.env.prod.example` | CD에서 GitHub Secrets로 렌더링하는 운영 템플릿 |
| `web/.env.example` | Next.js 로컬/빌드 환경 변수 템플릿 |

주요 변수:

| 변수 | 설명 |
| --- | --- |
| `KRAFT_DB_URL`, `KRAFT_DB_USERNAME`, `KRAFT_DB_PASSWORD` | Spring Boot DB 연결 |
| `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE` | 외부 회차 수집 URL. `{round}` 플레이스홀더 필요 |
| `KRAFT_EXTERNAL_LOTTO_AUTO_COLLECT_CRON` | 자동 수집 cron. 기본 `0 30 21 * * SAT` |
| `KRAFT_OPS_TOKEN` | `/ops/*` API 인증용 `X-Ops-Token` 값 |
| `KRAFT_REVALIDATE_SECRET` | 백엔드와 Next.js `/api/revalidate` 간 공유 secret |
| `KRAFT_REVALIDATE_WEB_URL` | 백엔드가 revalidation을 호출할 웹 서버 URL |
| `KRAFT_BACKEND_INTERNAL_URL` | Next.js 서버에서 호출할 백엔드 내부 URL |
| `KRAFT_PUBLIC_BASE_URL` | canonical URL, sitemap, OG 태그 기준 URL |
| `KRAFT_OPS_ALLOWED_HOST` | `/ops` 프론트 페이지 접근 허용 호스트 |
| `KRAFT_ADMIN_ALLOWED_CIDR` | Caddy 관리자 도메인 IP allowlist |
| `KRAFT_SECURITY_TRUSTED_PROXY_CIDR` | Actuator/프록시 신뢰 CIDR |
| `KRAFT_SECURITY_RATE_LIMIT_PER_MINUTE` | 공개 API/IP별 분당 제한 |
| `KRAFT_SAVED_MAX_PER_CLIENT` | 기기 토큰별 저장 번호 최대 개수 |
| `KRAFT_ADMIN_BOOTSTRAP_USERNAME`, `KRAFT_ADMIN_BOOTSTRAP_PASSWORD` | 관리자 최초 계정 생성 |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 |

외부 수집 URL이 비어 있으면 수집 API와 자동 수집 스케줄러는 비활성 상태로 동작합니다.

## 공개 화면

| 경로 | 설명 | 캐시 |
| --- | --- | --- |
| `/` | 최신 당첨 결과와 주요 기능 진입점 | ISR 60초 |
| `/rounds` | 최신 결과, 회차 목록, 회차 검색 | ISR 60초 |
| `/rounds/[round]` | 특정 회차 상세와 조합 분석 | ISR 1시간 |
| `/recommend` | 번호 추천, 제외 번호, 저장 연계 | 동적 클라이언트 UI |
| `/saved` | 기기 토큰 기반 저장 번호 관리 | noindex |
| `/frequency` | 번호별 출현 빈도 | ISR 30분 |
| `/stats` | 홀짝, 고저, 합계 패턴 통계 | ISR 30분 |
| `/companion` | 동반 출현 번호쌍 통계 | ISR 30분 |
| `/analysis` | 임의 번호 6개 조합 분석 | 동적 클라이언트 UI |
| `/status` | 데이터 최신성, 수집/보정 이력 | ISR 60초, noindex |
| `/ops` | 운영 대시보드. `/ops-api/*` 호출 | 호스트 제한 가능 |

## API 개요

### Public API

기본 경로는 `/api/v1`입니다.

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/rounds/latest` | 최신 당첨 회차 |
| `GET` | `/rounds/freshness` | 최신 데이터 반영 상태 |
| `GET` | `/rounds?page=0&size=20` | 회차 목록. `size`는 1-100 |
| `GET` | `/rounds/{round}` | 특정 회차 상세 |
| `POST` | `/numbers/recommend` | 추천 조합 생성 |
| `GET` | `/numbers/check?numbers=1,2,3,4,5,6` | 과거 1등 조합 여부 |
| `GET` | `/stats/frequency?limit=100` | 번호 빈도. `limit`은 100, 200, 500만 허용 |
| `GET` | `/stats/patterns` | 홀짝, 고저, 합계 패턴 통계 |
| `GET` | `/stats/companion` | 동반 출현 번호쌍 |
| `POST` | `/stats/analysis` | 번호 6개 조합 분석 |
| `GET` | `/saved` | 저장 번호 목록. `X-Device-Token` 필요 |
| `POST` | `/saved` | 번호 저장. `X-Device-Token` 필요 |
| `DELETE` | `/saved/{id}` | 저장 번호 삭제. `X-Device-Token` 필요 |
| `GET` | `/status/incidents` | 최근 공개 수집/보정 이력 |
| `GET` | `/status` | 서비스 상태 요약 |

예시:

```bash
curl http://localhost:8080/api/v1/rounds/latest

curl -X POST http://localhost:8080/api/v1/numbers/recommend \
  -H "Content-Type: application/json" \
  -d '{"count":5,"excludedNumbers":[1,2,3],"maximizePrize":true}'

curl -X POST http://localhost:8080/api/v1/saved \
  -H "Content-Type: application/json" \
  -H "X-Device-Token: 0123456789abcdef0123456789abcdef" \
  -d '{"numbers":[3,11,19,28,34,42],"label":"주말","source":"MANUAL"}'
```

저장 번호의 `X-Device-Token`은 32-128자 문자열이어야 하며, 서버에는 SHA-256 해시만 저장됩니다.

### Ops API

기본 경로는 `/ops`이고 모든 요청에 `X-Ops-Token`이 필요합니다. 토큰이 비어 있으면 API는 비활성 상태로 응답합니다.

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `GET` | `/summary` | 운영 요약과 데이터 최신성 |
| `GET` | `/logs` | 수집/보정 로그 목록. 필터: `page`, `size`, `operationType`, `executionStatus`, `round`, `from`, `to` |
| `POST` | `/rounds` | 회차 직접 입력 또는 갱신 |
| `POST` | `/collect/latest` | 다음 최신 회차 수집 |
| `POST` | `/collect/{round}` | 특정 회차 외부 수집 |

프론트 운영 페이지는 `/ops-api/*`를 호출합니다. Next.js 개발 서버는 rewrite로, 운영 Caddy는 `handle_path /ops-api/*`로 백엔드 `/ops/*`에 전달합니다.

### Admin UI

| 경로 | 설명 |
| --- | --- |
| `/admin/login` | 관리자 로그인 |
| `/admin/dashboard` | 관리자 대시보드 |
| `/admin/rounds` | 회차 수집, 전체 백필 |
| `/admin/audit` | 관리자 감사 로그 |

관리자 UI는 Spring Security 세션, CSRF, 로그인 실패 잠금, 감사 로그를 사용합니다. 운영 공개 도메인에서는 Caddy가 `/admin*`을 차단하고 관리자 도메인에서만 접근하도록 구성합니다.

## 데이터 모델

| 테이블 | 역할 |
| --- | --- |
| `winning_numbers` | 회차, 추첨일, 6개 번호, 보너스 번호, 1/2등 상금, 판매액 |
| `saved_numbers` | 기기 토큰 해시별 저장 번호 |
| `winning_number_operation_logs` | 외부 수집/수동 보정/백필 작업 이력 |
| `winning_number_frequency_summary` | 번호별 출현 빈도 요약 |
| `pattern_stats_summary` | 홀짝, 고저, 합계 구간 통계 요약 |
| `companion_pair_summary` | 동반 출현 번호쌍 요약 |
| `admin_users` | 관리자 계정 |
| `admin_audit_log` | 관리자 행동 감사 로그 |
| `shedlock` | 스케줄러 중복 실행 방지 락 |

Flyway 마이그레이션은 `src/main/resources/db/migration`에서 관리합니다.

## 수집과 갱신 흐름

1. `WinningNumberAutoCollectScheduler`가 토요일 21:30 KST와 일요일 07:00 KST에 최신 회차 catch-up을 시도합니다.
2. 외부 수집 URL은 `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE`의 `{round}`를 회차 번호로 치환해 호출합니다.
3. 수집 성공 시 회차별 upsert는 트랜잭션으로 처리하고, 작업 로그는 별도 트랜잭션으로 기록합니다.
4. 커밋 이후 `StatisticsRefreshListener`가 통계 요약 테이블을 갱신합니다.
5. 커밋 이후 `RevalidateWebhookListener`가 Next.js `/api/revalidate`를 호출해 `/`, `/rounds`, `/frequency`, `/stats`, `/companion`, `/rounds/{round}`를 재검증합니다.
6. ShedLock이 다중 인스턴스 환경에서 스케줄러 중복 실행을 막습니다.

전체 백필은 관리자 화면에서 비동기로 실행되며, 이미 저장된 회차 이후 첫 누락 회차부터 외부 API가 더 이상 데이터를 주지 않는 지점까지 순차 수집합니다.

## 테스트와 품질 게이트

### Backend

```powershell
.\gradlew.bat test bootJar
.\gradlew.bat check -PstrictCoverage=true -PstrictStatic=true
```

`strictCoverage=true`는 JaCoCo 커버리지 게이트를 켭니다. 현재 기준은 전체 라인 82%, 브랜치 65%, 메서드 88%, 클래스 97%이며, 핵심 패키지별 라인 커버리지 기준도 별도로 둡니다.

`strictStatic=true`는 Checkstyle 실패를 빌드 실패로 처리합니다. SpotBugs는 항상 실패를 차단합니다.

### Frontend

```powershell
cd web
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

### E2E

```powershell
cd web
npm run build
npm run test:e2e
```

Playwright는 `web/scripts/serve-standalone.mjs`로 Next.js standalone 산출물을 3100번 포트에 띄우고, 백엔드 미가용 상황에서도 주요 클라이언트 흐름과 fallback UI를 검증합니다.

### CI/CD

| 파일 | 역할 |
| --- | --- |
| `.github/workflows/ci.yml` | 백엔드 테스트/빌드, 웹 lint/type/test/build, Playwright, 정적 분석, Caddy 검증, 이미지 publish, Trivy, latest 승격 |
| `.github/workflows/pr.yml` | PR 의존성 취약점 스캔 |
| `.github/workflows/codeql.yml` | Java/Kotlin, JavaScript/TypeScript CodeQL 분석 |
| `.github/workflows/cd.yml` | CI 성공 후 SSH 배포, env 렌더링, image pull, readiness, smoke, 실패 시 rollback |
| `.github/dependabot.yml` | Gradle, npm, Docker, GitHub Actions 주간 업데이트 |

`scripts/check-no-removed-features.sh`는 제거된 Flutter, push/FCM, news 관련 코드가 재유입되지 않도록 CI에서 검사합니다.

## 운영 배포

운영 서버 초기화:

```bash
sudo bash scripts/server/init-ubuntu.sh
```

이 스크립트는 Ubuntu 24.04 기준으로 Docker, deploy 사용자, UFW, fail2ban, 저장소, 로그 디렉터리, DB 백업/복구 드릴 cron을 준비합니다.

운영 Compose 실행:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

로컬에서 운영 Compose를 시험하면서 포트를 열려면 `docker-compose.local.yml`을 함께 적용할 수 있습니다.

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml -f docker-compose.local.yml up -d
```

배포 스크립트:

| 파일 | 역할 |
| --- | --- |
| `scripts/deploy/validate-env.sh` | 필수 운영 환경 변수 검증 |
| `scripts/deploy/render-env.sh` | `.env.prod.example`을 `.env.prod`로 렌더링 |
| `scripts/deploy/render-alertmanager.sh` | Alertmanager 템플릿 렌더링 |
| `scripts/deploy/pull-and-up.sh` | 이미지 pull, Compose up, readiness/smoke |
| `scripts/deploy/smoke-test.sh` | 공개 API, 보안 차단 경로, redirect, 헤더 검증 |
| `scripts/deploy/rollback.sh` | 서비스 이미지를 이전 이미지로 rollback |
| `scripts/deploy/rollback-caddy.sh` | Caddyfile만 이전 커밋으로 rollback |
| `scripts/deploy/check-caddy-routes.sh` | Caddy 라우팅 로컬 검증 |

## 모니터링과 백업

Prometheus는 `/actuator/prometheus`, node-exporter, Caddy metrics를 수집합니다. Grafana는 Prometheus datasource를 provisioning하고, Alertmanager는 Discord webhook 템플릿을 사용할 수 있습니다.

주요 알림:

- Backend down
- 5xx 비율 5% 초과
- P95 응답 시간 2초 초과
- JVM heap 85% 초과
- 로또 데이터 최신성 지연
- 외부 회차 수집 반복 실패

DB 백업:

```bash
bash scripts/db-backup.sh
bash scripts/db-restore-drill.sh
bash scripts/db-restore.sh /var/backups/kraft/kraft_lotto_YYYYMMDD_HHMMSS.sql.gz
```

운영 MariaDB는 내부 `app` 네트워크에만 있으므로 백업/복구는 호스트 TCP 접속이 아니라 `docker compose exec mariadb` 경유로 실행됩니다. `BACKUP_REMOTE_DEST`와 rclone을 설정하면 원격 사본 업로드도 가능합니다.

## 보안 메모

- 공개 도메인 Caddy는 `/admin*`, `/ops*`, `/actuator*`, `/api/revalidate`를 차단합니다.
- 관리자 도메인은 `KRAFT_ADMIN_ALLOWED_CIDR`로 IP allowlist를 적용할 수 있습니다.
- 공개 API는 stateless이고 세션 쿠키를 사용하지 않습니다.
- 관리자 UI는 세션, CSRF, 단일 세션 제한, 로그인 실패 잠금을 적용합니다.
- 저장 번호는 원본 기기 토큰을 저장하지 않고 SHA-256 해시만 저장합니다.
- `/ops/*`는 `X-Ops-Token`으로 보호됩니다.
- 공개 API와 Ops API에는 Caffeine 기반 분당 rate limit이 적용됩니다.
- API 응답에는 보안 헤더와 캐시/ETag 정책을 적용합니다.
- Next.js는 요청별 CSP nonce를 생성합니다.
- 운영 컨테이너는 가능한 범위에서 non-root, `cap_drop: ALL`, `no-new-privileges`, read-only filesystem을 사용합니다.

## 문제 해결

| 증상 | 확인할 내용 |
| --- | --- |
| Next.js 화면에서 데이터를 못 불러옴 | `web/.env.local`의 `KRAFT_BACKEND_INTERNAL_URL=http://localhost:8080` 확인 |
| `/ops` 호출이 503 | `KRAFT_OPS_TOKEN`이 비어 있으면 Ops API가 비활성화됨 |
| 외부 수집이 503 | `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE`이 설정되어야 함 |
| Docker backend가 DB 연결 실패 | `.env`의 `MARIADB_PASSWORD`와 `KRAFT_DB_PASSWORD` 일치 확인 |
| 운영 공개 도메인에서 `/admin` 403 | 정상 동작. 관리자 도메인으로 접근 |
| H2 Console 접속 불가 | `local` 프로필인지, URL이 `/h2-console`인지 확인 |
