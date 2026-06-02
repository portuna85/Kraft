# kLo 저장소 개선 제안서

- 분석 기준: 전체 소스 파일 직접 열람 (Java, YAML, SQL, Dockerfile, CI/CD)
- 작성일: 2026-06-02

---

## 1. 요약 평가

Java 25 / Spring Boot 4.0.6 기반의 운영형 서비스로, 수집 안정성·관측 가능성·품질 게이트 측면에서 높은 완성도를 보인다.  
아래 우선순위 체계로 개선한다.

| 등급 | 기준 | 건수 |
|------|------|------|
| P0 | 데이터 손실·보안·런타임 오류 위험 | 7 |
| P1 | 운영 안정성·테스트 가능성 저하 | 9 |
| P2 | 코드 품질·관측성·유지보수성 | 10 |

---

## 2. P0 — 즉시 수정 필요

### P0-1. 필터 실행 순서 충돌

**파일**: `ActuatorAccessFilter.java`, `WwwRedirectFilter.java`

두 필터가 동일한 `Ordered.HIGHEST_PRECEDENCE + 10`으로 등록되어 있어 실행 순서가 JVM 구현에 따라 달라진다.  
WWW 리다이렉트가 Actuator 접근 차단보다 먼저 실행되면 IP 차단 없이 `/actuator/`가 리다이렉트될 수 있다.

```
현재: WwwRedirectFilter = HIGHEST_PRECEDENCE+10
      ActuatorAccessFilter = HIGHEST_PRECEDENCE+10  ← 동일 순서, 비결정적

권장:
  WwwRedirectFilter    = HIGHEST_PRECEDENCE + 5
  ActuatorAccessFilter = HIGHEST_PRECEDENCE + 10
  OpsAccessFilter      = HIGHEST_PRECEDENCE + 15
  RequestIdFilter      = HIGHEST_PRECEDENCE + 20
  PublicRateLimitFilter= HIGHEST_PRECEDENCE + 30
  SecurityHeadersFilter= HIGHEST_PRECEDENCE + 40
```

---

### P0-2. WinningNumberEntity.updateFrom() 필드 누락

**파일**: `WinningNumberEntity.java`, `updateFrom()` 메서드

`secondPrize`, `secondWinners` 컬럼이 `updateFrom()` 내부에서 갱신되지 않는다.  
수집 재실행 시 2등 정보가 이전 값으로 유지되는 묵음 버그다.

```java
// 현재 updateFrom()에 누락된 라인
this.secondPrize   = source.getSecondPrize();
this.secondWinners = source.getSecondWinners();
```

`WinningNumberPersister`의 `UPDATED` 케이스 단위 테스트에서 이 필드를 검증해야 한다.

---

### P0-3. ApiCircuitBreaker 리스너를 synchronized 블록 내부에서 호출

**파일**: `ApiCircuitBreaker.java`, `transitionTo()` 메서드

상태 전이 콜백(`listener.onTransition()`)이 synchronized 블록 안에서 실행된다.  
리스너가 다른 synchronized 메서드를 호출하거나 블로킹 I/O(메트릭 전송 등)를 수행하면 데드락이 발생한다.

```java
// 현재 (위험)
private synchronized void transitionTo(State next) {
    this.state = next;
    listener.onTransition(next);  // synchronized 블록 안
}

// 권장
private synchronized void transitionTo(State next) {
    this.state = next;
}
// 호출 후 별도로
listener.onTransition(next);  // 락 해제 후 실행
```

---

### P0-4. RecommendService 입력 묵음 보정

**파일**: `RecommendService.java`

`count` 파라미터를 `Math.clamp(count, 1, 10)`으로 조용히 보정한다.  
`count=999` 요청이 HTTP 200으로 성공 응답을 받아 클라이언트가 오류 인식을 못한다.

`RecommendFilter`의 `oddCount`(허용 범위 0–6), `sumMin`/`sumMax` 경계도 검증되지 않는다.

```java
// 권장 — count 명시 검증
private static int validateCount(int count) {
    if (count < MIN_COUNT || count > MAX_COUNT) {
        throw new BusinessException(ErrorCode.LOTTO_INVALID_COUNT,
            "count must be between " + MIN_COUNT + " and " + MAX_COUNT);
    }
    return count;
}
```

---

### P0-5. WinningStoreCollector.persist()의 선삭제 위험

**파일**: `WinningStoreCollector.java`, `persist()` 메서드

```java
storeRepository.deleteByRound(round);
if (!entities.isEmpty()) {
    storeRepository.saveAll(entities);
}
```

외부 API 일시 장애·파싱 실패 → 빈 리스트 반환 → 기존 데이터 전체 삭제.  
1등 fetch 성공 / 2등 fetch 실패 상황의 부분 성공도 구분할 수 없다.

```java
// 권장 — 완전 성공 확인 후 교체
record StoreFetchBatch(int round, boolean complete,
                       List<WinningStoreEntity> entities) {}

@Transactional
public void replaceStores(StoreFetchBatch batch) {
    if (!batch.complete()) {
        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE,
            "winning store fetch incomplete for round=" + batch.round());
    }
    storeRepository.deleteByRound(batch.round());
    storeRepository.saveAll(batch.entities());
}
```

---

### P0-6. NewsCollectionService 배치 단일 트랜잭션

**파일**: `NewsCollectionService.java`, `collect()` 메서드

전체 기사 목록을 하나의 `@Transactional` 안에서 처리한다.  
한 건의 `DataIntegrityViolationException`이 이미 저장된 모든 기사를 롤백시킨다.

```java
// 권장 — 건별 try-catch 또는 REQUIRES_NEW 트랜잭션
articles.forEach(article -> {
    try {
        saveArticle(article);
    } catch (DataIntegrityViolationException e) {
        log.warn("article duplicate, skipped: hash={}", article.linkHash());
    }
});
```

`link_hash`에 DB UNIQUE 제약이 있어도 동시 요청 시 중복 삽입 경쟁 조건이 발생할 수 있으므로  
예외를 정상 흐름으로 처리하는 코드가 반드시 필요하다.

---

### P0-7. Rate Limit 고정 윈도우 경계 폭발 허용

**파일**: `PublicRateLimitFilter.java`

Fixed-window 알고리즘은 윈도우 경계에서 최대 `2 × maxRequests` 요청을 허용한다.  
윈도우 59초에 120회, 0초에 120회 전송하면 2초 내 240요청이 통과된다.

```java
// 권장 — 슬라이딩 윈도우 또는 토큰 버킷으로 교체
// 단기 대안: 윈도우를 10초로 줄이고 maxRequests를 20으로 비례 축소
```

---

## 3. P1 — 단기 개선 권장

### P1-1. @Async 예외 핸들러 미등록

**파일**: `KraftLottoApplication.java`, `PastWinningCacheLoader.java`, `WinningStoreCollector.java`

`@EnableAsync` 설정에 `AsyncUncaughtExceptionHandler`가 없다.  
`@Async @EventListener` 메서드의 예외가 로그에 남지 않을 수 있고, requestId MDC 컨텍스트도 비동기 스레드로 전파되지 않는다.

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async task failed: method={}", method.getName(), ex);
    }

    @Override
    public Executor getAsyncExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            return () -> {
                if (mdc != null) MDC.setContextMap(mdc);
                try { runnable.run(); }
                finally { MDC.clear(); }
            };
        });
        executor.initialize();
        return executor;
    }
}
```

---

### P1-2. 통계 입력값 검증 누락

**파일**: `WinningStatisticsCacheService.java`

`frequencyForPeriod(int rounds)`와 `companionNumbers(int target)`에 입력 검증이 없다.  
`rounds=0`, `target=46` 같은 요청이 repository 쿼리까지 도달한다.

```java
// frequencyForPeriod
if (rounds <= 0) throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
    "rounds must be positive");

// companionNumbers
if (target < LOTTO_NUMBER_MIN || target > LOTTO_NUMBER_MAX)
    throw new BusinessException(ErrorCode.LOTTO_INVALID_NUMBER,
        "target must be between 1 and 45");
```

---

### P1-3. NewsQueryService 페이지 파라미터 미검증

**파일**: `NewsQueryService.java`

`list(int page, int size)`가 음수 값을 그대로 `PageRequest.of(page, size)`에 전달한다.  
Spring Data가 `IllegalArgumentException`을 던지며 500 응답이 반환된다.

---

### P1-4. NewsCollectionService Clock 미주입

**파일**: `NewsCollectionService.java`

`LocalDateTime.now()`를 직접 호출해 테스트에서 시간 제어가 불가능하다.

```java
// 권장
private final Clock clock;

// collect() 내부
.collectedAt(LocalDateTime.now(clock))
```

---

### P1-5. WwwRedirectFilter 도메인 하드코딩

**파일**: `WwwRedirectFilter.java`

`kraft.io.kr`과 `https://www.kraft.io.kr`이 코드에 고정되어 있어  
스테이징 환경이나 도메인 변경 시 코드 수정이 필요하다.

```java
// 권장 — properties에서 주입
@Value("${kraft.web.apex-host:kraft.io.kr}")
private String apexHost;

@Value("${kraft.web.canonical-origin:https://www.kraft.io.kr}")
private String canonicalOrigin;
```

---

### P1-6. FetchFailureReasonSupport 문자열 패턴 의존

**파일**: `FetchFailureReasonSupport.java`

예외 메시지 문자열 패턴으로 실패 분류를 수행한다.  
외부 라이브러리·JDK 예외 메시지가 변경되면 분류가 silent하게 깨진다.

```java
// 권장 — 예외 타입 우선 분류, 문자열 패턴은 fallback으로만 사용
if (ex instanceof SocketTimeoutException) return FailureReason.TIMEOUT;
if (ex instanceof ConnectException)       return FailureReason.NETWORK;
// ... 이후 메시지 패턴 분류
```

---

### P1-7. 비동기 이벤트 실패 시 캐시 갱신 경쟁 조건

**파일**: `WinningStatisticsService.java`, `evictCachesOnCollected()`

```java
refreshFrequencySummary();  // (1) summary 갱신
cacheManager.getCache("winningNumberFrequency").clear(); // (2) 캐시 무효화
```

(1)이 예외를 던지면 summary는 낡은 상태인데 (2)는 계속 실행되어 다음 조회 시  
캐시 미스 → 낡은 summary로 frequency 재계산 오류가 발생한다.

```java
// 권장
try {
    refreshFrequencySummary();
} catch (Exception e) {
    log.error("frequency summary refresh failed, skipping cache eviction", e);
    return;
}
cacheManager.getCache("winningNumberFrequency").clear();
```

---

### P1-8. ConstraintAwareLottoNumberGenerator O(n²) 탐색

**파일**: `ConstraintAwareLottoNumberGenerator.java`, `findLongRunIndices()` 메서드

내부에서 `ArrayList.contains()`를 루프 안에서 호출해 O(n²) 탐색이 발생한다.  
6개 요소라 실제 영향은 미미하지만, 코드 의도가 명확하지 않다.

```java
// 권장 — HashSet 사용
Set<Integer> runSet = new HashSet<>(runIndices);
```

---

### P1-9. XXE 방어 불완전

**파일**: `NewsRssClient.java`, `parse()` 메서드

`DISALLOW_DOCTYPE_DECL` feature 하나만 설정되어 있다.  
외부 일반 엔티티(external general entities)는 별도 feature로 차단해야 한다.

```java
factory.setFeature(
    "http://xml.org/sax/features/external-general-entities", false);
factory.setFeature(
    "http://xml.org/sax/features/external-parameter-entities", false);
factory.setXIncludeAware(false);
factory.setExpandEntityReferences(false);
```

---

## 4. P2 — 중기 개선 제안

### P2-1. Caffeine 캐시 통계 미노출

**파일**: `CacheConfig.java`

`recordStats()`는 호출되지만 Micrometer로 지표를 내보내지 않는다.  
캐시 히트율·미스율·적재량이 Prometheus에서 보이지 않는다.

```java
// 각 Caffeine Cache 빈에 추가
CaffeineCacheMetrics.monitor(meterRegistry, caffeine, cacheName);
```

---

### P2-2. LogSanitizer URL 인코딩된 값 누락

**파일**: `LogSanitizer.java`, `maskSensitiveQuery()` 메서드

`token=%33%33`처럼 URL 인코딩된 파라미터 값은 정규식 패턴에 걸리지 않아 로그에 그대로 출력된다.

```java
// 권장 — 마스킹 전 URL 디코딩
String decoded = URLDecoder.decode(query, StandardCharsets.UTF_8);
return SENSITIVE_PARAM_PATTERN.matcher(decoded).replaceAll("$1=***");
```

---

### P2-3. 동반 번호 순위 동점 처리 미흡

**파일**: `WinningStatisticsCacheService.java`, `companionNumbers()` 메서드

같은 `hitCount`의 번호가 순차 rank를 받아 API 응답 순서가 호출마다 다를 수 있다.

```java
// 권장 — dense rank 구현
int rank = 1;
long prevCount = -1;
for (var row : rows) {
    if (row.hitCount() != prevCount) { rank = rows.indexOf(row) + 1; }
    prevCount = row.hitCount();
}
```

---

### P2-4. Rollback 이전 이미지 다이제스트 미저장

**파일**: `scripts/deploy/build-and-up.sh`, `scripts/deploy/rollback.sh`

`rollback.sh`는 `deploy-state/previous.env`에서 이전 다이제스트를 읽지만  
`build-and-up.sh`는 해당 파일을 생성하지 않아 롤백이 동작하지 않는다.

```bash
# build-and-up.sh 배포 전에 추가
mkdir -p deploy-state
echo "PREVIOUS_DIGEST=$(docker inspect kraft-lotto-app:latest \
  --format '{{.Id}}' 2>/dev/null || echo '')" > deploy-state/previous.env
```

---

### P2-5. SBOM·성능 리포트 보존 기간 짧음

**파일**: `.github/workflows/ci.yml`

| 아티팩트 | 현재 | 권장 | 이유 |
|----------|------|------|------|
| SBOM | 14일 | 180일 | 공급망 감사 추적 |
| 성능 리포트 | 14일 | 90일 | 추세 분석 |
| 테스트 리포트 | 14일 | 30일 | 회귀 디버깅 |

---

### P2-6. CD 배포 아티팩트 감사 이력 미저장

**파일**: `.github/workflows/cd.yml`

배포된 commit SHA가 step summary에만 남고, 구조화된 감사 아티팩트가 없다.

```yaml
- name: Save deployment audit
  run: |
    echo "{\"ref\":\"${{ steps.ref.outputs.value }}\",\
    \"actor\":\"${{ github.actor }}\",\
    \"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" > deployment-audit.json
- uses: actions/upload-artifact@v5
  with:
    name: deployment-audit-${{ github.run_id }}
    path: deployment-audit.json
    retention-days: 365
```

---

### P2-7. Docker 이미지 레이어 캐시 비효율

**파일**: `Dockerfile`

`COPY . .` 이후 `./gradlew clean bootJar`를 실행해 소스 변경 시 의존성 다운로드가 반복된다.

```dockerfile
# 권장 — 의존성 레이어 선분리
COPY build.gradle.kts settings.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar -x test --no-daemon
```

---

### P2-8. 설정 파일·환경변수 정합성 자동 검증 없음

`application.yml`의 `${KRAFT_*}` 키와 `.env.example`·README 환경변수 표 간 불일치를 CI가 감지하지 못한다.

```python
# scripts/check_env_drift.py 추가
import re, sys
yml = open('src/main/resources/application.yml').read()
example = open('.env.example').read()
missing = [k for k in re.findall(r'\$\{(KRAFT_[^:}]+)', yml)
           if k not in example]
if missing:
    print("Missing in .env.example:", missing); sys.exit(1)
```

---

### P2-9. 멀티파트 업로드 크기 제한 미설정

**파일**: `application.yml`

`spring.servlet.multipart.max-file-size`, `max-request-size` 설정이 없다.  
큰 파일 업로드로 OOM·DoS가 가능하다.

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
```

---

### P2-10. 테스트 커버리지 보완 영역

| 대상 | 누락 케이스 |
|------|------------|
| `DhLotteryResponseParser` | 필드 누락 JSON, 타입 불일치, HTML 응답, 날짜 파싱 실패 |
| `ApiCircuitBreaker` | half-open 최대 호출 수, 동시 전이 경쟁 |
| `WinningNumberEntity.updateFrom()` | secondPrize / secondWinners 갱신 검증 |
| `NewsCollectionService.collect()` | 중복 저장 시 롤백 격리, 배치 부분 실패 |
| `NewsQueryService.list()` | page < 0, size = 0 경계값 |
| `WinningStoreCollector.persist()` | 빈 entities → 기존 데이터 보존 검증 |
| `PublicRateLimitFilter` | 윈도우 경계 burst 시나리오 |
| `frequencyForPeriod` / `companionNumbers` | 유효성 검증 단위 테스트 |

---

## 5. 파일군별 강점 요약

| 영역 | 강점 |
|------|------|
| 추천 도메인 | `LottoCombination` compact constructor 엄격한 검증, `PastWinningCache` volatile 스냅샷 패턴 |
| 수집 인프라 | 상세한 실패 분류 (13종), 회로 차단기 + 재시도 + 지터, 낙관적 잠금 upsert |
| 통계 | 비트마스크 기반 O(6) 조합 검증, summary table fallback 패턴 |
| 보안 필터 | constant-time 토큰 비교, CIDR allowlist, MDC 주입 |
| 설정 | `@ConfigurationProperties` 중첩 레코드 + Bean Validation, ProdConfigValidator fail-fast |
| CI/CD | Trivy + Syft 연동, E2E Axe 접근성 테스트, performanceSmokeTest 분리 |
| DB 스키마 | 37개 CHECK 제약, 복합 인덱스 설계, optimistic lock version 컬럼 |

---

## 6. 권장 적용 순서

```
1. P0-1  필터 순서 충돌 수정 (단순 상수 변경)
2. P0-2  WinningNumberEntity.updateFrom() secondPrize/Winners 추가
3. P0-3  ApiCircuitBreaker 리스너 synchronized 블록 외부로 이동
4. P0-4  RecommendService count 명시 검증 + RecommendFilter 범위 검증
5. P0-5  WinningStoreCollector 선삭제 방어 로직
6. P0-6  NewsCollectionService 건별 트랜잭션 격리
7. P0-7  PublicRateLimitFilter 슬라이딩 윈도우 전환
8. P1-1  AsyncConfig — 예외 핸들러 + MDC 전파
9. P1-2  통계 API 입력 검증
10. P1-9 XXE 방어 완전화
11. P2-4  Rollback 이전 이미지 저장 스크립트
12. 나머지 P1/P2 순서대로
```
