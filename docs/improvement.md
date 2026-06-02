# kLo 저장소 상세 분석 및 개선 제안서

- 대상 저장소: `https://github.com/portuna85/kLo.git`
- 기준 브랜치: `main`
- 작성일: 2026-06-02 (Asia/Seoul)
- 산출물: `improvement.md`

## 0. 분석 범위와 제한사항

현재 실행 환경에서 `git clone https://github.com/portuna85/kLo.git`은 DNS 해석 실패로 직접 수행하지 못했습니다. 대신 GitHub 웹/커넥터를 통해 기본 브랜치의 루트, README, Gradle, Spring 설정, Dockerfile, 핵심 Java 소스, 코드 검색 인덱스, 최근 PR 이력을 확인했습니다. 따라서 이 문서는 저장소 전체 구조와 핵심 실행 경로를 기준으로 한 정적 분석 보고서입니다.

확인한 주요 범위는 다음과 같습니다.

- 루트/빌드: `README.md`, `build.gradle.kts`, `Dockerfile`, Docker/CI 관련 구성
- 애플리케이션 설정: `src/main/resources/application.yml` 및 프로파일 기반 구성 방향
- 백엔드 핵심 패키지: `feature.recommend`, `feature.winningnumber`, `feature.statistics`, `feature.news`, `infra`, `support`, `web`
- 보안/운영: Security filter chain, Ops/API token filter, Rate limit, Actuator/Ops allowlist, 보안 헤더
- 수집/통계: 당첨번호 수집, 판매점 수집, 통계 캐시, native query 기반 집계
- 테스트/품질: JUnit, jqwik, Testcontainers, Playwright, Checkstyle, SpotBugs, JaCoCo 구성

## 1. 요약 평가

저장소는 단순 샘플 프로젝트가 아니라 운영을 염두에 둔 Spring Boot 기반 서비스 구조를 갖추고 있습니다. Java 25, Spring Boot 4.x, MariaDB/Flyway, Caffeine, ShedLock, Micrometer/Prometheus/OTLP, Docker Compose, 정적 분석과 커버리지 게이트까지 포함되어 있습니다.

강점은 다음과 같습니다.

1. **도메인 분리**: 추천, 당첨번호, 통계, 뉴스, 운영 웹/API가 패키지 단위로 구분되어 있습니다.
2. **운영 안전장치**: rate limit, Ops allowlist, Actuator allowlist, CSP/HSTS 등 운영 보안 요소가 반영되어 있습니다.
3. **수집 안정성 설계**: API client, circuit breaker, retry/backoff, ShedLock, fetch log retention 등 외부 API 장애를 고려한 구조입니다.
4. **관측 가능성**: Micrometer, Prometheus, tracing, build info, health endpoint가 준비되어 있습니다.
5. **품질 게이트**: Checkstyle, SpotBugs, JaCoCo, performance smoke test, E2E 테스트가 Gradle/README에 반영되어 있습니다.

다만 현재 코드 기준으로는 다음 영역을 우선 개선하는 것이 좋습니다.

- 서비스 레벨 입력 검증 정책 불일치
- 외부 수집 결과가 비어 있을 때 기존 데이터가 삭제될 수 있는 갱신 흐름
- 보안 헤더 설정의 이중화/드리프트 가능성
- native query 집계의 장기 확장성
- 비동기 이벤트 처리 실패 관측성
- Docker 이미지 빌드와 테스트 게이트의 명확한 분리

## 2. 우선순위별 개선안

### P0 — 즉시 반영 권장

#### P0-1. `RecommendService`의 count 보정 정책을 명시적 검증으로 변경

**확인 내용**

`RecommendService`는 `MIN_COUNT = 1`, `MAX_COUNT = 10`을 정의하지만, 실제 `normalizeCount()`에서 `Math.clamp(count, MIN_COUNT, MAX_COUNT)`로 값을 조용히 보정합니다.

```java
private static int normalizeCount(int count) {
    return (int) Math.clamp(count, MIN_COUNT, MAX_COUNT);
}
```

**문제점**

- API 요청자가 `count=999`를 보내도 10개 추천으로 성공 처리될 수 있습니다.
- 컨트롤러/DTO에서 검증하는 정책과 서비스 계층 정책이 어긋날 수 있습니다.
- 잘못된 입력에 대한 클라이언트 피드백이 불명확해집니다.

**개선 방향**

```java
private static int validateCount(int count) {
    if (count < MIN_COUNT || count > MAX_COUNT) {
        throw new BusinessException(
                ErrorCode.LOTTO_INVALID_COUNT,
                "count must be between " + MIN_COUNT + " and " + MAX_COUNT
        );
    }
    return count;
}
```

**수용 기준**

- `count < 1`, `count > 10` 요청은 4xx와 `LOTTO_INVALID_COUNT`를 반환합니다.
- 서비스 단위 테스트와 WebMvc 테스트가 모두 동일한 정책을 검증합니다.
- README/API 문서에 `count` 허용 범위를 명시합니다.

---

#### P0-2. `WinningStoreCollector.persist()`의 삭제 후 저장 흐름 보호

**확인 내용**

판매점 수집 로직은 외부 API에서 등수별 판매점 목록을 가져온 뒤 `deleteByRound(round)`를 수행하고, `entities`가 비어 있지 않을 때만 `saveAll()`을 호출합니다.

```java
@Transactional
public void persist(int round, List<WinningStoreEntity> entities) {
    storeRepository.deleteByRound(round);
    if (!entities.isEmpty()) {
        storeRepository.saveAll(entities);
        log.info("winning stores saved: round={}, count={}", round, entities.size());
    }
}
```

**문제점**

- 외부 API 장애, 파싱 실패, 일시적 빈 응답이 `empty list`로 흡수되면 기존 데이터가 삭제될 수 있습니다.
- 1등/2등 중 한 등수만 실패한 부분 성공 상태도 전체 갱신 성공으로 오해될 수 있습니다.

**개선 방향**

- 외부 fetch 결과를 `FetchResult`처럼 상태와 데이터로 구분합니다.
- 모든 필수 grade fetch가 성공했을 때만 기존 데이터를 삭제합니다.
- 빈 응답이 정상인지 장애인지 구분 가능한 상태값을 추가합니다.
- 삭제/삽입 대신 round+grade+name/address 기반 upsert 또는 staging table 교체 방식을 고려합니다.

**예시 설계**

```java
record StoreFetchBatch(int round, boolean complete, List<WinningStoreEntity> entities, List<String> warnings) {}

@Transactional
public void replaceStores(StoreFetchBatch batch) {
    if (!batch.complete()) {
        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE, "winning store fetch incomplete");
    }
    storeRepository.deleteByRound(batch.round());
    storeRepository.saveAll(batch.entities());
}
```

**수용 기준**

- 외부 API가 빈 응답/부분 실패를 반환해도 기존 판매점 데이터가 삭제되지 않습니다.
- 실패 grade, 실패 round, 원인별 metric/log가 남습니다.
- 재시도 시 idempotent하게 동작합니다.

---

#### P0-3. 보안 헤더 설정을 `KraftSecurityProperties`와 단일화

**확인 내용**

`application.yml`에는 CSP, X-Frame-Options, Referrer-Policy, Permissions-Policy, HSTS 등 보안 헤더 값이 환경변수 기반으로 정의되어 있습니다. 반면 `SecurityConfig` 쪽에서는 CSP와 header 정책을 코드에서 직접 지정하는 구조가 확인됩니다.

**문제점**

- 설정 파일과 코드의 보안 정책이 다르게 진화할 수 있습니다.
- 운영 환경에서 CSP를 환경변수로 바꿔도 실제 filter chain이 하드코딩 값을 사용할 위험이 있습니다.
- README와 실제 런타임 정책이 달라질 수 있습니다.

**개선 방향**

- `SecurityConfig`는 `KraftSecurityProperties.Headers`만 참조하도록 정리합니다.
- 보안 헤더 적용 책임을 `SecurityHeadersFilter`로 단일화할지, Spring Security `headers()`로 단일화할지 결정합니다.
- 둘 다 유지하지 말고 한 경로에서만 header를 세팅합니다.

**수용 기준**

- `KRAFT_SECURITY_CSP` 변경 시 실제 응답 CSP가 변경됩니다.
- 보안 헤더 테스트가 `MockMvc`로 존재합니다.
- 프로파일별 HSTS 활성/비활성 정책이 테스트됩니다.

---

#### P0-4. Ops/Admin 보호 경로 매칭을 보강

**확인 내용**

`AdminApiTokenFilter`는 `POST /api/winning-numbers/refresh`에 대한 토큰 검증을 수행합니다. exact path 기반 매칭은 간단하지만, trailing slash, context path, proxy rewrite, 신규 mutation endpoint 추가 시 우회/누락 위험이 있습니다.

**문제점**

- `/api/winning-numbers/refresh/` 같은 변형 경로가 의도치 않게 다른 filter chain으로 흘러갈 수 있습니다.
- 신규 운영성 POST endpoint가 추가될 때 보호 목록에 누락될 수 있습니다.

**개선 방향**

- `/ops/**`, `/admin/ops/**`, 수집/갱신성 endpoint를 하나의 운영 API namespace로 정렬합니다.
- Spring `PathPatternParser` 또는 `AntPathRequestMatcher` 기반으로 명확히 매칭합니다.
- 모든 mutation endpoint에 대해 “보호됨/차단됨” 테스트를 작성합니다.

**수용 기준**

- 보호 대상 POST/PUT/PATCH/DELETE endpoint는 토큰 또는 Ops allowlist 없이는 실패합니다.
- trailing slash, context path, forwarded prefix 케이스가 테스트됩니다.
- 신규 endpoint 추가 시 실패하는 architecture/security test가 있습니다.

## 3. P1 — 단기 개선 권장

### P1-1. 통계 API 입력값 검증 강화

`WinningStatisticsCacheService`의 `combinationPrizeHistory()`는 조합 검증이 매우 좋습니다. null, 개수, 범위, 중복을 bit mask로 검증합니다. 반면 `frequencyForPeriod(int rounds)`, `companionNumbers(int target)`은 repository query 전에 명시 검증을 추가하는 편이 안전합니다.

**개선 방향**

- `rounds`: 1 이상, 운영상 허용 가능한 상한값 설정
- `target`: 1~45 범위 검증
- 오류 코드는 `LOTTO_INVALID_NUMBER` 또는 별도 통계 파라미터 오류 코드로 통일

**수용 기준**

- `/api/statistics/companion?target=0`, `target=46`, `rounds=0`, `rounds=-1` 요청이 4xx로 실패합니다.
- repository mock이 호출되지 않는 단위 테스트가 있습니다.

---

### P1-2. native query 집계의 실행 계획 관리

`WinningNumberRepository`는 ball frequency, prize hit, odd/even, sum, companion query를 native query와 `UNION ALL`로 처리합니다. 현재 데이터 크기에서는 충분하지만 장기적으로 회차/부가 통계가 증가하면 쿼리 비용이 누적될 수 있습니다.

**개선 방향**

- `winning_numbers(round)`는 기본/PK로 충분한지 확인합니다.
- `n1..n6`, `bonus_number`에 대한 query pattern별 index 필요성을 실행 계획으로 검증합니다.
- 자주 조회되는 통계는 summary table 또는 materialized aggregate 형태로 유지합니다.
- `ORDER BY hitCount DESC`에는 tie breaker(`other_ball ASC`)를 추가해 deterministic response를 보장합니다.

**수용 기준**

- Testcontainers MariaDB에서 `EXPLAIN` 기반 smoke test 또는 문서화된 실행 계획을 보관합니다.
- 통계 조회 p95 latency 목표를 정하고 performance smoke test에 포함합니다.

---

### P1-3. 비동기 이벤트 처리 실패 관측성 추가

`WinningStatisticsService`와 `WinningStoreCollector`는 `@Async @EventListener`를 사용합니다. 비동기 이벤트는 사용자 요청과 분리되어 좋지만, 실패가 호출자에게 전파되지 않으므로 로그/metric/retry 체계가 중요합니다.

**개선 방향**

- `AsyncConfigurer` 또는 `AsyncUncaughtExceptionHandler`를 등록합니다.
- 이벤트 핸들러별 실패 counter와 latency timer를 기록합니다.
- 외부 API 의존 이벤트는 backoff retry 또는 재처리 가능한 job 형태로 전환합니다.

**수용 기준**

- 이벤트 핸들러 예외 발생 시 metric과 structured log가 남습니다.
- 운영 대시보드에서 마지막 이벤트 성공/실패 시각을 볼 수 있습니다.

---

### P1-4. Docker 이미지 빌드와 테스트 게이트 분리 명확화

`Dockerfile`은 이미지 빌드 단계에서 `./gradlew --no-daemon clean bootJar -x test`를 실행합니다. Docker build 자체를 빠르게 하는 방향으로는 타당하지만, CI/CD에서 테스트가 선행되어야 안전합니다.

**개선 방향**

- CI workflow에서 `./gradlew check -PstrictStatic=true -PstrictCoverage=true` 통과 후에만 Docker build를 수행합니다.
- Dockerfile에는 “CI에서 테스트를 선행한다”는 주석 또는 문서화를 추가합니다.
- Spring Boot layered jar 또는 Buildpacks/Jib를 검토해 이미지 layer cache 효율을 높입니다.

**수용 기준**

- main 배포 workflow는 check 실패 시 Docker build/deploy 단계에 진입하지 않습니다.
- 이미지 SBOM 또는 dependency vulnerability scan을 추가합니다.

## 4. P2 — 중기 개선 제안

### P2-1. 추천 생성기의 재현성/분포 검증 강화

`ConstraintAwareLottoNumberGenerator`는 `ThreadLocalRandom` 기반으로 후보를 뽑고 birthday/decade/long-run 제약을 만족시키도록 보정합니다. 현재 운영 성능에는 큰 문제가 없어 보이나, 장기적으로는 “편향 제거”가 실제로 의도한 분포를 만드는지 진단하는 테스트가 있으면 좋습니다.

**개선 방향**

- 테스트용 `RandomSource` 인터페이스를 두어 deterministic seed 기반 검증을 쉽게 합니다.
- property-based test로 10만 회 생성 시 규칙 위반률 0, 번호별 분포 편차 허용 범위를 검증합니다.
- 추천 화면/README에 “당첨 확률 향상 아님”을 계속 명시합니다.

---

### P2-2. 캐시 무효화 경쟁 조건 점검

통계 서비스는 데이터 수집 이벤트 발생 시 frequency summary를 refresh한 뒤 여러 cache를 clear합니다. 이 순서는 의도적으로 보이지만, 동시 요청이 들어오는 상황에서는 오래된 cache miss/recompute와 clear가 교차할 수 있습니다.

**개선 방향**

- 수집 완료 이벤트 처리 중 통계 cache refresh/evict를 하나의 application service로 캡슐화합니다.
- cache version key 또는 latestRound 기반 key를 사용해 old/new cache 교차 문제를 줄입니다.
- 이벤트 처리 실패 시 다음 조회에서 fallback recompute가 안전한지 테스트합니다.

---

### P2-3. 설정 파일과 `.env.example` 정합성 자동 검증

`application.yml`에는 상당히 많은 `KRAFT_*` 환경변수 키가 있습니다. 기능이 늘어날수록 `.env.example`, README 환경변수 표, 실제 `@ConfigurationProperties` 간 불일치가 발생하기 쉽습니다.

**개선 방향**

- `application.yml`의 `${KRAFT_*}` 키를 추출해 `.env.example`에 누락된 키가 없는지 검증하는 Gradle task를 추가합니다.
- `@ConfigurationProperties` 클래스에는 Bean Validation annotation을 적극 적용합니다.
- 운영 필수값은 `ProdConfigValidator`에서 fail-fast로 검증합니다.

---

### P2-4. 프론트엔드 자산의 품질 게이트 확장

현재 README는 HTMX, Bootstrap, PWA, Playwright, 접근성 검증을 언급합니다. JavaScript/CSS 규모가 계속 커진다면 백엔드와 별도 품질 게이트가 필요합니다.

**개선 방향**

- `npm run lint`, `npm run format:check`, `npm run test:e2e`를 CI에 명시적으로 연결합니다.
- DOM 조작은 `innerHTML`보다 `createElement`/`replaceChildren` 중심으로 유지합니다.
- axe-core E2E 외에 keyboard navigation, reduced motion, dark mode snapshot 검증을 추가합니다.

## 5. 파일군별 상세 분석

### 5.1 루트/문서

**확인한 특징**

- README는 프로젝트 목적, 기술 스택, 아키텍처, 프로파일, 빠른 시작, 환경변수, 빌드/테스트, 보안, 스케줄러를 한국어로 정리하고 있습니다.
- “확률 향상”이 아니라 “편향 조합 회피”를 목적으로 둔다고 명시한 점은 로또 서비스의 오해를 줄이는 좋은 방향입니다.
- public site, CI badge, Java/Spring/MariaDB badge가 포함되어 onboarding이 좋습니다.

**개선 제안**

- README의 기술 스택과 `build.gradle.kts` 실제 버전이 항상 일치하는지 CI로 검증합니다.
- 환경변수 표는 필수/권장/운영 전용을 더 엄격히 분류합니다.
- 운영 배포 절차와 rollback 절차를 별도 `docs/operations.md`로 분리합니다.

### 5.2 Gradle/품질 게이트

**확인한 특징**

- Spring Boot, dependency management, JaCoCo, Checkstyle, SpotBugs가 적용되어 있습니다.
- `strictStatic`, `strictCoverage` Gradle property로 로컬 개발과 CI 엄격도를 분리한 점이 좋습니다.
- performance smoke test를 별도 task로 분리해 일반 테스트와 성능 테스트를 구분하고 있습니다.

**개선 제안**

- CI에서는 항상 `-PstrictStatic=true -PstrictCoverage=true`를 적용합니다.
- dependency locking 또는 version catalog(`libs.versions.toml`)를 도입해 의존성 변경 추적성을 높입니다.
- OWASP Dependency-Check, Trivy, Gradle dependency verification을 추가 검토합니다.

### 5.3 Spring 설정

**확인한 특징**

- `application.yml`은 DB, Hikari, Flyway, JPA, management, tracing, security, collect, API client, cache, news, recommend 설정을 환경변수 기반으로 구성합니다.
- `server.forward-headers-strategy: native`가 설정되어 reverse proxy 환경을 고려하고 있습니다.
- Actuator exposure는 기본적으로 health/info로 제한되어 있습니다.

**개선 제안**

- 보안 헤더와 SecurityConfig의 정책 중복을 제거합니다.
- Hikari pool 값은 운영 DB 자원과 트래픽 목표에 맞춘 문서화가 필요합니다.
- `KRAFT_TRACING_SAMPLE_RATE`, `KRAFT_OTEL_ENDPOINT`는 prod 기본값/필수 여부를 README와 일치시킵니다.

### 5.4 보안/Ops

**확인한 특징**

- Rate limit, IP allowlist, Ops token, Actuator 제한, CSP/HSTS 계열 header가 반영되어 있습니다.
- token 비교에 constant-time 비교를 사용한 점은 적절합니다.
- 운영성 endpoint를 일반 public API와 분리하려는 방향이 보입니다.

**개선 제안**

- 운영성 mutation endpoint를 `/ops/**` 또는 `/admin/ops/**`로 완전히 통합합니다.
- security test matrix를 작성합니다: public, authenticated, ops-token, allowlist, actuator, docs, static resource.
- proxy header 신뢰 설정(`trusted-proxies`)과 실제 배포 nginx/ingress 설정을 문서화합니다.

### 5.5 추천 도메인/애플리케이션

**확인한 특징**

- `ExclusionRule` 전략 패턴으로 추천 제외 규칙이 분리되어 있습니다.
- 생일 편향, 등차수열, 연속수, 십의 자리 집중, 과거 당첨 조합 등 규칙이 명확합니다.
- metrics recorder와 timeout exception을 별도로 둔 점이 좋습니다.

**개선 제안**

- count 입력은 silent clamp 대신 명시적 오류로 통일합니다.
- filter rule 내부 class가 늘어나면 별도 파일/패키지로 분리합니다.
- 추천 생성기의 장기 분포 검증을 property-based/performance smoke test로 보강합니다.

### 5.6 당첨번호/판매점 수집

**확인한 특징**

- API client, circuit breaker, retry/backoff, event notifier, range collector, fetch log repository 등 수집 장애를 의식한 설계입니다.
- 수동/자동 수집, retention scheduler, backfill job 관리가 존재합니다.

**개선 제안**

- 판매점 수집은 fetch 성공 상태와 저장 교체를 분리해 기존 데이터 삭제 위험을 제거합니다.
- long backfill은 취소, 재시작, 진행률 persistence를 강화합니다.
- 외부 API 응답 schema drift를 contract test 또는 fixture test로 관리합니다.

### 5.7 통계/쿼리

**확인한 특징**

- frequency summary table과 cache를 함께 사용해 성능을 확보하려는 구조입니다.
- 조합 검증은 bit mask를 활용해 간결하고 빠르게 구현되어 있습니다.
- native query로 DB 집계를 직접 수행해 JPA object materialization 비용을 줄이고 있습니다.

**개선 제안**

- companion/frequency period 입력값을 명시 검증합니다.
- native query별 실행 계획과 index 효과를 문서화합니다.
- tie-breaker 정렬을 보강해 동일 count 결과의 응답 순서를 안정화합니다.

### 5.8 Docker/배포

**확인한 특징**

- multi-stage Dockerfile, digest pinning, non-root user, healthcheck, heap dump/log volume이 반영되어 있습니다.
- BuildKit cache mount를 사용해 Gradle dependency/build cache를 활용합니다.

**개선 제안**

- Dockerfile의 `-x test`는 CI 선행 테스트 조건과 함께 문서화합니다.
- SBOM, image vulnerability scan, base image digest 주기적 갱신 정책을 추가합니다.
- curl 설치가 필요한지 재검토하고, 가능하면 더 작은 runtime image를 검토합니다.

### 5.9 테스트

**확인한 특징**

- README 기준 JUnit 5, jqwik, Testcontainers, Playwright, axe-core E2E, performance smoke test가 포함되어 있습니다.
- Gradle은 perf tag를 일반 test에서 제외하고 별도 task로 실행합니다.

**개선 제안**

- 보안 matrix test와 입력값 negative test를 추가합니다.
- Testcontainers 기반 MariaDB 쿼리 성능 smoke test를 추가합니다.
- scheduler/async event 실패 케이스를 테스트합니다.

## 6. 이슈화 가능한 작업 목록

| 우선순위 | 작업 | 주요 파일 | 완료 기준 |
|---|---|---|---|
| P0 | 추천 count 검증 정책 통일 | `RecommendService`, controller tests | invalid count가 4xx/`LOTTO_INVALID_COUNT` |
| P0 | 판매점 수집 삭제 위험 제거 | `WinningStoreCollector`, repository tests | fetch 실패 시 기존 데이터 보존 |
| P0 | 보안 헤더 설정 단일화 | `SecurityConfig`, `SecurityHeadersFilter`, `KraftSecurityProperties` | env CSP 변경이 응답에 반영 |
| P0 | Ops path matching matrix 보강 | `AdminApiTokenFilter`, security tests | trailing slash/context path 우회 없음 |
| P1 | 통계 입력값 검증 추가 | `WinningStatisticsCacheService` | invalid target/rounds가 repository 호출 전 실패 |
| P1 | native query 실행 계획 관리 | `WinningNumberRepository`, migration docs | index/EXPLAIN 문서와 smoke test |
| P1 | async event 실패 관측성 | event handlers, async config | 실패 metric/log/retry 확인 가능 |
| P1 | Docker build/test gate 명확화 | `Dockerfile`, CI workflows | check 실패 시 image build 미실행 |
| P2 | 추천 분포 검증 | recommend tests | property/perf test 통과 |
| P2 | 설정/env 정합성 검증 | Gradle task, `.env.example` | 누락 env key가 CI에서 실패 |

## 7. 권장 적용 순서

1. `RecommendService` count 검증 정책 변경 및 테스트 추가
2. `WinningStoreCollector` 저장 교체 로직 안전화
3. Security/Ops endpoint matrix 테스트 추가
4. Security header 설정 단일화
5. 통계 API 입력값 검증 추가
6. native query 실행 계획/인덱스 점검
7. async event observability 보강
8. Docker/CI image gate 보강
9. env/README/config drift 자동 검증
10. 추천 분포/성능 property test 확장

## 8. 결론

kLo 저장소는 이미 운영형 Spring Boot 서비스에 필요한 상당수의 요소를 갖추고 있습니다. 가장 먼저 손볼 부분은 “잘못된 입력을 조용히 보정하지 않는 것”, “외부 수집 실패가 기존 데이터를 파괴하지 않도록 하는 것”, “보안 설정을 한 경로로 단일화하는 것”입니다. 이 세 가지는 기능 추가보다 우선순위가 높으며, 장애·보안·운영 리스크를 직접 줄입니다.

