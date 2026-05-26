# kLo — 로또 번호 추천 서비스

한국 로또 6/45 당첨 번호를 수집·분석하고, 통계 기반 편향 제거 규칙을 적용한 번호 조합을 추천하는 Spring Boot 애플리케이션입니다.

## 주요 기능

- **번호 추천** — 생일 편향, 등차수열, 단일 십의 자리 집중, 장기 미출현, 과거 당첨 조합 등 5가지 ExclusionRule 을 통과한 조합을 추천
- **당첨 번호 수집** — 동행복권 API 에서 회차별 당첨 번호를 자동(토·일) 또는 수동(Ops API)으로 수집
- **번호 빈도 통계** — 전체 회차 누적 번호 출현 빈도 요약 제공
- **조합 이력 조회** — 특정 번호 조합의 과거 당첨 이력 조회
- **Ops 관리 API** — 수집 트리거, 서킷브레이커 상태, fetch 로그 조회 등 운영 엔드포인트 제공

## 기술 스택

| 분류 | 내용 |
|------|------|
| 런타임 | Java 25, Spring Boot 4.0.5 |
| 데이터베이스 | MariaDB 11.7 (운영), H2 (테스트) |
| 스키마 관리 | Flyway |
| 캐시 | Caffeine (번호 빈도, 이력, 통계 요약) |
| 분산 스케줄러 | ShedLock + JDBC |
| 프론트엔드 | Thymeleaf + HTMX + Bootstrap 5 |
| API 문서 | SpringDoc/OpenAPI (`/swagger-ui.html`, local 프로파일만) |
| 컨테이너 | Docker Compose |
| 테스트 | JUnit 5, jqwik (property-based), Testcontainers, Playwright (E2E) |
| 정적 분석 | Checkstyle, SpotBugs, JaCoCo |

## 아키텍처

```
com.kraft.lotto
├── feature/
│   ├── winningnumber/   당첨 번호 수집·조회 (API 클라이언트, 스케줄러, 서킷브레이커)
│   ├── recommend/       번호 추천 엔진 (ExclusionRule 전략 패턴)
│   └── statistics/      번호 빈도 통계 요약
├── infra/config/        프로퍼티 바인딩, 시작 시 검증
├── support/             공통: 필터(Rate Limit, Ops/Actuator 접근, 보안 헤더), 예외 처리
└── web/                 홈 컨트롤러, Ops 컨트롤러, SEO 컨트롤러
```

**피처 슬라이스 패키징**: 각 피처가 `application`, `domain`, `infrastructure`, `web` 하위 패키지를 소유합니다.

**ExclusionRule**: 추천 번호를 필터링하는 전략 인터페이스. 구현체는 Spring이 자동 수집하여 고정 순서로 평가합니다.

**ApiCircuitBreaker**: 동행복권 API 호출을 감싸는 직접 구현 서킷브레이커 (closed → open → half-open). `/ops/circuit-breakers` 로 상태 조회 가능.

## 프로파일

| 프로파일 | DB 호스트 | API 클라이언트 | 자동 수집 | 이력 초기화 |
|---------|----------|--------------|----------|-----------|
| `local` (기본값) | `localhost` | mock | 비활성 | 비활성 |
| `prod` | `mariadb` (컨테이너) | 실제 API | 활성 | 설정 가능 |

## 빠른 시작

### 사전 요건

- Java 25+
- Docker + Docker Compose
- (E2E 테스트) Node.js 18+

### 로컬 개발 환경

```bash
# 1. 환경 변수 파일 생성
cp .env.local.example .env
# .env 에서 KRAFT_DB_USER, KRAFT_DB_PASSWORD 등 실제 값으로 수정

# 2. MariaDB 컨테이너 기동
docker compose -f docker-compose.local.yml up -d

# 3. 애플리케이션 실행
./gradlew bootRun
```

애플리케이션이 기동되면 `http://localhost:8080` 에서 접근할 수 있습니다.

### 전체 스택 (Docker Compose)

```bash
cp .env.prod.example .env
# .env 에서 SPRING_PROFILES_ACTIVE=prod 및 DB 자격증명, OPS 토큰 설정

docker compose up -d --build
```

## 환경 변수

| 변수 | 필수 | 설명 |
|------|------|------|
| `KRAFT_DB_USER` | 항상 | 데이터베이스 사용자 |
| `KRAFT_DB_PASSWORD` | 항상 | 데이터베이스 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | 운영 | `/ops/**` 엔드포인트 Bearer 토큰 |
| `SPRING_PROFILES_ACTIVE` | 컨테이너 | `local` 또는 `prod` |
| `KRAFT_API_CLIENT` | — | `mock` / `smok` / `real` (기본: `mock`) |
| `KRAFT_COLLECT_AUTO_ENABLED` | — | 자동 수집 스케줄러 활성화 (기본: `true`) |

나머지 변수는 `application.yml` 에 기본값이 정의되어 있습니다. `.env.local.example` / `.env.prod.example` 파일을 참고하세요.

## 빌드 및 테스트

```bash
# 단위 + 통합 테스트 (@Tag("perf") 제외)
./gradlew test

# 성능 스모크 테스트
./gradlew performanceSmokeTest

# 전체 검사 (Checkstyle + SpotBugs + 테스트 + JaCoCo)
./gradlew check

# 엄격 정적 분석 (위반 시 빌드 실패)
./gradlew check -PstrictStatic=true

# 엄격 커버리지 검사 (라인 76%, 브랜치 59%, 메서드 80%, 클래스 90%)
./gradlew check -PstrictCoverage=true

# E2E 테스트 (Playwright — Docker 내부에서만 실행)
docker run --rm -it -v "$PWD:/work" -w /work mcr.microsoft.com/playwright:v1.60.0-noble \
  bash -lc "npm ci && npx playwright install chromium && npm run test:e2e"

# UTF-8 인코딩 검증
python scripts/check_utf8.py
```

## 주요 엔드포인트

| 경로 | 설명 |
|------|------|
| `GET /` | 메인 페이지 (번호 추천 + 빈도 통계) |
| `GET /fragments/recommend` | HTMX 번호 추천 프래그먼트 |
| `GET /fragments/frequency` | HTMX 빈도 통계 프래그먼트 |
| `GET /fragments/rounds` | HTMX 회차 목록 프래그먼트 |
| `GET /actuator/health` | 헬스체크 (readiness/liveness) |
| `POST /ops/collect` | 수동 당첨 번호 수집 트리거 |
| `GET /ops/circuit-breakers` | 서킷브레이커 상태 조회 |
| `GET /ops/fetch-logs` | 수집 로그 조회 |
| `GET /swagger-ui.html` | API 문서 (local 프로파일만) |

Ops 엔드포인트는 IP 허용 목록 + Bearer 토큰 인증이 필요합니다.

## 보안

- **Rate Limit**: IP당 분당 120 요청 (토큰 버킷, Caffeine 기반)
- **Ops 접근 제어**: IP 허용 목록 + Bearer 토큰
- **Actuator 접근 제어**: IP 허용 목록
- **보안 헤더**: CSP, HSTS, X-Frame-Options, Referrer-Policy, Permissions-Policy

## 스케줄러

| 스케줄러 | 실행 시각 | 설명 |
|---------|---------|------|
| `WinningNumberAutoCollectScheduler` | 토요일 22:30, 일요일 07:00 (Asia/Seoul) | 최신 회차 당첨 번호 수집 |
| `LottoFetchLogRetentionScheduler` | 매일 03:30 | 오래된 수집 로그 정리 (기본 90일 보관) |

## 라이선스

[LICENSE](LICENSE) 파일을 참고하세요.
