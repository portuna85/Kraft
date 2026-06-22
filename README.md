# Kraft Lotto

로또 6/45 당첨 결과 조회, 번호 추천, 통계 분석, 번호 저장, 운영 관리 기능을 제공하는 풀스택 서비스입니다. 저장소는 Spring Boot 백엔드, Next.js 프론트엔드, Docker Compose 기반 운영 스택, GitHub Actions CI/CD, Caddy 리버스 프록시, Prometheus/Grafana/Alertmanager 모니터링까지 함께 포함합니다.

## 핵심 기능

- 최신 회차, 특정 회차, 전체 회차 목록 조회
- 번호 추천과 과거 1등 조합 중복 여부 확인
- 번호 출현 빈도, 패턴 통계, 동반 출현 통계, 조합 분석
- 브라우저 단위 저장 번호 관리 (`X-Device-Token` 기반)
- 운영 대시보드에서 수동 적재, 최신 회차 수집, 작업 로그 조회
- 관리자 SSR 페이지(`Thymeleaf`)에서 회차 수집과 감사 로그 확인
- 자동 수집, Revalidate 연동, 보안 헤더, 공개 API rate limit, 운영 모니터링

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1, Spring Web, Validation, JPA, Security, Thymeleaf |
| Data | MariaDB 11.7, H2, Flyway, Caffeine |
| Infra/Resilience | Docker Compose, Caddy, Resilience4j, ShedLock |
| Frontend | Next.js 16 App Router, React 19, TypeScript |
| Test | JUnit 5, Spring Test, Testcontainers, Vitest, Testing Library |
| Observability | Spring Actuator, Prometheus, Grafana, Alertmanager, node-exporter |
| CI/CD | GitHub Actions, GHCR, SSH deploy |

## 저장소 구조

```text
Kraft/
├── src/main/java/com/kraft
│   ├── admin/           # 관리자 SSR, 로그인, 감사 로그
│   ├── common/          # 공통 설정, 에러 처리, 보안, 웹 유틸
│   ├── ops/             # 운영 API
│   ├── operationlog/    # 작업 로그
│   ├── recommend/       # 번호 추천
│   ├── saved/           # 저장 번호
│   ├── statistics/      # 빈도/패턴/동반/분석 통계
│   └── winningnumber/   # 회차 조회, 외부 수집, 백필
├── src/main/resources
│   ├── db/migration/    # Flyway 마이그레이션
│   └── templates/admin/ # 관리자 HTML 템플릿
├── src/test/java        # 백엔드 테스트
├── web/
│   ├── src/app/         # Next.js 페이지, Route Handler
│   ├── src/components/  # UI 컴포넌트
│   ├── src/lib/         # API/포맷/검증/로깅 유틸
│   └── src/__tests__/   # 프론트엔드 테스트
├── caddy/               # 공개/관리 도메인 라우팅
├── infra/               # Prometheus, Grafana, Alertmanager 설정
├── scripts/             # 배포, 마이그레이션, DB 백업/복구, 서버 초기화
└── .github/workflows/   # CI, CD, PR Check, CodeQL
```

## 애플리케이션 구성

### Backend

- 기본 포트: `8080`
- 프로필:
  - 기본 `local`
  - 운영 `prod`
- 로컬 기본 DB:
  - H2 인메모리
  - `KRAFT_FLYWAY_ENABLED=false` 기본값
- Docker/운영 DB:
  - MariaDB
  - Flyway 활성화
- 주요 보안/운영 특성:
  - `/api/**`, `/actuator/**`, `/ops/**` 는 stateless filter chain
  - `/actuator/health/**` 공개
  - `/actuator/prometheus` 는 trusted CIDR만 허용
  - 공개 API rate limit 설정 가능
  - 외부 당첨번호 수집에 circuit breaker 적용
  - 스케줄러 중복 실행 방지용 ShedLock 사용

### Frontend

- 기본 포트: `3000`
- Next.js App Router 기반 SSR/ISR 혼합
- 백엔드 내부 호출:
  - `KRAFT_BACKEND_INTERNAL_URL`
- 공개 기준 URL:
  - `KRAFT_PUBLIC_BASE_URL`
- 주요 페이지:
  - `/`
  - `/latest`
  - `/rounds`
  - `/rounds/[round]`
  - `/recommend`
  - `/saved`
  - `/frequency`
  - `/stats`
  - `/companion`
  - `/analysis`
  - `/ops`

### 운영 스택

- `docker-compose.dev.yml`
  - 로컬 개발용 MariaDB
- `docker-compose.yml`
  - 전체 스택 로컬/단일 서버 실행
- `docker-compose.prod.yml`
  - 운영 이미지 기반 실행
- Caddy
  - 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단
  - 관리자 도메인에서 `/admin*`, `/ops-api/*`, `/grafana/*` 허용

## 공개 API 개요

### Spring Boot API

- `GET /api/v1/rounds/latest`
- `GET /api/v1/rounds?page=0&size=20`
- `GET /api/v1/rounds/{round}`
- `POST /api/v1/numbers/recommend`
- `GET /api/v1/numbers/check?numbers=1&numbers=2...`
- `GET /api/v1/stats/frequency`
- `GET /api/v1/stats/patterns`
- `GET /api/v1/stats/companion`
- `POST /api/v1/stats/analysis`
- `GET /api/v1/saved`
- `POST /api/v1/saved`
- `DELETE /api/v1/saved/{id}`

**공식 진입점은 Caddy → `backend:8080` 직결**입니다(`caddy/Caddyfile`의 `handle /api/v1/* { reverse_proxy backend:8080 }`).
`web/src/app/api/v1/**`에도 동일 경로의 Next.js route handler가 존재하지만, 운영에서는 Caddy가 먼저 가로채므로
**`npm run dev`로 Next.js만 단독 실행할 때만** 쓰이는 폴백입니다(`backend-proxy.ts`가 `KRAFT_BACKEND_INTERNAL_URL`로 직접 프록시).
두 경로 모두 같은 백엔드를 호출하므로 계약 드리프트 위험은 낮지만, API 동작을 바꿀 때는 백엔드 컨트롤러만 수정하면 됩니다.

### 운영 API

- `GET /ops/summary`
- `GET /ops/logs`
- `POST /ops/rounds`
- `POST /ops/collect/latest`
- `POST /ops/collect/{round}`

운영 API는 `X-Ops-Token` 기반이며, 프론트엔드에서는 `/ops-api/*` 프록시 경로를 통해 호출합니다.

## 환경 파일

| 파일 | 용도 |
| --- | --- |
| `.env.local.example` | 로컬에서 Spring Boot를 직접 실행할 때 참고용 |
| `.env.example` | Docker Compose 개발/단일 서버 실행용 |
| `.env.prod.example` | 운영 배포용 |
| `src/main/resources/application.yml` | 공통 기본 설정 |
| `src/main/resources/application-local.yml` | 로컬 프로필 설정 |
| `src/main/resources/application-prod.yml` | 운영 프로필 설정 |

중요 환경 변수 예시:

- `KRAFT_DB_URL`
- `KRAFT_DB_USERNAME`
- `KRAFT_DB_PASSWORD`
- `KRAFT_PUBLIC_BASE_URL`
- `KRAFT_BACKEND_INTERNAL_URL`
- `KRAFT_REVALIDATE_SECRET`
- `KRAFT_OPS_TOKEN`
- `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE`
- `KRAFT_ADMIN_BOOTSTRAP_USERNAME`
- `KRAFT_ADMIN_BOOTSTRAP_PASSWORD`
- `KRAFT_SECURITY_TRUSTED_PROXY_CIDR`
- `GRAFANA_ADMIN_PASSWORD`

## 로컬 개발

### 사전 요구사항

- JDK 25
- Node.js 24 이상
- npm
- Docker Desktop 또는 Docker Engine

### 1. 개발용 DB만 Docker로 실행

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. 환경 파일 준비

```bash
copy .env.local.example .env.local
```

`.env.local` 에서 MariaDB 연결 정보를 채우면 Spring Boot가 로컬 MariaDB를 사용합니다. 값을 채우지 않으면 H2 인메모리로 실행됩니다.

### 3. 백엔드 실행

```bash
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

### 4. 프론트엔드 실행

```bash
cd web
npm ci
npm run dev
```

### 접속 주소

| 서비스 | URL |
| --- | --- |
| Web | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Actuator Health | http://localhost:8080/actuator/health |
| H2 Console | http://localhost:8080/h2-console |
| Admin Login | http://localhost:8080/admin/login |
| Ops Page | http://localhost:3000/ops |

## Docker 기반 실행

전체 스택을 로컬에서 Compose로 올리려면:

```bash
copy .env.example .env
docker compose up -d --build
```

운영형 실행은 `.env.prod` 를 준비한 뒤:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

## 테스트와 검증

### Backend

```bash
.\gradlew.bat test bootJar
```

엄격 모드:

```bash
.\gradlew.bat check -PstrictCoverage=true -PstrictStatic=true
```

포함 항목:

- JUnit 5
- Spring MVC/Test
- Security Test
- Testcontainers MariaDB
- JaCoCo
- Checkstyle
- SpotBugs

### Frontend

```bash
cd web
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

## 마이그레이션과 데이터 작업

### DB 마이그레이션

Flyway SQL 파일:

- `src/main/resources/db/migration/V1__baseline.sql`
- `...V9__drop_raw_json.sql`

### 운영 보조 스크립트

- `scripts/db-backup.sh`
- `scripts/db-restore.sh`
- `scripts/db-restore-drill.sh`
  - 세 스크립트 모두 `docker compose exec`로 `mariadb` 컨테이너 내부에서 실행된다(DB는 internal `app` 네트워크 전용이라 호스트 TCP로 접속 불가). 리포지토리 루트가 스크립트 위치 기준으로 자동 추론되며, 필요 시 `COMPOSE_PROJECT_DIR`/`COMPOSE_FILE`/`COMPOSE_ENV_FILE` 환경변수로 재정의 가능.
  - 스케줄링은 `scripts/server/init-ubuntu.sh`가 deploy 유저 crontab에 자동 등록한다: 매일 03:00 `db-backup.sh`, 매주 일요일 03:30 `db-restore-drill.sh`. 수동으로 등록하려면 동일한 라인을 `crontab -u deploy -e`에 추가.
- `scripts/migrate/run-migration.sh`
- `scripts/migrate/01-dump-source.sh`
- `scripts/migrate/02-import-winning-numbers.sh`
- `scripts/migrate/03-transform-saved-numbers.sh`
- `scripts/migrate/04-rebuild-stats.sh`
- `scripts/migrate/05-validate.sh`
- `scripts/migrate/06-cutover.sh`

## 배포

### GitHub Actions 워크플로우

- `ci.yml`
  - 백엔드 테스트/빌드
  - 프론트엔드 lint/typecheck/test/build
  - Checkstyle/SpotBugs
  - Caddyfile 검증
  - 제거된 기능 회귀 방지 검사
  - GHCR 이미지 푸시
  - Trivy 이미지 스캔
  - `latest` 태그 승격
- `cd.yml`
  - CI 성공 후 SSH로 서버 배포
  - 환경 렌더링
  - 이미지 pull 및 재기동
  - Caddy 강제 재생성
  - readiness 확인
  - smoke test
  - 실패 시 Caddy 우선 롤백 후 이미지 롤백
- `pr.yml`
  - PR용 백엔드/프론트엔드 빌드 체크
- `codeql.yml`
  - Java/TypeScript CodeQL 분석

### 서버 초기화

Ubuntu 24.04 서버 초기화 스크립트:

```bash
sudo bash scripts/server/init-ubuntu.sh
```

이 스크립트는 Docker, deploy 사용자, UFW, fail2ban, 리포지토리 클론, 로그 디렉터리까지 준비합니다.

## 모니터링

Compose 스택에 아래 서비스가 포함됩니다.

- Prometheus
- Grafana
- Alertmanager
- node-exporter

관련 설정 위치:

- `infra/prometheus/prometheus.yml`
- `infra/prometheus/rules/kraft_alerts.yml`
- `infra/grafana/provisioning/`
- `infra/alertmanager/alertmanager.yml.tmpl`

## 관리자와 운영

### 관리자 페이지

- 경로: `/admin`
- 구현: Spring MVC + Thymeleaf
- 기능:
  - 로그인
  - 최신 회차 확인
  - 회차 수집
  - 전체 백필 실행
  - 감사 로그 조회

세부 접속 절차는 [docs/admin-access.md](D:\workspace\spring\Kraft\docs\admin-access.md) 를 참고하세요. 이 문서는 로컬 참고용 성격입니다.

### 운영 대시보드

- 경로: `/ops`
- 구현: Next.js UI + `/ops-api/*` 프록시
- 기능:
  - 요약 상태 확인
  - 최신 회차 수집
  - 특정 회차 수집
  - 수동 적재
  - 작업 로그 필터링

## 보안 및 라우팅 메모

- 공개 도메인은 `/admin*`, `/ops*`, `/actuator*` 를 Caddy에서 차단합니다.
- 관리자 도메인에서만 `/admin*`, `/ops-api/*`, `/grafana/*` 를 프록시합니다.
- 저장 번호 API는 `X-Device-Token` 헤더를 요구합니다.
- 운영 API는 `X-Ops-Token` 을 요구합니다.
- Prometheus 엔드포인트는 trusted CIDR만 허용합니다.
- `scripts/deploy/smoke-test.sh` 는 배포 후 핵심 라우팅과 차단 규칙까지 검사합니다.

## 참고 메모

- 루트 `README.md` 는 실제 코드 기준 문서입니다. 환경 변수와 실행 절차는 예시 파일을 우선 기준으로 삼으세요.
- `.claude/`, `docs/` 일부 파일처럼 로컬 운영 문맥을 담는 보조 자료가 있을 수 있으나, 서비스 동작 기준은 코드와 Compose 설정이 우선입니다.
