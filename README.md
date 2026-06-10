# KRAFT Lotto

한국 로또 6/45 당첨번호 수집 · 분석 · 조회 서비스

> 운영 URL: **https://www.kraft.io.kr**

---

## 개요

동행복권 공식 API 기반으로 로또 6/45 당첨번호를 자동 수집하고, 수집된 데이터를 분석해 웹에서 조회할 수 있도록 구성한 프로덕션 지향 서비스다.

| 번호 데이터 | 통계 분석 | 운영 관리 | 관측성 |
|---|---|---|---|
| 최신 회차 자동 수집 | 빈도·패턴 분석 | Ops API | Prometheus |
| 누락 회차 보정 | 동반 번호 분석 | 관리자 콘솔 | Grafana |
| 특정 회차 재수집 | 추천 엔진 | 감사 로그 | Discord 알림 |

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language / Runtime | Java 25 |
| Framework | Spring Boot 4.0.6 |
| Build | Gradle 9.5.1, Kotlin DSL |
| Database | MariaDB 11.7, H2 |
| ORM | Spring Data JPA, Hibernate |
| Migration | Flyway |
| Cache | Caffeine |
| Scheduler | Spring Scheduler, ShedLock |
| Template | Thymeleaf |
| Frontend | Bootstrap 5, HTMX |
| CSS Pipeline | PostCSS, cssnano |
| API Docs | SpringDoc OpenAPI 3 |
| Metrics | Micrometer, Prometheus |
| Tracing | OpenTelemetry, OTLP |
| Container | Docker, Docker Compose |
| Reverse Proxy | Caddy |
| Monitoring | Grafana, Alertmanager |
| E2E Test | Playwright |
| Static Analysis | Checkstyle 10, SpotBugs |
| Coverage | JaCoCo |

---

## 프로젝트 구조

```
src/main/java/com/kraft/lotto/
├── feature/
│   ├── admin/          관리자 기능: 감사 로그, 뉴스 검수
│   ├── news/           로또 뉴스 수집·분류
│   ├── recommend/      제외 규칙 기반 번호 추천 엔진
│   ├── statistics/     빈도·패턴·동반 번호 통계
│   └── winningnumber/  당첨번호 수집·저장·조회
├── infra/config/       설정 바인딩 및 검증
├── support/            필터, IP 제어, 공통 응답, 예외 처리
└── web/                공개·Ops·관리자 컨트롤러

src/main/resources/
├── application.yml             공통 설정
├── application-local.yml       로컬 개발 프로필
├── application-prod.yml        운영 프로필
├── db/migration/               Flyway 공통 마이그레이션 (V1–V14)
├── db/vendor/h2/               H2 전용 마이그레이션 (V15–V17)
├── db/vendor/mysql/            MySQL 전용 마이그레이션 (V15–V17)
├── db/vendor/mariadb/          MariaDB 전용 마이그레이션 (V15–V17)
├── static/                     CSS, JS, 이미지, Bootstrap
└── templates/                  Thymeleaf 템플릿
```

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

`/ops/**` 경로는 운영 전용 API다. **IP allowlist** + **운영 토큰** 이중 검증을 통과해야 한다.

| 인증 방식 | Header |
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

`KRAFT_ADMIN_ENABLED=true` 설정 시 활성화된다. Spring Security 폼 로그인을 사용한다.

| 경로 | 설명 |
|---|---|
| `/admin/ops` | 관리자 대시보드 |
| `/admin/ops/collection` | 회차 수집 실행 |
| `/admin/ops/news` | 뉴스 승인·거부·도메인 차단 |
| `/admin/ops/cache` | 캐시 관리 |
| `/admin/ops/audit` | 감사 로그 조회 |
| `/admin/ops/system` | 시스템 정보 |

---

## 데이터 수집

### 수집 클라이언트

`kraft.api.client` 값에 따라 당첨번호 수집 클라이언트를 선택한다.

| 값 | 설명 |
|---|---|
| `mock` | 로컬 개발용 Mock |
| `smok` | Smok API (운영 primary) |
| `public-data` | 공공데이터포털 API (운영 fallback) |
| `dhlottery` / `real` | 동행복권 공식 API |

Primary 실패 시 `kraft.api.fallback-client`를 통해 자동 전환한다. 권장 fallback은 `public-data`이며, `.env.prod.example`과 `render-env.sh`에 명시되어 있다. 운영 환경 파일에 `KRAFT_API_FALLBACK_CLIENT`를 반드시 명시할 것.

Circuit Breaker 기본 정책: **5회 실패 → OPEN 30초**

### 자동 수집 스케줄

| 대상 | 기본 실행 시각 |
|---|---|
| 당첨번호 | 토요일 22:30, 일요일 07:00 |
| 뉴스 | 매 6시간 |
| 수집 로그 정리 | 매일 03:30 (기본 90일 보관) |

---

## 로컬 개발

### 요구 사항

| 항목 | 버전 |
|---|---|
| Java | 25 |
| Docker | Docker & Docker Compose |
| Node.js | 22+ |

### 실행

```bash
# 환경 파일 준비
cp .env.local.example .env

# DB 시작
docker compose -f docker-compose.local.yml up -d

# 애플리케이션 실행
./gradlew bootRun

# 접속
http://localhost:8080
http://localhost:8080/swagger-ui/index.html
```

최소 필수 설정:

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

# 정적 분석 포함 전체 검증
./gradlew check

# JavaScript 구문 검사
npm run check:js

# CSS 빌드
npm run build:css

# E2E 스모크 테스트
npm run test:e2e
```

### 커버리지 기준

| 지표 | 최솟값 |
|---|---|
| Line | 82% |
| Branch | 65% |
| Method | 88% |
| Class | 97% |

---

## 배포

### Docker Compose 서비스

| 서비스 | 역할 | 메모리 |
|---|---|---|
| `app` | Spring Boot 애플리케이션 | 1 GB |
| `mariadb` | MariaDB 11.7 | 512 MB |
| `caddy` | TLS 리버스 프록시 | 64 MB |
| `prometheus` | 메트릭 수집, 15일 보관 | 256 MB |
| `grafana` | 대시보드 | 256 MB |
| `alertmanager` | Discord 알림 | 64 MB |
| `node-exporter` | 호스트 메트릭 | 64 MB |

### CI/CD 흐름

```
main push
  └─ CI (ci.yml)
       ├─ build-test        컴파일, 테스트, 커버리지, bootJar
       ├─ static-analysis   SpotBugs, Checkstyle
       ├─ e2e-smoke         Playwright
       ├─ docker-publish    GHCR 이미지 push (SHA 태그)
       ├─ security-scan     Trivy 취약점 스캔, Syft SBOM
       └─ promote           SHA → latest 태그
  └─ CD (cd.yml)
       └─ self-hosted runner
            ├─ .env 렌더링
            ├─ docker compose pull & up
            ├─ readiness 대기 (최대 5분)
            ├─ smoke 확인
            └─ 실패 시 자동 롤백
```

---

## 환경 변수

### 필수

| 변수 | 설명 |
|---|---|
| `KRAFT_DB_NAME` | 데이터베이스 이름 |
| `KRAFT_DB_USER` | DB 사용자 |
| `KRAFT_DB_PASSWORD` | DB 비밀번호 |
| `KRAFT_DB_ROOT_PASSWORD` | DB root 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | Ops API 인증 토큰 |
| `ALERTMANAGER_DISCORD_WEBHOOK_URL` | Discord 알림 웹훅 |
| `KRAFT_GRAFANA_ADMIN_PASSWORD` | Grafana 관리자 비밀번호 |

### 선택

| 변수 | 기본값 | 설명 |
|---|---|---|
| `KRAFT_API_CLIENT` | `mock` | 당첨번호 수집 클라이언트 |
| `KRAFT_API_FALLBACK_CLIENT` | `public-data` | Fallback 클라이언트 |
| `KRAFT_ADMIN_ENABLED` | `false` | 관리자 UI 활성화 |
| `KRAFT_ADMIN_PASSWORD_HASH` | — | 관리자 비밀번호 해시 (Spring Security prefix 포함) |
| `PUBLIC_DATA_API_KEY` | — | 공공데이터포털 API 키 |
| `KRAFT_COLLECT_AUTO_ENABLED` | `true` | 자동 수집 활성화 |
| `KRAFT_SECURITY_OPS_ALLOWED_IPS` | `127.0.0.1,::1,...` | Ops API 허용 IP |
| `KRAFT_SECURITY_TRUSTED_PROXIES` | `127.0.0.1/32` | 신뢰할 프록시 CIDR |
| `KRAFT_SECURITY_RATE_LIMIT_MAX_REQUESTS` | `120` | 분당 요청 제한 |
| `KRAFT_TRACING_SAMPLE_RATE` | `0.0` | OTEL 추적 샘플링 비율 |
| `KRAFT_API_ENRICH_DELAY_HOURS` | `12` | enrich 지연 시간 (추첨 후) |

전체 설정은 `src/main/resources/application.yml` 참조.

---

## 모니터링

| 컴포넌트 | URL | 범위 |
|---|---|---|
| Prometheus | `http://localhost:9090` | 내부망 전용 |
| Grafana | `http://localhost:3000` | 내부망 전용 |
| Alertmanager | — | Discord 알림 발송 |

### 알림 규칙

| 규칙 | 조건 | 심각도 |
|---|---|---|
| AppDown | 2분 이상 응답 없음 | CRITICAL |
| CircuitBreakerOpen | 5분 이상 OPEN | CRITICAL |
| FallbackExhausted | Primary + Fallback 모두 실패 | CRITICAL |
| AutoCollectError | 1시간 내 오류 발생 | WARNING |
| HostDiskAlmostFull | 디스크 여유 < 10% | WARNING |
| HighMemoryUsage | 메모리 사용률 > 85% | WARNING |

---

## 보안

| 영역 | 제어 방식 |
|---|---|
| Public Web | 인증 없음, Rate Limiting 적용 |
| Ops API | IP allowlist + Bearer token 이중 검증 |
| Admin Page | Spring Security 폼 로그인, 로그인 잠금 |
| Security Headers | CSP, X-Frame-Options, HSTS |
| Caddy | 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단 |
| Actuator | `health`, `info`, `prometheus`만 노출, IP 제한 |
| Container | `no-new-privileges`, `cap_drop: ALL`, `read_only`, `pids: 256` |

`KRAFT_SECURITY_TRUSTED_PROXIES`는 실제 서버의 Docker network CIDR에 맞춰 최소 범위로 설정한다.

취약점 제보: portuna85@gmail.com (공개 Issue 사용 금지)

---

## 데이터베이스 마이그레이션

Flyway 마이그레이션은 `src/main/resources/db/` 아래에서 관리한다.

공통 마이그레이션(`db/migration/`)은 모든 환경에 적용되고, 벤더별 마이그레이션(`db/vendor/{vendor}/`)은 해당 DB에만 적용된다.

구조: 공통 V1–V14, V18–V20 / 벤더별 V15–V17

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
| V9 | 관리자 감사 로그, 차단 키워드/도메인 테이블 추가 |
| V10 | 뉴스 거절 컬럼 추가 |
| V11 | 판매점 지역 컬럼 추가 (시도, 시군구, 구매방법) |
| V12 | 판매점 데이터 출처 컬럼 추가 |
| V13 | 판매점 무결성 제약 정비 |
| V14 | 뉴스 거절 사유 컬럼 추가 |
| V15 | 판매점 dedupe 컬럼 NOT NULL 정규화 (벤더별) |
| V16 | 수집 로그 진단 컬럼 추가: `api_client`, `retry_count`, `latency_ms` (벤더별) |
| V17 | 뉴스 title_hash 컬럼 추가 (벤더별) |
| V18 | 뉴스 title_hash 인덱스 추가 (공통) |
| V19 | 관리자 감사 로그 복합 인덱스 추가 (공통) |
| V20 | 빈도 집계 뷰 및 뉴스 공개 인덱스 추가 (공통) |

---

## 라이선스

[LICENSE](LICENSE) 참조.
