# KRAFT Lotto 최적화·개선 분석서

> 작성일: 2026-06-13 · 대상: `demo` (Spring Boot 4.1 / Java 25 백엔드 + Next.js 16 프론트엔드)
> 본 문서는 **분석 산출물**이며 코드를 변경하지 않는다. 실제 수정은 우선순위에 따라 후속 작업에서 진행한다.

---

## 1. 개요

### 분석 범위
- **백엔드**: 빌드 설정, `application.yml`, 핵심 서비스(수집/조회/통계/추천/저장), 웹 필터(레이트리밋/보안헤더/IP), 전역 예외 처리, 외부 수집 클라이언트, 이벤트 리스너, CORS·캐시 설정
- **프론트엔드**: `web/src/lib/api.ts`, `format.ts`, 클라이언트 컴포넌트, 미들웨어
- **인프라**: `docker-compose.yml`(런타임 하드닝 관점)

### 제외 범위
- **CI/CD** (`.github/workflows/*`) — 사용자 요청에 따라 제외
- 이미 완료된 blueprint 구현 항목(B-1~B-5, CSP nonce 등)은 회귀 관점만 언급

### 우선순위 기준
| 등급 | 의미 |
|------|------|
| **High** | 정확성/데이터 정합성에 영향을 주는 실제 결함. 우선 수정 권장 |
| **Medium** | 성능·견고성·일관성 저하. 운영 부하 증가 시 체감 |
| **Low** | 코드 품질·확장성·관측성 개선. 여유 시 정리 |

---

## 2. 즉시 수정 권장 (High)

### F-1. `@Async`가 실제로 비활성 — 이벤트 리스너가 동기 실행됨

**현황**
`StatisticsRefreshListener`, `RevalidateWebhookListener` 두 `@EventListener`에 `@Async`가 선언되어 있으나, 애플리케이션 어디에도 `@EnableAsync`가 없다. `Application.java`는 `@EnableScheduling`만 보유한다.

```
$ grep -r "EnableAsync" src/main   → 0건
```

**문제/영향**
`@EnableAsync`가 없으면 `@Async`는 **무시되고 메서드는 호출 스레드에서 동기 실행**된다.
`WinningNumberCollectionService.collectRound()`는 `@Transactional`이며, 트랜잭션 커밋 *전*에 `eventPublisher.publishEvent(...)`를 호출한다(`WinningNumberCollectionService.java:55`). 따라서:

1. **ISR 레이스**: `RevalidateWebhookListener`가 **DB 커밋 전에** Next.js `/api/revalidate`를 호출한다. Next.js가 즉시 백엔드를 재조회하면 아직 커밋되지 않은 **이전 데이터를 다시 ISR 캐시에 저장**할 수 있다.
2. **트랜잭션 점유 중 외부 I/O**: DB 트랜잭션이 열린 채로 외부 HTTP 호출이 수행된다(커넥션 점유 시간 증가).
3. **수집 지연**: `statisticsCacheService.rebuildAllSummaries()`가 수집 트랜잭션 시간에 합산된다.

**개선안**
1. 비동기 설정 + 전용 Executor 빈 추가
2. 두 리스너를 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 전환하여 **커밋 후** 비동기 실행

```java
// 신규: com.kraft.common.config.AsyncConfig
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "eventTaskExecutor")
    ThreadPoolTaskExecutor eventTaskExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("kraft-evt-");
        ex.initialize();
        return ex;
    }
}
```

```java
// RevalidateWebhookListener / StatisticsRefreshListener
@Async("eventTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onCollected(WinningNumbersCollectedEvent event) { ... }
```

> 참고: `AFTER_COMMIT` 전환 시, 커밋이 일어나지 않는 경로(예: 데이터 미변경)에서는 이벤트가 발화되지 않으므로 `dataChanged` 가드와 의미가 자연스럽게 합치된다.

**우선순위**: High
**대상 파일**: `src/main/java/com/kraft/Application.java`, `winningnumber/RevalidateWebhookListener.java`, `statistics/StatisticsRefreshListener.java`, (신규) `common/config/AsyncConfig.java`

---

### F-2. `@Cacheable` 읽기 메서드가 같은 빈의 `@Transactional/@CacheEvict` 메서드를 자기호출

**현황**
`WinningStatisticsCacheService`의 `getFrequencyStats()` / `getPatternStats()` / `getCompanionStats()`는 모두 `@Transactional(readOnly = true)`이다. summary가 비어 있으면 같은 빈의 `this.rebuildAllSummaries()`를 **직접 호출**한다(`WinningStatisticsCacheService.java:64, 79, 103`).

```java
@Cacheable(CacheConfig.STATS_FREQUENCY)   // 클래스에 @Transactional(readOnly=true)
public FrequencyStatsResponse getFrequencyStats() {
    ...
    if (summaries.isEmpty()) {
        rebuildAllSummaries();   // ← 자기호출: 프록시 미경유
        ...
    }
}

@Transactional                                       // ← 무시됨
@CacheEvict(value = {...}, allEntries = true)         // ← 무시됨
public void rebuildAllSummaries() { ... }
```

**문제/영향**
Spring AOP는 프록시 기반이므로 **같은 객체 내부의 `this.` 호출은 프록시를 거치지 않는다.** 따라서 `rebuildAllSummaries()`의 `@Transactional`(쓰기 전파)·`@CacheEvict`가 **적용되지 않는다.**

1. **읽기전용 트랜잭션 안에서 쓰기 발생**: 콜드 스타트(summary 미존재) 시 `saveAll(...)`이 `readOnly = true` 트랜잭션 컨텍스트에서 실행된다. Hibernate `FlushMode.MANUAL` 강제 및 일부 드라이버의 read-only 커넥션에서 쓰기 실패/미플러시 위험.
2. **`@CacheEvict` 미동작**: 재계산 후 다른 통계 캐시가 무효화되지 않는다.

**개선안 (택1)**
- (권장) 재계산 로직을 **별도 빈**(`StatisticsSummaryRebuilder`)으로 분리하여 프록시를 경유하게 한다.
- 또는 self-injection(`ObjectProvider<WinningStatisticsCacheService>`)으로 프록시 인스턴스를 통해 호출한다.
- 콜드 스타트 시점의 쓰기 경로는 명시적 쓰기 트랜잭션 경계 안에서만 수행되도록 보장한다.

**우선순위**: High
**대상 파일**: `src/main/java/com/kraft/statistics/WinningStatisticsCacheService.java`

---

### F-3. `SavedNumbersService.save()`의 한도/중복 검사 TOCTOU

**현황**
```java
// SavedNumbersService.java:39-58
public SaveNumberResult save(...) {
    return repo.findByClientTokenHashAndNumbers(hash, normalized)   // ① 조회
        .map(existing -> new SaveNumberResult(toResponse(existing), false))
        .orElseGet(() -> createSavedNumber(...));                    // ②
}
private SaveNumberResult createSavedNumber(...) {
    long currentCount = repo.countByClientTokenHash(hash);           // ③ 카운트
    if (currentCount >= max) throw CONFLICT;
    repo.save(...);                                                  // ④ 저장
}
```

**문제/영향**
- ③→④가 원자적이지 않다. 동일 기기에서 동시 저장 요청 시 `maxPerClient`를 **초과**할 수 있다.
- ①→④ 사이 레이스로 **동일 번호 동시 저장** 시 unique 제약(`client_token_hash, numbers`) 위반 → `DataIntegrityViolationException`이 `GlobalExceptionHandler`의 포괄 `Exception` 핸들러(`GlobalExceptionHandler.java:86`)로 떨어져 **500**으로 응답된다. 멱등 의도상 **200**이어야 한다.

**개선안**
- unique 제약 위반(`DataIntegrityViolationException`)을 잡아 기존 행을 재조회·반환하는 멱등 처리.
- 한도는 동시성 허용오차를 문서화하거나, 필요 시 DB 카운트 제약/락으로 보장.

```java
try {
    SavedNumber saved = repo.save(...);
    return new SaveNumberResult(toResponse(saved), true);
} catch (DataIntegrityViolationException e) {
    return repo.findByClientTokenHashAndNumbers(hash, normalized)
        .map(x -> new SaveNumberResult(toResponse(x), false))
        .orElseThrow(() -> e);
}
```

**우선순위**: Medium (정확성 영향이지만 빈도 낮음)
**대상 파일**: `src/main/java/com/kraft/saved/SavedNumbersService.java`

---

## 3. 성능 최적화 (Medium)

### P-1. 외부 `RestClient`에 연결/읽기 타임아웃 미설정

**현황**: `HttpExternalWinningNumberFetchClient`(`:20`)와 `RevalidateWebhookListener`(`:24`)가 `RestClient.builder().build()`를 타임아웃 없이 사용한다.

**문제**: 동행복권/web 엔드포인트가 응답을 지연하면 호출 스레드가 무한 대기한다. 서킷브레이커는 *예외/실패율* 기준이라, 타임아웃 없이 매달린 호출은 OPEN 전환을 지연시킨다.

**개선안**:
```java
var settings = ClientHttpRequestFactorySettings.defaults()
        .withConnectTimeout(Duration.ofSeconds(3))
        .withReadTimeout(Duration.ofSeconds(5));
this.restClient = RestClient.builder()
        .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
        .build();
```

**대상 파일**: `winningnumber/HttpExternalWinningNumberFetchClient.java`, `winningnumber/RevalidateWebhookListener.java`

---

### P-2. `totalRounds` 계산의 중복 쿼리

**현황**: stats 3개 메서드가 동일하게 `winningNumberRepository.count() > 0 ? findTopByOrderByRoundDesc()... : 0` 패턴을 사용한다(`WinningStatisticsCacheService.java:58-60, 83-85, 108-110`). `count()`와 `findTop...()` 두 쿼리가 발생하며 `count() > 0` 가드는 불필요하다.

**개선안**: 한 쿼리로 단순화하고 헬퍼로 추출.
```java
private int latestRound() {
    return winningNumberRepository.findTopByOrderByRoundDesc()
            .map(WinningNumber::getRound).orElse(0);
}
```

**대상 파일**: `statistics/WinningStatisticsCacheService.java`

---

### P-3. `saveAll(existing.values())` 불필요한 재저장

**현황**: `rebuildFrequency`/`upsertPatternRows`/`rebuildCompanions`가 이미 영속 상태(managed)인 기존 엔티티를 `row.update(...)`로 수정한 뒤 다시 `saveAll(existing.values())`를 호출한다(`:196, 239, 275`).

**문제**: 트랜잭션 안에서 managed 엔티티는 **더티 체킹으로 자동 flush**되므로 명시적 `saveAll`은 중복이다(불필요한 머지/순회 비용).

**개선안**: 신규 행만 `saveAll(toSave)`. 기존 행은 필드 수정만으로 충분.

**대상 파일**: `statistics/WinningStatisticsCacheService.java`

> 참고: 회차 수가 늘면 `rebuildAllSummaries()`가 매 수집마다 전체 재계산(`findAll()` + 동반쌍 O(n·15))을 수행한다. 현재 데이터 규모(~1,200회차)에서는 무해하나, 증분 갱신으로 전환하면 수집 지연을 추가로 줄일 수 있다.

---

### P-4. Caffeine 캐시 통계가 Prometheus에 미노출

**현황**: `CacheConfig`가 모든 캐시에 `.recordStats()`를 켰지만(`CacheConfig.java:39`), `CaffeineCacheMetrics`를 Micrometer 레지스트리에 바인딩하지 않아 적중률/적재 수가 Prometheus에 노출되지 않는다.

**개선안**: `CaffeineCacheManager` 대신 캐시별 `CaffeineCacheMetrics.monitor(registry, cache, name)` 등록, 또는 Spring Boot의 `spring.cache.type=caffeine` + `management.metrics.cache` 경로 활용.

**우선순위**: Low (관측성)
**대상 파일**: `common/config/CacheConfig.java`

---

## 4. 보안·일관성 (Medium / Low)

### S-1. CORS `allowedOriginPatterns("*")` 과도 허용

**현황**: `CorsConfig`가 `setAllowedOriginPatterns(List.of("*"))`로 모든 출처를 허용한다(`CorsConfig.java:16`). 실제 트래픽은 Next.js API 프록시(서버 간 호출)이므로 브라우저 CORS가 필요한 출처는 제한적이다.

**개선안**: `KRAFT_PUBLIC_BASE_URL` 기반 화이트리스트로 축소.
```java
config.setAllowedOriginPatterns(List.of(publicBaseUrl));   // 환경변수 주입
```

**우선순위**: Medium
**대상 파일**: `common/config/CorsConfig.java`

---

### S-2. CORS 노출 헤더와 실제 발급 헤더 불일치

**현황**: CORS `exposedHeaders`에 `X-RateLimit-Reset`이 포함되어 있으나(`CorsConfig.java:23`), `PublicRateLimitFilter`는 `X-RateLimit-Limit`/`X-RateLimit-Remaining`만 설정하고 `X-RateLimit-Reset`을 발급하지 않는다(`PublicRateLimitFilter.java:62-63`).

**개선안 (택1)**: ① 슬라이딩 윈도우 만료 시각을 `X-RateLimit-Reset`으로 실제 발급하거나, ② `exposedHeaders`에서 `X-RateLimit-Reset`을 제거하고 `X-RateLimit-Limit`을 추가하여 계약을 일치시킨다.

**우선순위**: Low
**대상 파일**: `common/config/CorsConfig.java`, `common/web/PublicRateLimitFilter.java`

---

### S-3. 레이트리미터의 인스턴스 로컬 상태

**현황**: `PublicRateLimitFilter`의 카운터가 인스턴스 로컬 Caffeine 캐시다(`:34`). 백엔드를 2개 이상으로 수평 확장하면 한도가 인스턴스별로 분리되어 전체 한도가 실질적으로 N배가 된다.

**개선안**: 현재 단일 인스턴스 전제이므로 **문서화 수준**. 다중 인스턴스 전환 시 Redis 등 공유 저장소 기반 토큰버킷으로 교체 검토.

**우선순위**: Low (현 구성에서는 정상)

---

### S-4. API/actuator 응답에 CSP 미적용

**현황**: 웹 페이지 CSP는 `web/src/middleware.ts`(이번에 추가)로 적용되지만, 백엔드 `SecurityHeadersFilter`는 `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`만 설정한다(`SecurityHeadersFilter.java:20-23`).

**개선안**: JSON API/actuator 응답에는 최소 CSP(`default-src 'none'; frame-ancestors 'none'`)를 추가하여 오리진 직접 접근 시 렌더링 표면을 차단.

**우선순위**: Low
**대상 파일**: `common/web/SecurityHeadersFilter.java`

---

## 5. 프론트엔드 견고성 (Medium / Low)

### W-1. `fetchJson`에 타임아웃/Abort 부재

**현황**: `web/src/lib/api.ts`의 `fetchJson`(`:34`)이 `AbortSignal` 없이 `fetch`한다. 백엔드가 느리면 SSR 렌더가 무한 대기하여 페이지 응답이 지연된다.

**개선안**:
```ts
const response = await fetch(`${backendBaseUrl}${path}`, {
  ...init,
  signal: AbortSignal.timeout(5000),
});
```

**우선순위**: Medium
**대상 파일**: `web/src/lib/api.ts`

---

### W-2. `analyzeNumbers`·`getOpsSummary`가 `fetchJson` 우회

**현황**: `analyzeNumbers`(`:116`)는 자체 `fetch`를 직접 구현하고, `getOpsSummary`(`:127`)는 `fetchJson`을 쓰지만 두 경로의 에러 처리/타임아웃 정책이 분산되어 있다.

**개선안**: POST/헤더 옵션을 받는 단일 `fetchJson`으로 통합하여 타임아웃·에러 표면 정책을 한 곳에서 관리.

**우선순위**: Low
**대상 파일**: `web/src/lib/api.ts`

---

### W-3. 백엔드 에러 코드/메시지 유실

**현황**: `fetchJson`이 실패 시 `throw new Error("Backend request failed: ... (status)")`로 백엔드의 구조화된 에러 바디(`code`, `message`)를 버린다(`:36-38`).

**개선안**: 응답 바디의 `code`/`message`를 파싱해 커스텀 에러로 전달하여 사용자 메시지/로깅 품질 향상.

**우선순위**: Low
**대상 파일**: `web/src/lib/api.ts`

---

### W-4. `RecommendationResponse` 타입 중복 정의

**현황**: 동일 타입이 `web/src/lib/api.ts`(`:24`)와 `web/src/components/recommend-client.tsx`(`:6`)에 각각 정의되어 있다.

**개선안**: `api.ts`의 타입을 import하여 단일 출처로 통합.

**우선순위**: Low
**대상 파일**: `web/src/components/recommend-client.tsx`

---

### W-5. 저장 응답 상태코드 가정 확인 필요

**현황**: `recommend-client.tsx`(`:81`)가 `res.status === 201`로 "저장 완료" vs "이미 저장됨"을 구분한다. 백엔드 저장 API가 신규=201/중복=200을 반환하는지 확인이 필요하다. 만약 둘 다 200이면 "이미 저장된 번호입니다" 분기가 동작하지 않는다.

**개선안**: 백엔드 응답의 `created` 플래그(이미 `SaveNumberResult.created` 존재)를 바디로 노출해 상태코드가 아닌 명시적 필드로 분기.

**우선순위**: Low
**대상 파일**: `web/src/components/recommend-client.tsx`, `saved/SavedNumbersController.java`(응답 바디 확인)

---

## 6. 요약 표

| ID | 항목 | 우선순위 | 영향 | 대상 |
|----|------|:---:|------|------|
| F-1 | `@EnableAsync` 누락 → 리스너 동기 실행, ISR 레이스 | **High** | 정합성/성능 | `Application.java`, 리스너 2종, (신규)`AsyncConfig` |
| F-2 | `@Cacheable` 내 `@Transactional/@CacheEvict` 자기호출 | **High** | 정합성 | `WinningStatisticsCacheService` |
| F-3 | `save()` 한도/중복 TOCTOU → 500 가능 | Medium | 정합성 | `SavedNumbersService` |
| P-1 | 외부 RestClient 타임아웃 미설정 | Medium | 성능/가용성 | 외부 클라이언트, revalidate 리스너 |
| P-2 | `totalRounds` 중복 쿼리 | Medium | 성능 | `WinningStatisticsCacheService` |
| P-3 | `saveAll(existing)` 중복 재저장 | Medium | 성능 | `WinningStatisticsCacheService` |
| P-4 | Caffeine 캐시 메트릭 미노출 | Low | 관측성 | `CacheConfig` |
| S-1 | CORS `*` 과도 허용 | Medium | 보안 | `CorsConfig` |
| S-2 | RateLimit 헤더 계약 불일치 | Low | 일관성 | `CorsConfig`, `PublicRateLimitFilter` |
| S-3 | 레이트리밋 인스턴스 로컬 | Low | 확장성 | `PublicRateLimitFilter` |
| S-4 | API 응답 CSP 미적용 | Low | 보안 | `SecurityHeadersFilter` |
| W-1 | `fetchJson` 타임아웃 부재 | Medium | 가용성 | `web/src/lib/api.ts` |
| W-2 | fetch 로직 분산 | Low | 유지보수 | `web/src/lib/api.ts` |
| W-3 | 백엔드 에러 표면 유실 | Low | UX/로깅 | `web/src/lib/api.ts` |
| W-4 | 타입 중복 정의 | Low | 유지보수 | `recommend-client.tsx` |
| W-5 | 저장 상태코드 가정 | Low | UX | `recommend-client.tsx` |

---

## 7. 부록: 검증 방법

### F-1 재현/확인
```bash
# @EnableAsync 부재 확인
grep -r "EnableAsync" src/main   # → 0건

# 동기 실행 확인: 수집 트리거 후 같은 스레드명으로 리스너 로그가 찍히는지 관찰
# (로그 패턴: WinningNumberCollectionService → RevalidateWebhookListener 가 동일 thread)
```
수정 후: 리스너 로그 스레드명이 `kraft-evt-*`로 분리되고, revalidate 로그가 수집 커밋 *이후* 타임스탬프에 찍히는지 확인.

### F-2 재현/확인
summary 테이블을 비운 상태(`TRUNCATE frequency_summary ...`)에서 `/api/v1/stats/frequency` 최초 호출 시:
- (현행) readOnly 트랜잭션 내 쓰기 경고/실패 또는 캐시 미무효화 관찰
- (수정 후) 별도 빈 경유로 쓰기 트랜잭션 정상 커밋 + `@CacheEvict` 동작 확인

### F-3 재현/확인
동일 `X-Device-Token`·동일 번호로 동시(병렬) POST `/api/v1/saved` 2건 전송:
- (현행) 한 건이 500(`INTERNAL_ERROR`) 가능
- (수정 후) 두 건 모두 200/201, `created` 플래그로 신규/중복 구분

### 빌드/타입 검증
- 백엔드: `./gradlew test`
- 프론트: `cd web && npm run test && npx tsc --noEmit`
