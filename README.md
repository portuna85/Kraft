<div align="center">

# 🎯 KRAFT Lotto

### 한국 로또 6/45 데이터 수집 · 분석 · 조회 서비스

<p>
  <strong>당첨번호 자동 수집</strong>부터 <strong>번호 빈도 통계</strong>, <strong>조합 분석</strong>,
  <strong>동반 번호</strong>, <strong>추천 엔진</strong>, <strong>뉴스 수집</strong>까지 제공하는
  프로덕션 지향 로또 데이터 서비스입니다.
</p>

<p>
  <a href="https://www.kraft.io.kr"><strong>🌐 서비스 바로가기</strong></a>
  ·
  <a href="#-주요-기능"><strong>주요 기능</strong></a>
  ·
  <a href="#-로컬-개발"><strong>로컬 개발</strong></a>
  ·
  <a href="#-배포"><strong>배포</strong></a>
  ·
  <a href="#-보안"><strong>보안</strong></a>
</p>

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot&logoColor=white">
  <img alt="MariaDB" src="https://img.shields.io/badge/MariaDB-11.7-003545?logo=mariadb&logoColor=white">
  <img alt="Gradle" src="https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white">
  <img alt="Monitoring" src="https://img.shields.io/badge/Monitoring-Prometheus%20%2B%20Grafana-E6522C?logo=prometheus&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-See%20LICENSE-lightgrey">
</p>

</div>

---

## 📌 개요

**KRAFT Lotto**는 동행복권 공식 API를 기반으로 로또 6/45 당첨번호를 자동 수집하고, 수집된 데이터를 분석해 웹에서 조회할 수 있도록 구성한 서비스입니다.

단순한 당첨번호 조회를 넘어 다음 영역을 하나의 서비스에서 제공합니다.

| 번호 데이터 | 통계 분석 | 운영 관리 | 관측성 |
|:---:|:---:|:---:|:---:|
| 최신 회차 수집 | 빈도·패턴 분석 | Ops API | Prometheus |
| 누락 회차 보정 | 동반 번호 분석 | 관리자 콘솔 | Grafana |
| 특정 회차 재수집 | 추천 번호 | 감사 로그 | Discord 알림 |

> 운영 URL: **https://www.kraft.io.kr**

---

## 🧭 목차

| 구분 | 바로가기 |
|---|---|
| 서비스 기능 | [주요 기능](#-주요-기능), [공개 페이지](#-공개-페이지) |
| 운영 기능 | [Operations API](#-operations-api), [관리자 콘솔](#-관리자-콘솔) |
| 개발 환경 | [기술 스택](#-기술-스택), [프로젝트 구조](#-프로젝트-구조), [로컬 개발](#-로컬-개발) |
| 품질 관리 | [테스트](#-테스트), [커버리지 기준](#커버리지-기준) |
| 운영 배포 | [배포](#-배포), [환경 변수](#-환경-변수), [모니터링](#-모니터링) |
| 보안·DB | [보안](#-보안), [데이터베이스 마이그레이션](#-데이터베이스-마이그레이션) |

---

## ✨ 주요 기능

| 영역 | 제공 기능 |
|:---|:---|
| **당첨번호 수집** | 최신 회차 자동 수집, 누락 회차 보정, 특정 회차 강제 재수집 |
| **번호 분석** | 번호별 빈도, 패턴 통계, 조합 분석, 동반 출현 번호 분석 |
| **추천 엔진** | 제외 규칙 기반 번호 추천 |
| **뉴스** | Google News RSS 기반 로또 관련 뉴스 수집·분류 |
| **운영 관리** | Ops API, 수집 상태 조회, Circuit Breaker 상태 확인 |
| **관리자 기능** | 뉴스 검수, 캐시 관리, 감사 로그, 시스템 정보 확인 |
| **관측성** | Prometheus, Grafana, Alertmanager, OpenTelemetry |
| **품질 관리** | 단위·통합 테스트, E2E 테스트, 정적 분석, 커버리지 검증 |

---

## 🧱 기술 스택

| 분류 | 기술 |
|:---|:---|
| **Language / Runtime** | Java 25 |
| **Framework** | Spring Boot 4.0.6 |
| **Build** | Gradle 9.5.1, Kotlin DSL |
| **Database** | MariaDB 11.7, H2 |
| **ORM** | Spring Data JPA, Hibernate |
| **Migration** | Flyway |
| **Cache** | Caffeine |
| **Scheduler** | Spring Scheduler, ShedLock |
| **Template** | Thymeleaf |
| **Frontend** | Bootstrap 5, HTMX |
| **CSS Pipeline** | PostCSS, cssnano |
| **API Docs** | SpringDoc OpenAPI 3 |
| **Metrics** | Micrometer, Prometheus |
| **Tracing** | OpenTelemetry, OTLP |
| **Container** | Docker, Docker Compose |
| **Reverse Proxy** | Caddy |
| **Monitoring** | Grafana, Alertmanager |
| **E2E Test** | Playwright, axe-core |
| **Static Analysis** | Checkstyle 10, SpotBugs |
| **Coverage** | JaCoCo |

---

## 🗂️ 프로젝트 구조

```text
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
├── db/migration/               Flyway 마이그레이션
├── static/                     CSS, JS, 이미지, Bootstrap
└── templates/                  Thymeleaf 템플릿
```

---

## 🌐 공개 페이지

| 경로 | 설명 |
|:---|:---|
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

## 🔐 Operations API

`/ops/**` 경로는 운영 전용 API입니다. 접근 시 **IP allowlist**와 **운영 토큰** 검증을 모두 통과해야 합니다.

| 인증 방식 | Header |
|:---|:---|
| Ops Token | `X-Ops-Token: <token>` |
| Bearer Token | `Authorization: Bearer <token>` |

### 엔드포인트

| Method | Endpoint | 설명 |
|:---:|:---|:---|
| `POST` | `/ops/collect` | 최신 회차까지 당첨번호 수집 |
| `POST` | `/ops/collect/missing` | 누락 회차만 수집 |
| `POST` | `/ops/collect/refresh?round=N` | 특정 회차 강제 재수집 |
| `GET` | `/ops/collect/status` | 수집 작업 현황 |
| `GET` | `/ops/circuit-breakers` | API Circuit Breaker 상태 |
| `GET` | `/ops/data-freshness` | 최신 데이터 동기화 상태 |
| `POST` | `/ops/news/collect` | 뉴스 수집 트리거 |
| `GET` | `/ops/fetch-logs/**` | 수집 실패 로그 조회 |

---

## 🛠️ 관리자 콘솔

관리자 페이지는 Spring Security 폼 로그인을 사용합니다.

```env
KRAFT_ADMIN_ENABLED=true
```

| 경로 | 설명 |
|:---|:---|
| `/admin/ops` | 관리자 대시보드 |
| `/admin/ops/collection` | 회차 수집 실행 |
| `/admin/ops/news` | 뉴스 승인·거부·도메인 차단 |
| `/admin/ops/cache` | 캐시 관리 |
| `/admin/ops/audit` | 감사 로그 조회 |
| `/admin/ops/system` | 시스템 정보 |

---

## 📥 데이터 수집

### 당첨번호 수집 클라이언트

`kraft.api.client` 값에 따라 당첨번호 수집 클라이언트를 선택합니다.

| 값 | 설명 |
|:---:|:---|
| `mock` | 로컬 개발용 Mock 클라이언트 |
| `dhlottery` / `real` | 동행복권 공식 API |
| `smok` | Smok API |
| `public-data` | 공공데이터포털 API |

Primary 클라이언트 실패 시 `kraft.api.fallback-client`를 통해 Fallback 클라이언트로 자동 전환할 수 있습니다.

> Circuit Breaker 기본 정책: **5회 실패 → OPEN 30초**

### 자동 수집 스케줄

| 대상 | 기본 실행 시각 |
|:---|:---|
| 당첨번호 | 토요일 22:00, 일요일 06:00 |
| 뉴스 | 매 6시간 |
| 수집 로그 정리 | 매일 03:30, 기본 90일 보관 |

---

## 💻 로컬 개발

### 요구 사항

| 항목 | 버전 |
|:---|:---|
| Java | 25 |
| Docker | Docker & Docker Compose |
| Node.js | 22+ |

### 환경 파일 준비

`.env.local.example` 파일이 있다면 `.env`로 복사합니다.

```bash
cp .env.local.example .env
```

최소 필수 설정:

```env
KRAFT_DB_NAME=kraft_lotto
KRAFT_DB_USER=lotto_user
KRAFT_DB_PASSWORD=<password>
KRAFT_DB_ROOT_PASSWORD=<root_password>
KRAFT_API_CLIENT=mock
```

### 실행

```bash
# 1. DB 시작
docker compose -f docker-compose.local.yml up -d

# 2. 애플리케이션 실행
./gradlew bootRun

# 3. 서비스 접속
open http://localhost:8080

# 4. Swagger UI 접속, 로컬 전용
open http://localhost:8080/swagger-ui/index.html
```

---

## ✅ 테스트

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
|:---|---:|
| Line | 82% |
| Branch | 65% |
| Method | 88% |
| Class | 97% |

---

## 🚀 배포

### Docker Compose 서비스

| 서비스 | 역할 | 메모리 |
|:---|:---|---:|
| `app` | Spring Boot 애플리케이션 | 1 GB |
| `mariadb` | MariaDB 11.7 | 512 MB |
| `caddy` | TLS 리버스 프록시 | 64 MB |
| `prometheus` | 메트릭 수집, 15일 보관 | 256 MB |
| `grafana` | 대시보드 | 256 MB |
| `alertmanager` | Discord 알림 | 64 MB |
| `node-exporter` | 호스트 메트릭 | 64 MB |

### CI/CD 흐름

```text
main push
  └─ CI: ci.yml
       ├─ build-test        compile, test, coverage, Docker verification
       ├─ static-analysis   SpotBugs, Checkstyle
       ├─ e2e-smoke         Playwright, Chromium, Mobile Chrome
       ├─ docker-publish    GHCR image publish, sha + latest
       └─ security-scan     Trivy, Syft SBOM
  └─ CD: cd.yml
       └─ self-hosted runner
            ├─ render .env
            ├─ docker compose pull & up
            ├─ wait for readiness, max 5 minutes
            ├─ smoke test
            └─ rollback on failure
```

---

## ⚙️ 환경 변수

### 필수 환경 변수

| 변수 | 설명 |
|:---|:---|
| `KRAFT_DB_NAME` | 데이터베이스 이름 |
| `KRAFT_DB_USER` | DB 사용자 |
| `KRAFT_DB_PASSWORD` | DB 비밀번호 |
| `KRAFT_DB_ROOT_PASSWORD` | DB root 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | Ops API 인증 토큰 |
| `ALERTMANAGER_DISCORD_WEBHOOK_URL` | Discord 알림 웹훅 |

<details>
<summary><strong>선택 환경 변수 보기</strong></summary>

<br>

| 변수 | 기본값 | 설명 |
|:---|:---:|:---|
| `KRAFT_API_CLIENT` | `mock` | 당첨번호 수집 클라이언트 |
| `KRAFT_ADMIN_ENABLED` | `false` | 관리자 UI 활성화 |
| `KRAFT_ADMIN_PASSWORD_HASH` | — | 관리자 비밀번호 해시 (`{bcrypt}` 등 Spring Security prefix 포함) |
| `PUBLIC_DATA_API_KEY` | — | 공공데이터포털 API 키 |
| `KRAFT_COLLECT_AUTO_ENABLED` | `true` | 자동 수집 활성화 |
| `KRAFT_SECURITY_OPS_ALLOWED_IPS` | `127.0.0.1,::1,10.0.0.0/8,...` | Ops API 허용 IP |
| `KRAFT_SECURITY_TRUSTED_PROXIES` | `127.0.0.1/32` | `X-Forwarded-For`를 신뢰할 프록시 CIDR |
| `KRAFT_SECURITY_RATE_LIMIT_MAX_REQUESTS` | `120` | 분당 요청 제한 |
| `KRAFT_TRACING_SAMPLE_RATE` | `0.0` | OTEL 추적 샘플링 비율 |
| `KRAFT_GRAFANA_ADMIN_PASSWORD` | — | Grafana 관리자 비밀번호 |

</details>

전체 설정은 `src/main/resources/application.yml`을 기준으로 관리합니다.

---

## 📊 모니터링

| 컴포넌트 | URL | 범위 |
|:---|:---|:---|
| Prometheus | `http://localhost:9090` | 내부망 전용 |
| Grafana | `http://localhost:3000` | 내부망 전용 |
| Alertmanager | — | Discord 알림 발송 |

### 알림 규칙

| 규칙 | 조건 | 심각도 |
|:---|:---|:---:|
| AppDown | 2분 이상 응답 없음 | CRITICAL |
| CircuitBreakerOpen | 5분 이상 OPEN | CRITICAL |
| FallbackExhausted | Primary + Fallback 모두 실패 | CRITICAL |
| AutoCollectError | 1시간 내 오류 발생 | WARNING |
| HostDiskAlmostFull | 디스크 여유 < 10% | WARNING |
| HighMemoryUsage | 메모리 사용률 > 85% | WARNING |

---

## 🛡️ 보안

| 영역 | 제어 방식 |
|:---|:---|
| Public Web | 인증 없음, Rate Limiting 적용 |
| Ops API | IP allowlist + Bearer token 이중 검증 |
| Admin Page | Spring Security 폼 로그인 |
| Security Headers | CSP, X-Frame-Options, HSTS |
| Caddy | 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단 |
| Actuator | `health`, `info`, `prometheus`만 노출, IP 제한 |

`KRAFT_SECURITY_TRUSTED_PROXIES`는 운영 Docker/Caddy 네트워크 CIDR에 맞춰 최소 범위로 설정해야 합니다.
예시는 `172.18.0.0/16,127.0.0.1/32`이며, 실제 서버의 Docker network CIDR을 확인한 뒤 좁혀서 사용합니다.

취약점 제보는 공개 Issue가 아닌 이메일로 전달합니다.

```text
portuna85@gmail.com
```

---

## 🗃️ 데이터베이스 마이그레이션

Flyway 마이그레이션은 `src/main/resources/db/migration/` 아래에서 관리합니다.

<details open>
<summary><strong>Flyway V1 ~ V12</strong></summary>

<br>

| Version | 설명 |
|:---:|:---|
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
| V11 | 판매점 지역: 시도, 시군구, 구매방법 컬럼 추가 |
| V12 | 판매점 데이터 출처 컬럼 추가 |

</details>

---

## 📄 라이선스

라이선스 정보는 [LICENSE](LICENSE) 파일을 참조합니다.

---

<div align="center">

<sub>
Built for reliable Lotto 6/45 data collection, analysis, and operations.
</sub>

</div>
