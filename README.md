# KRAFT Lotto

KRAFT Lotto는 로또 6/45 데이터를 수집하고, 회차 이력 조회, 통계 분석, 번호 추천, 뉴스 검수 기능을 제공하는 Spring Boot 기반 웹 애플리케이션이다.

이 프로젝트는 당첨 보장을 주장하지 않는다. 과거 데이터와 명시적인 제외 규칙을 바탕으로 탐색형 도구를 제공한다.

## 개요

- 공개 웹 기능
  - 최신 회차 조회
  - 전체 회차 조회
  - 번호 빈도 통계
  - 번호 조합 분석
  - 동반 출현 분석
  - 추천 번호 생성
  - 뉴스 목록
  - 방법론, 데이터 출처, FAQ, 책임 있는 이용 안내
- 운영 기능
  - `/ops/**` 운영 API
  - `/admin/login`, `/admin/ops/**` 관리자 화면
  - 수집 실패 로그 조회
  - 뉴스 승인, 거부, 차단
  - 최신 회차 및 판매점 수집 실행
- 운영 보조 기능
  - Flyway 마이그레이션
  - Prometheus, Grafana, Alertmanager
  - Caddy 리버스 프록시
  - Playwright E2E

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| 언어 | Java 25 |
| 프레임워크 | Spring Boot 4.0.6 |
| 웹 | Spring MVC, Thymeleaf, HTMX |
| 보안 | Spring Security |
| 데이터 | Spring Data JPA, Flyway, MariaDB |
| 캐시 | Caffeine |
| 스케줄/락 | ShedLock |
| 관측성 | Micrometer, Prometheus, OpenTelemetry |
| 테스트 | JUnit 5, Spring Test, Testcontainers, Playwright |
| 빌드 | Gradle Kotlin DSL |
| 프런트 자산 | Bootstrap, 정적 CSS/JS, PostCSS |

## 주요 경로

### 공개 페이지

| 경로 | 설명 |
| --- | --- |
| `/` | 홈, 추천 카드 포함 |
| `/latest` | 최신 회차 정보 |
| `/rounds` | 회차 목록과 회차별 조회 |
| `/frequency` | 번호 빈도 통계 |
| `/stats` | 통계 요약 |
| `/analysis` | 번호 조합 분석 |
| `/companion` | 동반 출현 분석 |
| `/news` | 로또 관련 뉴스 |
| `/methodology` | 추천 규칙 설명 |
| `/data-source` | 데이터 수집 현황과 출처 |
| `/faq` | FAQ |
| `/responsible-play` | 책임 있는 이용 안내 |
| `/privacy` | 개인정보 처리방침 |
| `/terms` | 이용약관 |
| `/contact` | 문의 |

### 관리자 및 운영 경로

| 경로 | 설명 |
| --- | --- |
| `/admin/login` | 관리자 로그인 페이지 |
| `/admin/ops` | 수집 실패 대시보드 |
| `/admin/ops/collection` | 회차/판매점 수집 실행 |
| `/admin/ops/news` | 뉴스 승인/거부/차단 |
| `/admin/ops/audit` | 관리자 감사 로그 |
| `/ops/collect/**` | 운영 수집 API |
| `/ops/fetch-logs/**` | 수집 실패 로그 API |
| `/ops/data-freshness` | 최신 데이터 신선도 API |
| `/ops/circuit-breakers` | 외부 API 상태 API |
| `/ops/recommend/stats` | 추천 통계 API |
| `/ops/news/collect` | 뉴스 수집 트리거 API |

## 번호 추천 규칙

추천 엔진은 조합을 생성한 뒤 아래 규칙을 바탕으로 제외 필터를 적용한다.

- `BirthdayBiasRule`
  - 모든 숫자가 31 이하인 조합 제외
- `LongRunRule`
  - 5개 이상 연속 숫자가 포함된 조합 제외
- `ArithmeticSequenceRule`
  - 등차수열 패턴 조합 제외
- `SingleDecadeRule`
  - 같은 십의 자리 숫자가 과도하게 몰린 조합 제외
- `PastWinningRule`
  - 과거 당첨 번호와 완전히 같은 조합 제외

규칙은 코드와 설정을 통해 조정되며, 예측 정확도를 보장하는 기능이 아니다.

## 저장소 구조

```text
src/main/java/com/kraft/lotto
  feature/
    admin/          관리자 뉴스/감사 기능
    news/           뉴스 수집, 분류, 조회
    recommend/      추천 엔진과 규칙
    statistics/     통계 집계 및 캐시
    winningnumber/  당첨번호/판매점 수집과 조회
  infra/config/     설정 바인딩, 검증, 시큐리티 구성
  support/          공통 필터, 예외 처리, IP/토큰 보안
  web/              공개 컨트롤러와 운영 API

src/main/resources
  templates/        Thymeleaf 템플릿
  static/           CSS, JS, 이미지, vendor 자산
  db/migration/     Flyway SQL

src/test
  java/             단위/통합 테스트
  resources/        테스트 설정

tests/e2e
  Playwright E2E
```

## 실행 환경

기본 프로필은 `local`이다.

주요 파일:

- `.env.local.example`
- `.env.prod.example`
- `docker-compose.local.yml`
- `docker-compose.yml`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-prod.yml`

### 필수 준비물

- Java 25
- Docker, Docker Compose
- Node.js 20 이상 권장

## 로컬 실행

### 1. 환경 파일 준비

PowerShell:

```powershell
Copy-Item .env.local.example .env
```

Bash:

```bash
cp .env.local.example .env
```

`.env`에서 최소한 아래 값을 채운다.

- `KRAFT_DB_USER`
- `KRAFT_DB_PASSWORD`
- `KRAFT_DB_ROOT_PASSWORD`

기본 로컬 예시는 외부 API 없이 개발할 수 있도록 `KRAFT_API_CLIENT=mock`를 사용한다.

### 2. 로컬 MariaDB 실행

PowerShell:

```powershell
docker compose -f docker-compose.local.yml up -d
```

Bash:

```bash
docker compose -f docker-compose.local.yml up -d
```

### 3. 애플리케이션 실행

PowerShell:

```powershell
.\gradlew.bat bootRun
```

Bash:

```bash
./gradlew bootRun
```

### 4. 접속 확인

- 앱: `http://localhost:8080`
- 헬스체크: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Swagger는 `local` 프로필에서만 활성화된다.

## 테스트

### 백엔드 테스트

PowerShell:

```powershell
.\gradlew.bat test
```

정적 분석 포함:

```powershell
.\gradlew.bat check
```

엄격 모드:

```powershell
.\gradlew.bat check -PstrictStatic=true -PstrictCoverage=true
```

성능 스모크 테스트:

```powershell
.\gradlew.bat performanceSmokeTest
```

### 프런트 자산 및 E2E

```powershell
npm ci
npm run check:js
npm run test:e2e
```

Playwright는 기본적으로 로컬 서버를 `18080` 포트에서 띄워 H2 메모리 DB로 테스트한다.

## 관리자 페이지 접속

현재 구현 기준 관리자 페이지는 폼 로그인 방식이다.

- 로그인 URL: `/admin/login`
- 보호 경로: `/admin/ops`, `/admin/ops/**`
- 계정 ID: `admin`
- 비밀번호: `KRAFT_ADMIN_PASSWORD`
- 활성화 조건: `KRAFT_ADMIN_ENABLED=true`

운영 환경에서는 공개 도메인에서 관리자 경로를 직접 열지 않고, 관리자 도메인 또는 SSH 터널을 통해 접근한다.

상세 절차는 [docs/admin-access.md](docs/admin-access.md)를 참고한다.

## 운영 배포 개요

`docker-compose.yml`은 아래 서비스를 포함한다.

- `app`
- `mariadb`
- `caddy`
- `prometheus`
- `alertmanager`
- `grafana`
- `node-exporter`

특징:

- `app`, `mariadb`는 localhost 바인딩으로 직접 노출을 줄인다.
- 공개 도메인에서는 `/actuator*`, `/ops*`, `/admin*`를 Caddy가 차단한다.
- 관리자 도메인에서는 `/admin*`와 정적 자산만 허용한다.
- 프로덕션 프로필에서는 Swagger가 비활성화된다.

## 보안 메모

- 운영 API `/ops/**`는 IP allowlist와 토큰 검증을 사용한다.
- 관리자 화면 `/admin/ops/**`는 Spring Security 인증을 사용한다.
- `robots.txt`는 `/admin`을 차단한다.
- 관리자 로그인 페이지는 `noindex, nofollow, noarchive` 메타를 포함한다.

자세한 내용은 [SECURITY.md](SECURITY.md)를 참고한다.

## 문서

- [SECURITY.md](SECURITY.md)
- [docs/admin-access.md](docs/admin-access.md)
- [docs/improvement.md](docs/improvement.md)
- [CLAUDE.md](CLAUDE.md)
