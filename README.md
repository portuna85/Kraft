# KRAFT Lotto

KRAFT Lotto는 한국 로또 6/45 데이터를 수집, 저장, 분석, 조회하는 Spring Boot 기반 웹 애플리케이션입니다. 최신 회차, 회차별 이력, 번호 통계, 조합 분석, 추천 보조 기능, 뉴스, 관리자 운영 기능을 제공합니다.

이 프로젝트는 당첨을 보장하지 않습니다. 추천 기능은 과거 데이터와 명시적인 제외 규칙을 기반으로 한 분석 도구입니다.

## 주요 기능

- 외부 원천에서 로또 당첨번호를 수집합니다.
- MariaDB에 회차별 당첨번호와 당첨 판매점 정보를 저장합니다.
- Flyway로 운영 DB 스키마를 관리합니다.
- 1등/2등 당첨 판매점을 수집하고 출처를 기록합니다.
- 판매점 출처는 `dhlottery`, `relay`, `public-data`, `manual` 중 하나로 저장됩니다.
- 최신 회차, 회차 목록, 번호 빈도, 통계, 조합 분석, 동반 번호, 뉴스 페이지를 제공합니다.
- 관리자 화면과 Ops API로 수집, 모니터링, 캐시, 뉴스 검수, 감사 로그를 관리합니다.
- Actuator readiness/health 엔드포인트로 컨테이너 상태를 확인합니다.
- GitHub Actions로 테스트, 정적 분석, 커버리지, Docker 이미지 발행, 보안 스캔, 배포를 자동화합니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Java 25 |
| 프레임워크 | Spring Boot 4.0.6 |
| 웹 | Spring MVC, Thymeleaf, HTMX, 정적 CSS/JS |
| 보안 | Spring Security, Ops 토큰 필터, IP allowlist, 보안 헤더 |
| 데이터 | MariaDB, Spring Data JPA, Flyway |
| 캐시 | Caffeine |
| 스케줄 | Spring Scheduler, ShedLock |
| 관측성 | Actuator, Micrometer, Prometheus, Grafana, Alertmanager, OpenTelemetry bridge |
| 테스트 | JUnit 5, Spring Test, H2, Testcontainers, Playwright, axe-core |
| 빌드 | Gradle Kotlin DSL, npm, PostCSS |
| 배포 | Docker Compose, GHCR 이미지, GitHub Actions CI/CD |

## 공개 페이지

| 경로 | 설명 |
|---|---|
| `/` | 홈 화면, 추천/최신 회차/빈도/회차 카드 |
| `/latest` | 최신 회차 상세와 판매점 수집 상태 |
| `/rounds` | 회차 목록과 회차 검색 |
| `/frequency` | 번호 빈도 통계 |
| `/stats` | 통계 요약 |
| `/analysis` | 번호 조합 분석 |
| `/companion` | 동반 출현 번호 분석 |
| `/news` | 로또 관련 뉴스 |
| `/methodology` | 추천 방식과 규칙 설명 |
| `/data-source` | 데이터 출처 설명 |
| `/faq` | FAQ |
| `/responsible-play` | 책임 있는 이용 안내 |
| `/privacy` | 개인정보 처리방침 |
| `/terms` | 이용약관 |
| `/contact` | 문의 |

## 공개 API

| 엔드포인트 | 설명 |
|---|---|
| `GET /api/lotto/draws/{drawNo}/winning-stores` | 회차별 당첨 판매점 목록. `grade`로 1등/2등 필터 가능 |
| `GET /api/lotto/draws/{drawNo}/winning-regions` | 회차별 당첨 판매점 지역 집계 |

## 관리자 및 Ops 기능

| 경로 | 설명 |
|---|---|
| `/admin/login` | 관리자 로그인 |
| `/admin/ops` | 관리자 대시보드 |
| `/admin/ops/collection` | 회차/판매점 수집과 판매점 수동 입력 |
| `/admin/ops/news` | 뉴스 승인, 거부, 도메인/키워드 차단 |
| `/admin/ops/cache` | 캐시 관리 |
| `/admin/ops/audit` | 관리자 감사 로그 |
| `/admin/ops/system` | 시스템 정보 |
| `/ops/collect` | 최신 회차까지 당첨번호 수집 |
| `/ops/collect/missing` | 누락 회차 수집 |
| `/ops/collect/refresh?round={n}` | 특정 회차 강제 재수집 |
| `/ops/collect/stores?round={n}` | 특정 회차 당첨 판매점 수집 |
| `/ops/collect/stores/backfill-regions` | 기존 판매점 주소에서 시도/시군구 재파싱 |
| `/ops/fetch-logs/**` | 수집 실패 로그 API |
| `/ops/circuit-breakers` | 외부 API Circuit Breaker 상태 |
| `/ops/data-freshness` | 데이터 최신성 상태 |
| `/ops/recommend/stats` | 추천 기능 통계 |
| `/ops/news/collect` | 뉴스 수집 트리거 |

`/ops/**`는 IP allowlist와 `X-Ops-Token` 헤더로 보호됩니다. `KRAFT_SECURITY_OPS_REQUIRED_TOKEN`이 설정되어 있으면 토큰 검증이 활성화됩니다.

## 데이터 수집 구조

### 당첨번호 수집

당첨번호 클라이언트는 `kraft.api.client` 값으로 선택됩니다.

| 값 | 설명 |
|---|---|
| `mock` | 로컬 개발용 mock 클라이언트 |
| `dhlottery`, `real` | 동행복권 API 클라이언트 |
| `smok` | Smok API 클라이언트 |
| `public-data` | 공공데이터포털 API 클라이언트 |

`kraft.api.fallback-client`를 설정하면 `CompositeLottoApiClient`가 primary/fallback 체인을 구성합니다. primary 실패 시 fallback을 시도하고, primary 응답의 2등 정보가 비어 있으면 fallback 데이터로 보충할 수 있습니다.

### 당첨 판매점 수집

판매점 수집 우선순위는 다음과 같습니다.

1. `KRAFT_API_STORE_RELAY_URL`이 있으면 `RelayStoreApiClient`를 primary로 사용합니다.
2. 없으면 `DhLotteryStoreApiClient`가 동행복권을 직접 호출합니다.
3. `PUBLIC_DATA_API_KEY`가 있으면 primary를 `CompositeWinningStoreApiClient`로 감싸고 `PublicDataStoreApiClient`를 fallback으로 사용합니다.

운영 서버에서 relay를 사용하지 않는다면 `PUBLIC_DATA_API_KEY` 설정이 중요합니다. 동행복권이 JSON 대신 HTML을 반환하면 판매점 수집 결과가 `saved:false`가 될 수 있고, 이때 공공데이터포털 fallback이 필요합니다.

자동 수집 스케줄:

| 대상 | 기본 스케줄 |
|---|---|
| 당첨번호 | 토요일 22:00, 일요일 06:00 |
| 당첨 판매점 | 토요일 21:15, 토요일 22:00, 일요일 06:00, 매일 00:10 |

## 저장소 구조

```text
src/main/java/com/kraft/lotto
  feature/
    admin/          관리자 화면, 감사 로그, 뉴스 검수
    news/           뉴스 수집, 분류, 조회
    recommend/      번호 추천 엔진과 제외 규칙
    statistics/     빈도, 패턴, 동반 번호 통계
    winningnumber/  당첨번호/판매점 수집, 저장, 조회
  infra/config/     설정 바인딩, 시작 검증, Flyway, 보안 구성
  support/          공통 필터, 예외 처리, IP allowlist, 로그 유틸리티
  web/              공개 컨트롤러와 Ops API 컨트롤러

src/main/resources
  db/migration/     Flyway 마이그레이션
  static/           CSS, JavaScript, 이미지, vendor 자산
  templates/        Thymeleaf 템플릿

src/test/java        단위, 슬라이스, 통합, 도메인 테스트
tests/e2e            Playwright 스모크/접근성 테스트
scripts              운영, 배포, 백업, 검증 스크립트
docker               Prometheus, Grafana, Caddy, Alertmanager 설정
```

## 로컬 개발

### 요구 사항

- Java 25
- Docker 및 Docker Compose
- Node.js 22 권장
- Windows는 PowerShell 기준, Linux/macOS는 Bash 기준

### 환경 파일 준비

PowerShell:

```powershell
Copy-Item .env.local.example .env
```

Bash:

```bash
cp .env.local.example .env
```

최소한 아래 값을 채웁니다.

```text
KRAFT_DB_NAME
KRAFT_DB_USER
KRAFT_DB_PASSWORD
KRAFT_DB_ROOT_PASSWORD
```

로컬 개발에서는 보통 외부 API 없이 실행할 수 있도록 다음 값을 사용합니다.

```text
KRAFT_API_CLIENT=mock
```

### 로컬 DB 실행

PowerShell 또는 Bash:

```bash
docker compose -f docker-compose.local.yml up -d
```

### 애플리케이션 실행

PowerShell:

```powershell
.\gradlew.bat bootRun
```

Bash:

```bash
./gradlew bootRun
```

접속:

```text
http://localhost:8080
```

헬스 체크:

```text
http://localhost:8080/actuator/health
```

Swagger UI는 `local` 프로필에서만 활성화됩니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 테스트

백엔드 테스트:

```powershell
.\gradlew.bat test
```

전체 백엔드 검증:

```powershell
.\gradlew.bat check
```

엄격 정적 분석 및 커버리지 검증:

```powershell
.\gradlew.bat check -PstrictStatic=true -PstrictCoverage=true
```

JavaScript 구문 검사:

```powershell
npm run check:js
```

Playwright E2E:

```powershell
npm run test:e2e
```

CSS 빌드:

```powershell
npm run build:css
```

## 운영 설정

운영은 `docker-compose.yml` 기반으로 실행됩니다.

주요 서비스:

- `app`
- `mariadb`
- `prometheus`
- `alertmanager`
- `grafana`
- `node-exporter`
- 선택적 `caddy` TLS reverse proxy profile

필수 운영 Secret:

```text
KRAFT_DB_NAME
KRAFT_DB_USER
KRAFT_DB_PASSWORD
KRAFT_DB_ROOT_PASSWORD
KRAFT_SECURITY_OPS_REQUIRED_TOKEN
ALERTMANAGER_DISCORD_WEBHOOK_URL
```

권장 운영 Secret/변수:

```text
KRAFT_ADMIN_ENABLED=true
KRAFT_ADMIN_PASSWORD=<관리자 비밀번호>
PUBLIC_DATA_API_KEY=<공공데이터포털 서비스키>
KRAFT_PUBLIC_DATA_BASE_URL=https://apis.data.go.kr
```

배포 스크립트는 GitHub Environment Secrets에서 서버 `.env`를 렌더링합니다. 새 Secret을 컨테이너 안에서 사용해야 한다면 배포 스크립트가 해당 값을 `.env`에 쓰는지 반드시 확인해야 합니다.

## CI/CD

GitHub Actions workflow:

- `CI`: Java 빌드, 테스트, 커버리지, CSS 빌드 검증, JavaScript 구문 검사, Playwright 스모크 테스트, 정적 분석, Docker 이미지 발행, 보안 스캔.
- `CD - Deploy to kraft-server`: `main`의 CI 성공 후 GHCR 이미지를 받아 Docker Compose 서비스를 재시작하고 readiness/smoke test를 수행합니다.
- `CodeQL`: 코드 스캔.

CI는 `README.md` 또는 `docs/**`만 변경된 push는 무시합니다.

## 보안 메모

- `.env`와 비밀값은 커밋하지 않습니다.
- 관리자 화면은 Spring Security로 보호됩니다.
- Ops API는 IP allowlist와 토큰 검증으로 보호됩니다.
- 공개 reverse proxy는 `/admin*`, `/ops*`, `/actuator*`를 공개 도메인에서 차단합니다.
- 운영 프로필에서는 Swagger UI를 비활성화합니다.
- 보안 헤더와 rate limit은 `kraft.security` 아래에서 설정합니다.

## Git 정책

- `main`이 운영 브랜치입니다.
- `docs/**` 문서는 Git 추적 대상이 아닙니다.
- 기본적으로 GitHub에 게시하는 문서는 `README.md`만 허용합니다.
- 커밋 전 `.env`, 운영 문서, 비밀값이 스테이징되지 않았는지 확인합니다.

## 라이선스

`LICENSE` 파일을 확인하세요.
