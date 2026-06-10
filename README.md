# KRAFT Lotto

한국 로또 6/45 당첨번호 수집 · 분석 · 추천 서비스

> 운영 URL: **https://www.kraft.io.kr**

---

## 목차

- [개요](#개요)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [로컬 개발](#로컬-개발)
- [테스트](#테스트)
- [배포](#배포)
- [환경 변수](#환경-변수)
- [공개 페이지](#공개-페이지)
- [Operations API](#operations-api)
- [관리자 콘솔](#관리자-콘솔)
- [데이터 수집](#데이터-수집)
- [모니터링 및 알림](#모니터링-및-알림)
- [백업](#백업)
- [보안](#보안)
- [데이터베이스 마이그레이션](#데이터베이스-마이그레이션)
- [라이선스](#라이선스)

---

## 개요

Smok API(primary) / 공공데이터포털(fallback)로 로또 6/45 당첨번호를 자동 수집하고, 통계 분석과 번호 추천을 웹에서 제공하는 프로덕션 지향 서비스다.

| 번호 데이터 | 통계 분석 | 번호 추천 | 운영 관리 |
|---|---|---|---|
| 최신 회차 자동 수집 | 빈도 · 패턴 분석 | 제외 규칙 기반 추천 | Ops REST API |
| 누락 회차 자동 보정 | 동반 번호 분석 | 조합 당첨 이력 | 관리자 콘솔 |
| 특정 회차 재수집 | 요약 테이블 캐시 | — | 감사 로그 · 알림 |

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Runtime | Java 25 |
| Framework | Spring Boot 4.0.6 |
| Build | Gradle 9.5.1, Kotlin DSL |
| Database | MariaDB 11.7, H2(테스트) |
| ORM | Spring Data JPA, Hibernate |
| Migration | Flyway |
| Cache | Caffeine |
| Scheduler | Spring Scheduler, ShedLock |
| Template | Thymeleaf |
| Frontend | Bootstrap 5, HTMX |
| CSS Pipeline | PostCSS, PurgeCSS, cssnano |
| API Docs | SpringDoc OpenAPI 3 |
| Metrics | Micrometer, Prometheus |
| Tracing | OpenTelemetry, OTLP |
| Container | Docker, Docker Compose |
| Reverse Proxy | Caddy (TLS 자동 발급) |
| Monitoring | Grafana, Alertmanager, Discord |
| E2E Test | Playwright |
| Static Analysis | Checkstyle 10, SpotBugs |
| Coverage | JaCoCo |

---

## 프로젝트 구조

```
src/main/java/com/kraft/lotto/
├── feature/
│   ├── admin/          관리자 기능: 감사 로그, 뉴스 검수
│   ├── news/           로또 뉴스 수집 · 분류
│   ├── recommend/      제외 규칙 기반 번호 추천 엔진
│   ├── statistics/     빈도 · 패턴 · 동반 번호 통계 및 캐시
│   └── winningnumber/  당첨번호 수집 · 저장 · 조회
├── infra/config/       설정 바인딩 및 기동 검증
├── support/            필터(Rate Limit, IP, CSP), 공통 응답, 예외 처리
└── web/                공개 · Ops · 관리자 컨트롤러

src/main/resources/
├── application.yml             공통 설정
├── application-local.yml       로컬 개발 프로필
├── application-prod.yml        운영 프로필
├── db/migration/               Flyway 공통 마이그레이션 (V1–V22)
├── db/vendor/{h2,mysql,mariadb}/  벤더별 마이그레이션 (V15–V17)
├── static/                     CSS, JS, 이미지, 정적 자원
└── templates/                  Thymeleaf 템플릿

scripts/
├── db-backup.sh                로컬 + rclone 오프사이트 백업
├── db-restore-drill.sh         복원 드릴 (DRILL_STATE_FILE 갱신)
├── db-restore.sh               수동 복원
├── backup-timer/               systemd 백업 · 복원 드릴 타이머 설치
└── deploy/                     CI/CD 배포 스크립트 모음
```

---

## 로컬 개발

### 요구 사항

| 항목 | 버전 |
|---|---|
| Java | 25 |
| Docker & Docker Compose | 최신 |
| Node.js | 22 이상 |

### 빠른 시작

```bash
# 1. 환경 파일 준비
cp .env.local.example .env

# 2. DB 컨테이너 시작
docker compose -f docker-compose.local.yml up -d

# 3. 애플리케이션 실행
./gradlew bootRun

# 4. 접속
open http://localhost:8080
open http://localhost:8080/swagger-ui/index.html   # API 문서
```

최소 필수 `.env` 설정:

```env
KRAFT_DB_NAME=kraft_lotto
KRAFT_DB_USER=lotto_user
KRAFT_DB_PASSWORD=<password>
KRAFT_DB_ROOT_PASSWORD=<root_password>
KRAFT_API_CLIENT=mock
```

---

## 테스트

```bash
# 단위 + 통합 테스트
./gradlew test

# 정적 분석 포함 전체 검증 (커버리지 임계값 적용)
./gradlew check

# JavaScript 구문 검사
npm run check:js

# CSS 재빌드
npm run build:css

# E2E 스모크 테스트
npm run test:e2e
```

### 커버리지 임계값

| 지표 | 최솟값 |
|---|---|
| Line | 82 % |
| Branch | 65 % |
| Method | 88 % |
| Class | 97 % |

---

## 배포

### Docker Compose 서비스

| 서비스 | 역할 | 메모리 한도 |
|---|---|---|
| `app` | Spring Boot 애플리케이션 | 1 GB |
| `mariadb` | MariaDB 11.7 | 512 MB |
| `caddy` | TLS 리버스 프록시 | 64 MB |
| `prometheus` | 메트릭 수집 (15일 보관) | 256 MB |
| `grafana` | 대시보드 | 256 MB |
| `alertmanager` | Discord 알림 | 64 MB |
| `node-exporter` | 호스트 메트릭 | 64 MB |

모든 서비스에 `no-new-privileges: true` 적용. `app` 컨테이너는 추가로 `cap_drop: ALL`, `read_only`, `pids: 256` 적용.

### CI/CD 흐름

```
main push
  └─ CI (ci.yml)
       ├─ build-test       컴파일 · 테스트 · 커버리지 · bootJar
       ├─ static-analysis  SpotBugs, Checkstyle
       ├─ e2e-smoke        Playwright
       ├─ docker-publish   GHCR 이미지 push (SHA 태그)
       ├─ security-scan    Trivy 취약점 스캔, Syft SBOM
       └─ promote          SHA → latest 태그
  └─ CD (cd.yml)
       └─ self-hosted runner (운영 서버 직접 실행)
            ├─ .env 렌더링 (GitHub Secrets → 단일 따옴표 인코딩)
            ├─ GHCR 이미지 pull
            ├─ Flyway 히스토리 패치 (필요 시)
            ├─ docker compose up
            ├─ readiness 대기 — HTTP 폴링 + Docker health (최대 300 s)
            ├─ prod 프로필 검증
            ├─ smoke 확인
            └─ 실패 시 자동 롤백
```

---

## 환경 변수

`.env.example` 파일에 전체 키가 정의되어 있다. 운영 환경은 `scripts/deploy/render-env.sh`가 GitHub Secrets에서 자동 생성한다.

### 필수

| 변수 | 설명 |
|---|---|
| `KRAFT_DB_NAME` | 데이터베이스 이름 |
| `KRAFT_DB_USER` | DB 사용자 |
| `KRAFT_DB_PASSWORD` | DB 비밀번호 |
| `KRAFT_DB_ROOT_PASSWORD` | DB root 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | Ops API 인증 토큰 |
| `ALERTMANAGER_DISCORD_WEBHOOK_URL` | Discord 알림 웹훅 URL |
| `KRAFT_GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 |

### 선택

| 변수 | 기본값 | 설명 |
|---|---|---|
| `KRAFT_API_CLIENT` | `smok` | 당첨번호 수집 클라이언트 |
| `KRAFT_API_FALLBACK_CLIENT` | `public-data` | Fallback 클라이언트 |
| `KRAFT_ADMIN_ENABLED` | `false` | 관리자 UI 활성화 |
| `KRAFT_ADMIN_DOMAIN` | `admin.kraft.io.kr` | 관리자 전용 도메인 |
| `KRAFT_ADMIN_PASSWORD_HASH` | — | 관리자 비밀번호 bcrypt 해시 |
| `KRAFT_DOMAIN` | — | 서비스 공개 도메인 |
| `PUBLIC_DATA_API_KEY` | — | 공공데이터포털 API 키 |
| `KRAFT_COLLECT_AUTO_ENABLED` | `true` | 자동 수집 활성화 |
| `KRAFT_SECURITY_OPS_ALLOWED_IPS` | `127.0.0.1,::1,...` | Ops API 허용 IP 목록 |
| `KRAFT_SECURITY_TRUSTED_PROXIES` | `127.0.0.1/32` | 신뢰할 프록시 CIDR |
| `KRAFT_SECURITY_RATE_LIMIT_MAX_REQUESTS` | `120` | 분당 요청 제한 (per IP) |
| `KRAFT_TRACING_SAMPLE_RATE` | `0.0` | OTEL 추적 샘플링 비율 |
| `KRAFT_FLYWAY_REPAIR_ON_START` | `true` (prod) | 기동 시 Flyway repair 실행 |

전체 설정 키는 `src/main/resources/application.yml` 참조.

---

## 공개 페이지

| 경로 | 설명 |
|---|---|
| `/` | 홈: 추천 번호, 최신 회차, 빈도 카드 |
| `/latest` | 최신 회차 당첨번호 및 세후 수령액 |
| `/rounds` | 전체 회차 목록 및 회차 검색 |
| `/frequency` | 번호별 출현 빈도 통계 |
| `/stats` | 통계 요약 |
| `/analysis` | 번호 조합 분석 |
| `/companion` | 동반 출현 번호 분석 |
| `/news` | 로또 관련 뉴스 |
| `/methodology` | 추천 방식 설명 |
| `/data-source` | 데이터 출처 및 수집 이력 |
| `/faq` | 자주 묻는 질문 |

---

## Operations API

`/ops/**` 경로는 운영 전용 API다. **IP allowlist** + **운영 토큰** 이중 인증을 통과해야 한다.

| 인증 방식 | 헤더 |
|---|---|
| Ops Token | `X-Ops-Token: <token>` |
| Bearer Token | `Authorization: Bearer <token>` |

| Method | Endpoint | 설명 |
|---|---|---|
| `POST` | `/ops/collect` | 최신 회차까지 당첨번호 수집 |
| `POST` | `/ops/collect/missing` | 누락 회차만 수집 |
| `POST` | `/ops/collect/refresh?round=N` | 특정 회차 강제 재수집 |
| `GET` | `/ops/collect/status` | 수집 작업 현황 |
| `GET` | `/ops/circuit-breakers` | API Circuit Breaker 상태 |
| `GET` | `/ops/data-freshness` | 최신 데이터 동기화 상태 |
| `POST` | `/ops/news/collect` | 뉴스 수집 트리거 |
| `GET` | `/ops/fetch-logs/**` | 수집 실패 로그 조회 |

---

## 관리자 콘솔

`KRAFT_ADMIN_ENABLED=true` 설정 시 활성화된다. Spring Security 폼 로그인(bcrypt 해시 검증)을 사용하며, 5회 연속 실패 시 계정이 잠긴다.

| 경로 | 설명 |
|---|---|
| `/admin/ops` | 관리자 대시보드 |
| `/admin/ops/collection` | 회차 수집 실행 |
| `/admin/ops/news` | 뉴스 승인 · 거부 · 도메인 차단 |
| `/admin/ops/cache` | 캐시 관리 |
| `/admin/ops/audit` | 감사 로그 조회 |
| `/admin/ops/system` | 시스템 정보 |

---

## 데이터 수집

### 수집 클라이언트

`KRAFT_API_CLIENT` 값에 따라 당첨번호 수집 클라이언트를 선택한다.

| 값 | 설명 |
|---|---|
| `smok` | Smok API — **운영 primary** |
| `public-data` | 공공데이터포털 API — 운영 fallback |
| `dhlottery` / `real` | 동행복권 공식 API |
| `mock` | 로컬 개발용 Mock (기본) |

Primary 실패 시 `KRAFT_API_FALLBACK_CLIENT`(기본 `public-data`)로 자동 전환한다.
Circuit Breaker 기본 정책: **5회 실패 → OPEN 30 초**.

### 자동 수집 스케줄

| 대상 | 실행 시각 |
|---|---|
| 당첨번호 | 토요일 22:30, 일요일 07:00 |
| 뉴스 | 매 6시간 |
| 수집 로그 정리 | 매일 03:30 (90일 보관) |

---

## 모니터링 및 알림

| 컴포넌트 | 기본 포트 | 접근 범위 |
|---|---|---|
| Prometheus | 9090 | 내부망 전용 |
| Grafana | 3000 | 내부망 전용 |
| Alertmanager | 9093 | 내부망 전용 (Discord 웹훅 발송) |

### Prometheus 알림 규칙

| 규칙 | 조건 | 심각도 |
|---|---|---|
| AppDown | 2분 이상 응답 없음 | CRITICAL |
| CircuitBreakerOpen | 5분 이상 OPEN 상태 | CRITICAL |
| FallbackExhausted | Primary + Fallback 모두 실패 | CRITICAL |
| AutoCollectError | 1시간 내 수집 오류 발생 | WARNING |
| HostDiskAlmostFull | 디스크 여유 < 10 % | WARNING |
| HighMemoryUsage | 메모리 사용률 > 85 % | WARNING |

---

## 백업

`scripts/db-backup.sh`를 통해 로컬 덤프와 오프사이트 동기화를 수행한다.

```bash
DB_USER=... DB_PASSWORD=... ./scripts/db-backup.sh
```

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `BACKUP_DIR` | `/var/backups/kraft-lotto` | 로컬 백업 디렉터리 |
| `RETENTION_DAYS` | `7` | 로컬 백업 보관 일수 |
| `RCLONE_REMOTE` | — | rclone 오프사이트 경로 (예: `s3:my-bucket/...`) |
| `REMOTE_RETENTION_DAYS` | `30` | 원격 백업 보관 일수 |
| `BACKUP_GPG_RECIPIENT` | — | GPG 암호화 수신자 (미설정 시 평문 압축) |
| `DRILL_INTERVAL_DAYS` | `90` | 복원 드릴 주기 (초과 시 Prometheus 메트릭 경고) |

Prometheus `kraft_backup_restore_drill_overdue` 메트릭이 `1`이면 복원 드릴이 만료된 것이다.

### 복원 드릴

```bash
DB_USER=... DB_PASSWORD=... ./scripts/db-restore-drill.sh
```

성공 시 `DRILL_STATE_FILE`에 타임스탬프를 기록해 overdue 메트릭을 초기화한다. `scripts/backup-timer/install.sh`를 실행하면 일별 백업과 분기별 복원 드릴을 systemd 타이머로 설치한다.

---

## 보안

| 영역 | 제어 방식 |
|---|---|
| Public Web | 인증 없음, per-IP Rate Limit (기본 120 req/min) |
| Ops API | IP allowlist + Bearer 토큰 이중 검증 |
| Admin Page | Spring Security 폼 로그인, 5회 실패 시 잠금 |
| Security Headers | CSP, X-Frame-Options, HSTS (`max-age=31536000; includeSubDomains`) |
| Caddy | 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단 |
| Actuator | `health`, `info`, `prometheus`만 노출, IP 제한 |
| Container | `no-new-privileges`, `cap_drop: ALL`(app), `read_only`, `pids: 256` |
| XFF 처리 | XFF 오른쪽→왼쪽 순회로 위조 IP 삽입 공격 차단 |
| Secret 주입 | bcrypt `$` 등 특수문자는 단일 따옴표 인코딩 후 `env_file`로 주입 |

`KRAFT_SECURITY_TRUSTED_PROXIES`는 실제 Docker network CIDR에 맞춰 최소 범위로 설정한다.

취약점 제보: portuna85@gmail.com (공개 Issue 사용 금지)

---

## 데이터베이스 마이그레이션

공통 마이그레이션(`db/migration/`)은 모든 환경에, 벤더별 마이그레이션(`db/vendor/{vendor}/`)은 해당 DB에만 적용된다.

구조: 공통 V1–V14, V18–V22 / 벤더별 V15–V17

| Version | 설명 |
|---|---|
| V1 | 기본 스키마: 당첨번호, 수집 로그, 판매점, 빈도 캐시, ShedLock |
| V2 | 수집 로그 인덱스 최적화 |
| V3 | 뉴스 소스 등급 컬럼 추가 |
| V4 | 패턴 통계 요약 테이블 추가 |
| V5 | 동반 번호 요약 테이블 추가 |
| V6 | 뉴스 승인 컬럼 추가 |
| V7 | 판매점 수집 시각 컬럼 추가 |
| V8 | 뉴스 소스 도메인 컬럼 추가 |
| V9 | 관리자 감사 로그, 차단 키워드 · 도메인 테이블 추가 |
| V10 | 뉴스 거절 컬럼 추가 |
| V11 | 판매점 지역 컬럼 추가 (시도, 시군구, 구매방법) |
| V12 | 판매점 데이터 출처 컬럼 추가 |
| V13 | 판매점 무결성 제약 정비 |
| V14 | 뉴스 거절 사유 컬럼 추가 |
| V15 | 판매점 dedupe 컬럼 NOT NULL 정규화 (벤더별) |
| V16 | 수집 로그 진단 컬럼 추가: `api_client`, `retry_count`, `latency_ms` (벤더별) |
| V17 | 뉴스 `title_hash` 컬럼 추가 (벤더별) |
| V18 | 뉴스 `title_hash` 인덱스 추가 |
| V19 | 관리자 감사 로그 복합 인덱스 추가 |
| V20 | 빈도 집계 뷰 및 뉴스 공개 목록 인덱스 추가 |
| V21 | 판매점(`winning_stores`) 테이블 제거 |
| V22 | 미사용 빈도 집계 뷰(`v_ball_frequencies`) 제거 |

---

## 라이선스

[LICENSE](LICENSE) 참조.
