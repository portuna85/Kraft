# kLo 로컬 개발 기준 분석서

이 문서는 `local` 프로파일을 기준으로 `kLo` 저장소를 설명한다. 운영 배포보다 로컬 실행, 로컬 설정, 로컬 테스트, 코드 구조 파악을 우선한다.

## 1. 로컬 기준 요약

| 항목 | 로컬 기준 |
| --- | --- |
| 애플리케이션 | `kraft-lotto` |
| 기본 패키지 | `com.kraft.lotto` |
| 런타임 | Java 25 |
| 프레임워크 | Spring Boot 4, Spring MVC, Thymeleaf, Spring Data JPA |
| 로컬 DB | MariaDB 11.7 Docker container |
| 테스트 DB | H2, Testcontainers MariaDB |
| 기본 프로파일 | `local` |
| 로컬 API 클라이언트 | `application-local.yml` 기본값은 `smok`, `.env.local.example`은 오프라인 개발을 위해 `mock`으로 오버라이드 |
| 자동 수집 | 로컬에서 기본 비활성화 |
| 화면 | Thymeleaf, HTMX, Bootstrap, 정적 CSS/JS |
| 검증 | JUnit 5, Spring Boot Test, Playwright, Checkstyle, SpotBugs, JaCoCo |

로컬 실행은 다음 세 파일을 기준으로 한다.

| 파일 | 역할 |
| --- | --- |
| `.env.local.example` | 로컬 `.env` 작성 기준 |
| `docker-compose.local.yml` | 로컬 MariaDB 단독 실행 |
| `src/main/resources/application-local.yml` | 로컬 Spring profile override |

## 2. 로컬 빠른 시작

### 2.1 사전 준비

| 도구 | 용도 |
| --- | --- |
| Java 25 | Spring Boot 애플리케이션 실행 |
| Docker, Docker Compose | 로컬 MariaDB 실행 |
| Node.js 18 이상 | Playwright E2E 실행 시 필요 |

### 2.2 로컬 환경 파일 생성

```bash
cp .env.local.example .env
```

`.env.local.example`의 DB 계정 값을 로컬 MariaDB에 맞게 수정한다.

```dotenv
SPRING_PROFILES_ACTIVE=local
KRAFT_DB_NAME=kraft_lotto
KRAFT_DB_HOST=localhost
KRAFT_DB_PORT=3306
KRAFT_DB_USER=your-local-db-user
KRAFT_DB_PASSWORD=your-local-db-password
KRAFT_DB_ROOT_PASSWORD=your-local-root-password
KRAFT_API_CLIENT=mock
KRAFT_COLLECT_AUTO_ENABLED=false
```

로컬에서 외부 API 호출 없이 개발하려면 `KRAFT_API_CLIENT=mock`을 유지한다. 실제에 가까운 smok 클라이언트를 쓰려면 `KRAFT_API_CLIENT=smok`으로 바꾼다.

### 2.3 로컬 DB 실행

```bash
docker compose -f docker-compose.local.yml up -d
```

로컬 compose는 `kraft-lotto-local-mariadb` 컨테이너만 실행한다. 포트는 기본 `127.0.0.1:3306`이다.

### 2.4 애플리케이션 실행

```bash
./gradlew bootRun
```

접속 주소는 다음과 같다.

| 주소 | 용도 |
| --- | --- |
| `http://localhost:8080` | 공개 화면 |
| `http://localhost:8080/news` | 뉴스 |
| `http://localhost:8080/frequency` | 번호 빈도 |
| `http://localhost:8080/rounds` | 회차 조회 |
| `http://localhost:8080/actuator/health` | 로컬 health |
| `http://localhost:8080/swagger-ui/index.html` | local profile에서 활성화되는 Swagger UI |

## 3. 로컬 프로파일 동작

`src/main/resources/application-local.yml`은 로컬 개발 편의를 위해 다음 정책을 적용한다.

| 설정 | 로컬 동작 |
| --- | --- |
| DB URL | `localhost:${KRAFT_DB_PORT:3306}`의 MariaDB 사용 |
| Thymeleaf cache | 비활성화 |
| 정적 리소스 cache | no-store |
| Hibernate SQL format | 활성화 |
| Flyway validate | 완화 |
| Actuator health detail | 항상 표시 |
| Springdoc API docs | 활성화 |
| 자동 수집 | 비활성화 |
| API client | 기본 `smok`, `.env.local.example`은 `mock` 권장 |
| backfill delay | 300ms |

로컬에서 데이터 수집을 테스트할 때는 자동 스케줄러보다 Ops API 또는 테스트 코드를 사용하는 편이 안전하다. 자동 수집은 운영 흐름에 가깝고, 로컬에서는 의도하지 않은 외부 호출을 만들 수 있다.

## 4. 저장소 구조

```text
.
├── build.gradle.kts
├── docker-compose.local.yml
├── src/main/java/com/kraft/lotto
│   ├── feature
│   ├── infra
│   ├── support
│   └── web
├── src/main/resources
│   ├── application.yml
│   ├── application-local.yml
│   ├── db/migration
│   ├── static
│   └── templates
├── src/test/java/com/kraft/lotto
├── scripts
└── tests/e2e
```

| 영역 | 분석 |
| --- | --- |
| `feature` | 로또 도메인 기능을 당첨번호, 추천, 통계, 뉴스로 분리한다. |
| `infra` | Spring 설정, 환경 검증, Clock, Async, Flyway, ShedLock, health를 담당한다. |
| `support` | 예외, 필터, 캐시, 로그 정화, IP allowlist 같은 공통 인프라다. |
| `web` | 공개 화면과 운영 API 컨트롤러다. |
| `resources/templates` | Thymeleaf 페이지와 HTMX fragment다. |
| `resources/static` | CSS, JS, PWA manifest, vendored frontend asset이다. |
| `scripts` | 로컬 검증, DB 백업/복원, 배포 보조 스크립트다. |
| `tests/e2e` | Playwright 브라우저 테스트다. |

## 5. 핵심 도메인 분석

### 5.1 당첨번호 수집

| 파일군 | 역할 |
| --- | --- |
| `DhLotteryApiClient` | 동행복권 API 호출 |
| `DhLotteryResponseParser` | 응답 JSON 검증 및 `WinningNumber` 변환 |
| `ApiRetrySupport` | API 재시도 정책 |
| `ApiCircuitBreaker` | 외부 API 장애 차단 |
| `LottoSingleDrawCollector` | 단일 회차 수집 |
| `LottoRangeCollector` | 범위 수집 |
| `LottoCollectionCommandService` | 수집 명령 진입점과 실행 상태 관리 |
| `WinningNumberPersister` | 수집 결과 저장 |
| `WinningNumberUpsertExecutor` | 기존 회차 갱신 또는 신규 저장 |
| `WinningNumberQueryService` | 공개 조회 |
| `LottoFetchLogQueryService` | 실패 로그 조회 |
| `WinningStoreCollector` | 당첨 판매점 수집 |

로컬에서는 `mock` API client를 사용하면 외부 호출 없이 수집/조회 흐름을 확인할 수 있다.

### 5.2 추천

| 파일군 | 역할 |
| --- | --- |
| `RecommendService` | 추천 요청 검증 및 추천 실행 |
| `RecommendFilter` | 홀수 개수와 합계 범위 필터 검증 |
| `LottoRecommender` | 생성기와 제외 규칙을 조합해 추천 번호 생산 |
| `ConstraintAwareLottoNumberGenerator` | 편향 완화 조건을 포함한 번호 생성 |
| `ExclusionRule` 구현체 | 생일 편향, 등차수열, 연속 번호, 십의 자리 집중, 과거 당첨 조합 제외 |
| `PastWinningCache` | 과거 당첨 조합 메모리 캐시 |
| `RecommendMetricsRecorder` | 추천 요청 수, 실패 이유, 지연시간 기록 |

추천은 확률 향상 기능이 아니다. 흔한 조합과 편향 조합을 줄이는 추천 엔진이다.

### 5.3 통계

| 파일군 | 역할 |
| --- | --- |
| `WinningStatisticsCacheService` | 번호 빈도, 기간별 빈도, 동반 번호, 패턴, 조합 당첨 이력 계산 |
| `WinningStatisticsService` | 통계 facade와 캐시 eviction |
| `CombinationPrizeHistoryKeyGenerator` | 정렬된 번호 기반 cache key 생성 |
| `WinningNumberFrequencySummaryEntity` | 번호 빈도 요약 테이블 |

통계 캐시는 Caffeine을 사용하며 `CacheConfig`에서 Micrometer cache metric으로 바인딩된다.

### 5.4 뉴스

| 파일군 | 역할 |
| --- | --- |
| `NewsRssClient` | RSS XML fetch/parse, XXE 방어 |
| `NewsCollectionService` | RSS 기사 수집, 중복 skip |
| `NewsArticlePersister` | 기사별 별도 트랜잭션 저장 |
| `NewsQueryService` | page/size 검증 후 목록 조회 |
| `NewsArticleEntity` | 뉴스 JPA 엔티티 |

로컬에서는 RSS URL을 바꾸거나 `NewsRssClientTest`를 통해 파싱 동작을 확인할 수 있다.

## 6. Web 계층

| 파일 | 역할 |
| --- | --- |
| `HomeController` | 홈 화면, 추천 fragment, 빈도 fragment, 회차 fragment |
| `StatsController` | 통계 화면 |
| `AnalysisController` | 선택 조합 분석 |
| `CompanionController` | 동반 번호 화면 |
| `LatestRoundController` | 최신 회차 조회 |
| `NewsController` | 뉴스 화면 |
| `OpsCollectionController` | 수동 수집 운영 API |
| `OpsFetchLogController` | fetch 실패 로그 운영 API |
| `OpsMonitoringController` | 서킷브레이커와 추천 메트릭 운영 API |
| `OpsNewsController` | 뉴스 수동 수집 운영 API |
| `OpsPageController` | 운영 HTML 페이지 |
| `GlobalExceptionHandler` | 공개 화면 오류 처리 |
| `OpsExceptionHandler` | 운영 API 오류 처리 |

로컬 profile에서는 Swagger UI가 켜져 있어 API 탐색이 쉽다. 운영성 API는 `OpsAccessFilter`의 IP allowlist와 선택적 token 검증을 통과해야 한다.

## 7. 로컬 DB 스키마

Flyway baseline은 `src/main/resources/db/migration/V1__baseline.sql`이다.

| 테이블 | 역할 |
| --- | --- |
| `winning_numbers` | 회차별 당첨 번호, 1등/2등 정보, 원본 JSON, version |
| `lotto_fetch_logs` | 수집 성공/실패/스킵 로그 |
| `shedlock` | 스케줄러 분산 잠금 |
| `winning_number_frequency_summary` | 번호 빈도 요약 |
| `news_articles` | RSS 뉴스 기사 |
| `winning_stores` | 회차별 당첨 판매점 |

로컬 DB를 완전히 초기화하려면 Docker volume을 삭제해야 한다.

```bash
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
```

## 8. 로컬 테스트

### 8.1 전체 테스트

```bash
./gradlew test
```

### 8.2 정적 분석 포함

```bash
./gradlew check
```

### 8.3 엄격 모드

```bash
./gradlew check -PstrictStatic=true -PstrictCoverage=true
```

### 8.4 성능 스모크

```bash
./gradlew performanceSmokeTest
```

### 8.5 환경변수 drift 검사

```bash
python3 scripts/check_env_drift.py
```

### 8.6 E2E

```bash
npm ci
npx playwright install chromium
npm run test:e2e
```

## 9. 로컬 스크립트 분석

| 스크립트 | 로컬 용도 |
| --- | --- |
| `scripts/check_env_drift.py` | `application.yml`의 `KRAFT_*` 키가 `.env.example`에 있는지 확인 |
| `scripts/check_utf8.py` | 파일 UTF-8 검증 |
| `scripts/add-winning-number.sh` | 수동 당첨번호 추가/호출 보조 |
| `scripts/db-backup.sh` | DB 백업 |
| `scripts/db-restore.sh` | DB 복원 |
| `scripts/e2e-docker.sh` | Docker 기반 E2E 실행 |
| `scripts/flyway-reset-history.sql` | Flyway history 조정 |
| `scripts/server-hardening.sh` | 서버 보안 설정 보조 |
| `scripts/install-git-hooks.ps1` | Windows Git hook 설치 |

배포 스크립트는 `scripts/deploy` 아래에 있지만 로컬 개발 필수 경로는 아니다.

## 10. 로컬에서 자주 보는 설정

| 환경변수 | 의미 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=local` | local profile 활성화 |
| `KRAFT_DB_URL` | 로컬 MariaDB JDBC URL |
| `KRAFT_DB_USER` | 로컬 DB 사용자 |
| `KRAFT_DB_PASSWORD` | 로컬 DB 비밀번호 |
| `KRAFT_DB_ROOT_PASSWORD` | 로컬 MariaDB root 비밀번호 |
| `KRAFT_API_CLIENT=mock` | 외부 API 없이 개발 |
| `KRAFT_API_CLIENT=smok` | mock보다 실제 흐름에 가까운 client |
| `KRAFT_HISTORY_INIT_ENABLED=false` | 시작 시 이력 초기화를 끔 |
| `KRAFT_COLLECT_AUTO_ENABLED=false` | 자동 수집을 끔 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | 운영 API token |

## 11. 로컬 문제 해결

| 증상 | 확인 지점 |
| --- | --- |
| DB 연결 실패 | `.env`의 `KRAFT_DB_*`, `docker compose -f docker-compose.local.yml ps`, MariaDB health |
| Flyway 오류 | 로컬 volume에 오래된 schema가 남아 있는지 확인 |
| 추천이 오래 걸림 | `KRAFT_RECOMMEND_*_MAX_ATTEMPTS`, 제외 규칙, past winning cache 상태 |
| 수집이 외부 API를 호출함 | `.env`의 `KRAFT_API_CLIENT`가 `mock`인지 확인 |
| Ops API 401/403 | `KRAFT_SECURITY_OPS_REQUIRED_TOKEN`, allowlist, request IP |
| 정적 리소스가 갱신되지 않음 | local profile인지 확인. local은 정적 cache no-store |
| 환경변수 누락 | `python3 scripts/check_env_drift.py` 실행 |

## 12. 로컬 개발 시 변경 주의점

1. `application.yml`에 새 `KRAFT_*` 키를 추가하면 `.env.example`도 갱신한다.
2. 로컬 설정은 `application-local.yml`에 둔다. 운영 설정과 섞지 않는다.
3. 뉴스 저장은 `NewsArticlePersister`의 별도 트랜잭션에 의존한다.
4. 당첨번호 갱신 필드가 바뀌면 `WinningNumberEntity`, mapper, upsert snapshot을 함께 확인한다.
5. 외부 API 실패 사유를 추가하면 `LottoApiClientException.FailureReason`과 실패 로그 조회도 같이 본다.
6. 캐시 이름을 바꾸면 `CacheConfig`, 통계 서비스, 테스트, 대시보드 metric 이름을 같이 확인한다.
7. 로컬에서 자동 수집을 켤 때는 중복 수집과 외부 API 호출량을 확인한다.

## 13. 현재 Markdown 정책

루트 Markdown은 이 `README.md` 하나만 유지한다. 별도 작업 문서나 임시 Markdown은 만들지 않는다.
