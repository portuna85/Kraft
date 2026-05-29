<div align="center">

# kLo

**한국 로또 6/45 번호 추천 서비스**

통계 기반 편향 제거 규칙을 적용한 번호 조합 추천 · 당첨 번호 수집 · 빈도 통계

[![CI](https://github.com/portuna85/kLo/actions/workflows/ci.yml/badge.svg)](https://github.com/portuna85/kLo/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MariaDB](https://img.shields.io/badge/MariaDB-11.7-003545?style=flat-square&logo=mariadb&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)

</div>

---

## 목차

1. [주요 기능](#주요-기능)
2. [기술 스택](#기술-스택)
3. [아키텍처](#아키텍처)
4. [UI / 디자인](#ui--디자인)
5. [프로파일](#프로파일)
6. [빠른 시작](#빠른-시작)
7. [환경 변수](#환경-변수)
8. [빌드 및 테스트](#빌드-및-테스트)
9. [주요 엔드포인트](#주요-엔드포인트)
10. [보안](#보안)
11. [스케줄러](#스케줄러)
12. [라이선스](#라이선스)

---

## 주요 기능

| # | 기능 | 설명 |
|---|---|---|
| 01 | **번호 추천** | 생일 편향, 등차수열, 단일 십의 자리 집중, 장기 미출현, 과거 당첨 조합 등 **5가지 ExclusionRule** 을 통과한 조합 추천 |
| 02 | **당첨 번호 수집** | 동행복권 API 에서 회차별 당첨 번호를 **자동(토·일)** 또는 **수동(Ops API)** 으로 수집 |
| 03 | **번호 빈도 통계** | 전체 회차 누적 번호 출현 빈도 요약 제공 |
| 04 | **회차 검색** | 특정 회차 당첨 번호 및 1등 당첨 정보 조회 |
| 05 | **Ops 관리 대시보드** | 수집 트리거, 서킷브레이커 상태, fetch 로그, 실패 이유 분석 |

---

## 기술 스택

| 분류 | 내용 |
|------|------|
| **런타임** | Java 25, Spring Boot 4.0.5 |
| **데이터베이스** | MariaDB 11.7 *(운영)*, H2 *(테스트)* |
| **스키마 관리** | Flyway |
| **캐시** | Caffeine *(번호 빈도, 이력, 통계 요약)* |
| **분산 스케줄러** | ShedLock + JDBC |
| **프론트엔드** | Thymeleaf + HTMX + Bootstrap 5.3 |
| **UI 레이어** | 글라스모피즘 오버레이 CSS · 3D 구슬 효과 · 라이트/다크 테마 |
| **PWA** | Web App Manifest · safe-area-inset · 홈 화면 추가 지원 |
| **API 문서** | SpringDoc / OpenAPI — `/swagger-ui.html` *(local 프로파일만)* |
| **컨테이너** | Docker Compose |
| **테스트** | JUnit 5, jqwik *(property-based)*, Testcontainers, Playwright *(E2E)* |
| **정적 분석** | Checkstyle, SpotBugs, JaCoCo |
| **접근성** | WCAG 2 AA 준수 (axe-core E2E 검증) |

---

## 아키텍처

```text
com.kraft.lotto
├── feature/
│   ├── winningnumber/   당첨 번호 수집·조회 (API 클라이언트, 스케줄러, 서킷브레이커)
│   ├── recommend/       번호 추천 엔진 (ExclusionRule 전략 패턴)
│   └── statistics/      번호 빈도 통계 요약
├── infra/config/        프로퍼티 바인딩, 시작 시 검증
├── support/             공통: 필터(Rate Limit, Ops/Actuator 접근, 보안 헤더), 예외 처리
└── web/                 홈 컨트롤러, Ops 컨트롤러
```

### 핵심 컴포넌트

| 컴포넌트 | 설명 |
|---|---|
| **`ExclusionRule`** | 추천 번호를 필터링하는 전략 인터페이스. 구현체는 Spring이 자동 수집하여 고정 순서로 평가 |
| **`ApiCircuitBreaker`** | 동행복권 API 호출을 감싸는 직접 구현 서킷브레이커 (`closed → open → half-open`) |
| **`LottoCollectionCommandService`** | `CollectionRunState` 뮤텍스 + ShedLock으로 중복 수집 방지 |
| **`PastWinningCache`** | 시작 시 전체 당첨 이력을 메모리에 로드하여 추천 성능 확보 |

---

## UI / 디자인

### 글라스모피즘 오버레이 (`kraft-redesign.css`)

- 기존 Thymeleaf/HTMX 구조·JS를 변경하지 않는 **CSS 전용 오버레이**
- `app.css` 이후 로드되어 우선 적용

### 주요 시각 요소

| 요소 | 내용 |
|---|---|
| **배경** | 방사형 그라데이션 + 격자 패턴 오버레이 |
| **Navbar** | `backdrop-filter: blur(18px)` 글라스 효과, sticky 상단 고정 |
| **카드** | `backdrop-filter: blur(14px)` 반투명 카드, 둥근 모서리 |
| **로또 구슬** | `radial-gradient` 3D 구체 효과 + `::before` 반사광 하이라이트 |
| **라이트/다크 테마** | CSS 변수 기반 완전 대응 |

### 모바일 최적화

- **하단 내비게이션** — SVG 아이콘 + 활성 인디케이터 점, 글라스 배경
- **터치 타겟** — 버튼·입력 최소 44px (WCAG 2.5.5 준수)
- **폼 레이아웃** — 라벨 위 / 입력+버튼 한 행 그리드 구조
- **빈도 그리드** — 모바일 6컬럼 · 소형 5컬럼, 구슬 아래 출현 횟수 표시
- **회차 목록** — 카드형 행 레이아웃
- **반응형 타이포그래피** — `clamp()` 기반 유동형 폰트 크기

---

## 프로파일

| 프로파일 | DB 호스트 | API 클라이언트 | 자동 수집 | 이력 초기화 |
|---|---|---|:---:|:---:|
| **`local`** *(기본값)* | `localhost` | `mock` | — | — |
| **`prod`** | `mariadb` *(컨테이너)* | real | ✓ | 설정 가능 |

---

## 빠른 시작

### 사전 요건

- **Java** 25+
- **Docker** + Docker Compose
- *(E2E 테스트)* **Node.js** 18+

### 로컬 개발 환경

```bash
# 1. MariaDB 컨테이너 기동
docker compose -f docker-compose.local.yml up -d

# 2. 애플리케이션 실행
./gradlew bootRun
```

> [http://localhost:8080](http://localhost:8080) 에서 접근

### 전체 스택 (Docker Compose)

```bash
# KRAFT_DB_USER, KRAFT_DB_PASSWORD, KRAFT_SECURITY_OPS_REQUIRED_TOKEN 설정 필요
docker compose up -d --build
```

---

## 환경 변수

| 변수 | 필수 | 설명 |
|------|:---:|------|
| `KRAFT_DB_USER` | **항상** | 데이터베이스 사용자 |
| `KRAFT_DB_PASSWORD` | **항상** | 데이터베이스 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | **운영** | `/ops/**` Bearer 토큰 |
| `SPRING_PROFILES_ACTIVE` | 컨테이너 | `local` 또는 `prod` |
| `KRAFT_API_CLIENT` | — | `mock` / `smok` / `real` *(기본: `mock`)* |
| `KRAFT_COLLECT_AUTO_ENABLED` | — | 자동 수집 스케줄러 활성화 *(기본: `true`)* |

> 나머지 변수는 `application.yml` 에 기본값이 정의되어 있습니다.

---

## 빌드 및 테스트

```bash
# 단위 + 통합 테스트
./gradlew test

# 성능 스모크 테스트
./gradlew performanceSmokeTest

# 전체 검사 (Checkstyle + SpotBugs + 테스트 + JaCoCo)
./gradlew check

# 엄격 정적 분석
./gradlew check -PstrictStatic=true

# 엄격 커버리지 (라인 76%, 브랜치 59%, 메서드 80%, 클래스 90%)
./gradlew check -PstrictCoverage=true

# E2E 테스트 (Playwright — Docker 내부에서만 실행)
docker run --rm -it -v "$PWD:/work" -w /work \
  mcr.microsoft.com/playwright:v1.60.0-noble \
  bash -lc "npm ci && npx playwright install chromium && npm run test:e2e"

# UTF-8 인코딩 검증
python scripts/check_utf8.py
```

---

## 주요 엔드포인트

| Method | 경로 | 설명 |
|:---:|---|---|
| `GET`  | `/` | 메인 페이지 |
| `GET`  | `/fragments/recommend` | HTMX 번호 추천 프래그먼트 |
| `GET`  | `/fragments/frequency` | HTMX 빈도 통계 프래그먼트 |
| `GET`  | `/fragments/rounds` | HTMX 회차 목록 프래그먼트 |
| `GET`  | `/actuator/health` | 헬스체크 |
| `POST` | `/ops/collect` | 수동 당첨 번호 수집 트리거 |
| `GET`  | `/ops/circuit-breakers` | 서킷브레이커 상태 조회 |
| `GET`  | `/ops/fetch-logs` | 수집 로그 조회 |
| `GET`  | `/swagger-ui.html` | API 문서 *(local 프로파일만)* |

> **Ops 엔드포인트**는 IP 허용 목록 + Bearer 토큰 인증이 필요합니다.

---

## 보안

| 항목 | 내용 |
|---|---|
| **Rate Limit** | IP 당 분당 120 요청 *(토큰 버킷, Caffeine 기반)* |
| **Ops 접근 제어** | IP 허용 목록 + Bearer 토큰 |
| **Actuator 접근 제어** | IP 허용 목록 |
| **보안 헤더** | CSP · HSTS · X-Frame-Options · Referrer-Policy · Permissions-Policy |

---

## 스케줄러

| 스케줄러 | 실행 시각 *(Asia/Seoul)* | 설명 |
|---|---|---|
| `WinningNumberAutoCollectScheduler` | **토 22:30** · **일 07:00** | 최신 회차 당첨 번호 수집 |
| `LottoFetchLogRetentionScheduler` | **매일 03:30** | 오래된 수집 로그 정리 *(기본 90일 보관)* |

---

## 라이선스

저장소 루트의 [`LICENSE`](LICENSE) 파일을 참고하세요.

<div align="center">
<sub>kLo · Lotto Recommender</sub>
</div>
