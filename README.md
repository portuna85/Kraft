# KRAFT Lotto

로또 6/45 당첨 결과 조회, 번호 추천, 통계 분석, 번호 저장 기능을 제공하는 풀스택 서비스입니다. 1인 개발로 기획부터 백엔드/프론트엔드 구현, 인프라 구성, CI/CD, 운영 자동화, 모니터링까지 전 영역을 직접 설계하고 운영하고 있습니다.

**Live:** [kraft.io.kr](https://kraft.io.kr/)

---

## 이 프로젝트가 보여주는 것

단순 CRUD 토이 프로젝트가 아니라, 실제 트래픽을 받는 서비스를 1인이 끝까지 책임지는 과정에서 나온 엔지니어링 판단들을 모아둔 저장소입니다.

- **테스트로 회귀를 막는다** — JUnit 179개 + Vitest 45개. JaCoCo 커버리지 게이트(라인 82% / 분기 65% / 메서드 88% / 클래스 97%)를 CI에 강제해, 통과 못 하면 머지할 수 없습니다.
- **정적 분석이 실제로 버그를 잡는다** — Checkstyle + SpotBugs를 strict 모드로 PR에 건다. SpotBugs를 Java 25 호환 버전으로 올리는 과정에서 클라이언트 입력이 검증 없이 응답 헤더에 반영되는 CRLF 인젝션 가능성을 실제로 발견해 고쳤습니다.
- **인프라를 깊이까지 하드닝한다** — 모든 컨테이너 이미지 digest 고정, `cap_drop: ALL` + `no-new-privileges`, Docker 네트워크를 `edge`/`app`/`monitoring`으로 분리(`app`·`monitoring`은 internal 전용이라 DB는 호스트에서 직접 닿지 않음), Caddy `route {}`로 라우팅 우선순위 명시.
- **공급망까지 신경 쓴다** — GHCR 이미지에 SBOM·provenance attestation 부착, Trivy로 HIGH/CRITICAL 취약점 차단, CodeQL을 PR 단계부터 실행, Dependabot으로 의존성 추적.
- **동시성 문제를 가정하고 설계한다** — 외부 회차 수집과 수동 운영 API가 같은 회차를 동시에 처리해도 500이 아니라 멱등하게 update로 수렴하도록 처리, 통계 재집계는 ShedLock으로 다중 인스턴스 환경에서도 중복 실행을 막음.
- **운영 자동화까지 끝낸다** — DB 백업/복구 드릴이 cron으로 매일/매주 자동 실행되고, 배포 실패 시 Caddy 설정과 이미지를 자동으로 롤백.

각 판단의 배경은 [`docs/improvement.md`](docs/improvement.md)(로컬 전용, 비공개)에 더 자세히 기록돼 있습니다.

---

## 핵심 기능

- 최신 회차 / 특정 회차 / 전체 회차 목록 조회
- 번호 추천(과거 1등 조합과의 중복 확인 포함)
- 번호 출현 빈도, 패턴 통계, 동반 출현 통계, 조합 분석
- 기기 단위 저장 번호 관리(`X-Device-Token` 기반, 서버에 개인정보 없이 식별)
- 운영 대시보드에서 수동 적재 · 최신 회차 수집 · 작업 로그 조회
- 관리자 SSR 페이지에서 회차 수집과 감사 로그 확인
- 자동 수집, ISR on-demand revalidation, 보안 헤더, 공개 API rate limit

## 기술 스택

| 영역 | 사용 기술 | 비고 |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4.1, Spring Web/Validation/JPA/Security, Thymeleaf | 가상 스레드 대응 런타임, 기능 단위 패키징 |
| Data | MariaDB 11.7, Flyway, Caffeine, H2(로컬) | summary 테이블 + ShedLock 분산 락 |
| Resilience | Resilience4j, ShedLock | 외부 API 서킷 브레이커, 스케줄러 중복 실행 방지 |
| Frontend | Next.js 16(App Router), React 19, TypeScript | SSR/ISR 혼합, 요청별 CSP nonce |
| Infra | Docker Compose, Caddy | 네트워크 분리, 이미지 digest 고정 |
| Observability | Spring Actuator, Prometheus, Grafana, Alertmanager, node-exporter | |
| Test | JUnit 5, Spring Test, Testcontainers(MariaDB), Vitest, Testing Library | |
| CI/CD | GitHub Actions, GHCR, Trivy, CodeQL, SBOM/provenance, SSH 배포 | |

## 아키텍처

```text
                        ┌─────────────┐
  사용자 ──────────────▶│    Caddy    │  edge / app 네트워크
                        │ (리버스 프록시) │  도메인별 라우팅, TLS, 보안 헤더
                        └──────┬──────┘
                  ┌────────────┼────────────┐
                  ▼                         ▼
          ┌───────────────┐         ┌──────────────┐
          │  Next.js (web) │         │ Spring Boot   │
          │  SSR / ISR     │◀───────▶│  (backend)    │  app / monitoring 네트워크
          └───────────────┘  내부호출  └──────┬───────┘
                                              │
                          ┌───────────────────┼───────────────────┐
                          ▼                   ▼                   ▼
                   ┌────────────┐    ┌────────────────┐   ┌──────────────┐
                   │  MariaDB    │    │ Prometheus/     │   │ 외부 당첨번호  │
                   │ (app 전용,  │    │ Grafana/        │   │ 수집 API      │
                   │  internal)  │    │ Alertmanager    │   │ (서킷 브레이커) │
                   └────────────┘    └────────────────┘   └──────────────┘
```

`mariadb`/Prometheus 등은 `internal: true` 네트워크에 있어 호스트에서 직접 접근할 수 없고, Caddy를 통해서만 외부 트래픽이 들어옵니다.

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

## 빠른 시작 (로컬 개발)

### 사전 요구사항

- JDK 25, Node.js 24+, npm, Docker

### 1. 개발용 DB 실행

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. 환경 파일 준비

```bash
copy .env.local.example .env.local
```

값을 채우지 않으면 H2 인메모리로 동작합니다.

### 3. 백엔드 / 프론트엔드 실행

```bash
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

```bash
cd web && npm ci && npm run dev
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

### Docker로 전체 스택 실행

```bash
copy .env.example .env
docker compose up -d --build
```

운영형 실행(`.env.prod` 준비 후):

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

## 테스트와 정적 분석

```bash
# Backend
.\gradlew.bat test bootJar
.\gradlew.bat check -PstrictCoverage=true -PstrictStatic=true   # 커버리지 게이트 + Checkstyle + SpotBugs

# Frontend
cd web
npm ci && npm run lint && npm run typecheck && npm test && npm run build
```

| 항목 | 도구 | 게이트 |
| --- | --- | --- |
| 백엔드 단위/통합 테스트 | JUnit 5, Spring Test, Testcontainers(MariaDB) | 179개, CI 필수 |
| 커버리지 | JaCoCo | 라인 82% / 분기 65% / 메서드 88% / 클래스 97% |
| 정적 분석 | Checkstyle, SpotBugs | strict 모드 PR 게이트 |
| 프론트엔드 테스트 | Vitest, Testing Library | 45개 |
| 보안 스캔 | Trivy(이미지+의존성), CodeQL | PR 단계부터 실행 |

## API 개요

### 공개 API (`/api/v1/*`)

- `GET /api/v1/rounds/latest`, `GET /api/v1/rounds`, `GET /api/v1/rounds/{round}`
- `POST /api/v1/numbers/recommend`, `GET /api/v1/numbers/check`
- `GET /api/v1/stats/frequency`, `/patterns`, `/companion`, `POST /api/v1/stats/analysis`
- `GET /api/v1/saved`, `POST /api/v1/saved`, `DELETE /api/v1/saved/{id}`

공식 진입점은 **Caddy → `backend:8080` 직결**입니다. `web/src/app/api/v1/**`에도 동일 경로의 Next.js route handler가 있지만, 운영에서는 Caddy가 먼저 가로채므로 `npm run dev` 단독 실행 시에만 쓰이는 로컬 개발용 폴백입니다.

### 운영 API (`/ops/*`, `X-Ops-Token` 필요)

`GET /ops/summary`, `GET /ops/logs`, `POST /ops/rounds`, `POST /ops/collect/latest`, `POST /ops/collect/{round}`

## 배포 파이프라인

```text
PR 생성 ──▶ Backend/Web 빌드·테스트, Checkstyle/SpotBugs, CodeQL, Trivy(fs) ──▶ 머지
main push ──▶ 전체 검증 + GHCR 이미지 빌드(SBOM/provenance) ──▶ Trivy 이미지 스캔
         ──▶ latest 태그 승격 ──▶ (cd.yml) SSH 배포 ──▶ readiness 확인 ──▶ smoke test
                                                       └─ 실패 시 Caddy → 이미지 순으로 자동 롤백
```

- `ci.yml` — 테스트/빌드/정적 분석/이미지 빌드/보안 스캔/태그 승격
- `cd.yml` — SSH 배포, Caddy 강제 재생성, readiness/smoke test, 실패 시 자동 롤백
- `pr.yml` — PR 전용 빌드 체크 + 의존성 취약점 스캔
- `codeql.yml` — Java/TypeScript 정적 보안 분석(push + PR + 주간 스케줄)

서버 초기화는 `sudo bash scripts/server/init-ubuntu.sh` 한 번으로 Docker, deploy 사용자, UFW, fail2ban, 리포지토리 클론, DB 백업 cron까지 끝납니다.

## 운영 자동화

- **DB 백업/복구**: `scripts/db-backup.sh`(`docker compose exec`로 internal 네트워크의 mariadb 컨테이너 내부에서 실행 — 호스트 TCP 접근 자체가 불가능한 구조). cron으로 매일 03:00 백업, 매주 일요일 03:30 복구 드릴이 자동 실행되고 결과가 검증됩니다.
- **마이그레이션**: Flyway SQL(`src/main/resources/db/migration/`)로 스키마 버전 관리.
- **모니터링**: Prometheus가 지표 수집, Grafana 대시보드, Alertmanager 알림, node-exporter 시스템 지표. 설정은 `infra/`에 있습니다.

## 보안 메모

- 공개 도메인은 Caddy에서 `/admin*`, `/ops*`, `/actuator*`를 차단하고, 관리자 도메인에서만 허용합니다.
- 저장 번호 API는 `X-Device-Token`, 운영 API는 `X-Ops-Token`을 요구합니다.
- Prometheus 엔드포인트는 trusted CIDR만 허용합니다.
- 요청별 CSP nonce(`proxy.ts`)로 인라인 스크립트 없이 엄격한 CSP를 적용합니다.
- `scripts/deploy/smoke-test.sh`가 배포 직후 라우팅·차단 규칙까지 검사합니다.

## 환경 파일

| 파일 | 용도 |
| --- | --- |
| `.env.local.example` | 로컬에서 Spring Boot 직접 실행 |
| `.env.example` | Docker Compose 개발/단일 서버 실행 |
| `.env.prod.example` | 운영 배포 |

주요 환경 변수: `KRAFT_DB_URL`, `KRAFT_DB_USERNAME`, `KRAFT_DB_PASSWORD`, `KRAFT_PUBLIC_BASE_URL`, `KRAFT_BACKEND_INTERNAL_URL`, `KRAFT_REVALIDATE_SECRET`, `KRAFT_OPS_TOKEN`, `KRAFT_EXTERNAL_LOTTO_URL_TEMPLATE`, `KRAFT_ADMIN_BOOTSTRAP_USERNAME/PASSWORD`, `KRAFT_SECURITY_TRUSTED_PROXY_CIDR`, `GRAFANA_ADMIN_PASSWORD`.
