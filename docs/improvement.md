# kLo / kraft.io.kr 개선 브리프 (최종 검증본)

> 기준: 업로드된 `kLo-main` 실제 소스 대조 + 패치 적용 검증 후 롤백  
> 출처: GPT 정적 분석 보고서 + Claude 코드 리뷰를 통합하고, **모든 항목을 실제 코드로 확인**해 취사선택함  
> 원칙: 기능 추가 아님(코드 품질·견고성·보안 한정), 공개 동작/엔드포인트/응답 형식 보존

이 문서는 Codex / Claude Code 등 코딩 에이전트용 구현 가이드입니다.
초안 작성 후 실제로 패치를 적용·검증하는 과정에서 **두 가지 항목을 정정**했고
(아래 §0-D), 그 결과를 반영한 최종본입니다. GPT 보고서의 폭넓은 제안 중 실제
코드와 어긋나거나 이미 구현된 항목은 제외했습니다. CI/CD·인프라성 항목은
AGENTS.md의 "CI/CD 수정 금지" 원칙에 따라 **에이전트 작업이 아닌 오너 수동
작업**으로 분리했습니다.

---

## 0. 검증 결과

### A. 확인된 유효 항목 (작업으로 채택)

| 항목 | 코드 확인 결과 | 처리 |
|---|---|---|
| `WwwRedirectFilter` Host 헤더 오픈 리다이렉트 | `request.getServerName()`를 그대로 `https://www.<host>`로 301. apex 외 임의 호스트(`evil.com` 등)도 리다이렉트됨 | **채택 (B1, 최우선)** |
| upsert INSERT 충돌 catch 무력화 | assigned `@Id`+`@Version`이라 `save()`=persist, INSERT가 커밋 시점 지연 → 기존 `catch(DataIntegrityViolationException)`가 못 잡음 | **채택 (B2, B3과 통합)** |
| 낙관적 락 재시도 무효 | 재시도가 단일 `@Transactional` 내부에서 돌아 rollback-only로 커밋 불가 | **채택 (B3, B2와 통합)** |
| rate-limit 문서/구현 불일치 | CLAUDE.md "per-IP **token bucket**" vs 실제 `FixedWindowCounter` | **채택 (B5, 문서 1줄)** |
| 메트릭 null-가드 보일러플레이트 | 다수 클래스에 `if (meterRegistry == null) return;` 반복 | 채택 (B6, 선택) |
| `RequiredConfigValidator` 비대(약 338줄) | 검증 책임 다수 보유 | 채택 (B7, 선택) |
| 사소한 일관성(varargs in-place / inline FQN) | `DhLotteryApiClient.count`, `OpsController.mapCircuitBreakerStates` 확인 | 채택 (B8) |

### B. 이미 구현되어 있어 반려 (재작업 금지)

| GPT 제안 | 실제 코드 |
|---|---|
| 통계 캐시 무효화 추가 | **이미 이벤트 기반**: `WinningStatisticsService.evictCachesOnCollected(@EventListener)` + `PastWinningCacheLoader.onCollected(...)` |
| 외부 API Client 공통 retry/metric/timeout 추출 | **이미 추출됨**: `ApiCallExecutor`, `ApiRetrySupport`, `ApiCircuitBreaker`, `BackfillDelaySupport`, `FetchFailureReasonSupport` |
| fetch log 보관 정책 추가 | **이미 존재**: `LottoFetchLogRetentionScheduler` + `kraft.collect.log-retention.*` + V7/V11 인덱스. (GPT SQL은 테이블명 `lotto_fetch_log` 오기 — 실제 `lotto_fetch_logs`) |
| DTO record 적용 | 대부분 **이미 record**. Entity는 의도적 class 유지 — 정상 |
| Actuator 노출 최소화 | **이미 health,info만** 노출. GPT의 metrics/prometheus 추가는 오히려 노출 확대 |

### C. 원칙 위반으로 반려

| GPT 제안 | 반려 사유 |
|---|---|
| 추천 생성기를 `SumRangeConstraint` 등으로 분리 | (1) 추천 도메인은 **이미 `ExclusionRule` 전략 패턴**. 생성기는 의도적 pre-filter. (2) 제안 제약들은 **신규 기능**이라 "추천 동작 변경 금지" 위반 |

### D. 패치 검증 중 정정된 항목 ⚠️ (초안 대비 변경)

실제 코드에 패치를 적용해 보며 두 가지를 바로잡았습니다. **에이전트는 아래를 반드시 반영**하세요.

1. **B4(`catch (Throwable)` → `catch (Exception)`) 철회.**
   `OpsCollectionFacade.withLock`이 호출하는 ShedLock `LockingTaskExecutor.executeWithLock(...)`의
   시그니처는 `throws Throwable`이다. 따라서 `catch (Exception)`으로 좁히면 **컴파일 불가**.
   현재의 `catch (Throwable)` + RuntimeException/Error 재던지기 + checked 래핑은
   **올바른 처리이며 손대지 않는다.** (이전 브리프의 B4 지시는 무효)

2. **B2는 단독으로 불충분 → B3과 반드시 통합.**
   `saveAndFlush`만 적용해도, 제약 위반/낙관적 락 충돌이 트랜잭션을 rollback-only로
   만들기 때문에 같은 트랜잭션 안에서 `UNCHANGED`를 반환해도 커밋 단계에서
   `UnexpectedRollbackException`이 난다. 정확한 수정은 **시도별 새 트랜잭션
   (`REQUIRES_NEW`) + 비트랜잭션 외부 재시도**다. 아래 B2+B3 통합안 참조.

---

## 1. 작업 우선순위 요약 (에이전트 채택분)

| # | 작업 | 분류 | 위험(미수정) | 노력 | 우선순위 |
|---|------|------|------|------|------|
| B1 | `WwwRedirectFilter` apex allowlist + 고정 목적지 | 보안 | 중(오픈 리다이렉트/canonical 오염) | 낮음 | **즉시** |
| B2+B3 | upsert를 시도별 `REQUIRES_NEW` + 외부 재시도로 통합 | 정합성 | 중(경합 시) | 중 | **높음** |
| B5 | rate-limit 문서/구현 정합 (문서 1줄) | 품질 | 낮음 | 매우 낮음 | 중 |
| B6 | `MeterRegistry == null` 보일러플레이트 제거 | 품질 | 낮음 | 중 | 중(선택) |
| B7 | `RequiredConfigValidator` 검증기 분리 | 품질 | 낮음 | 중 | 낮음(선택) |
| B8 | 사소한 일관성 정리(varargs/inline FQN) | 품질 | 매우 낮음 | 낮음 | 낮음 |
| ~~B4~~ | ~~락 예외 축소~~ | — | — | — | **철회(§0-D)** |

---

## 2. 백엔드 작업 (에이전트 수행)

### Task B1: `WwwRedirectFilter` 오픈 리다이렉트 차단 (최우선)

파일: `support/WwwRedirectFilter.java`

문제: 목적지를 사용자 제어 가능한 Host에서 만든다.
```java
String host = request.getServerName();
String location = "https://www." + host + request.getRequestURI() + ...;
```
`needsWwwRedirect`가 www/localhost/사설IP/IPv4만 제외하므로, 임의 호스트
(`evil.com`)는 `https://www.evil.com/...`로 301된다 → 오픈 리다이렉트 + canonical 오염.

요구 변경: apex만 허용, 목적지는 고정 origin 사용.
```java
private static final String APEX_HOST = "kraft.io.kr";
private static final String CANONICAL_ORIGIN = "https://www.kraft.io.kr";

private static boolean needsWwwRedirect(String host) {
    if (host == null || host.isBlank()) return false;
    String h = host.toLowerCase(java.util.Locale.ROOT);
    int colon = h.indexOf(':');
    if (colon >= 0) h = h.substring(0, colon);   // 포트 제거
    return APEX_HOST.equals(h);                   // 정확히 apex일 때만
}
// 목적지에 입력 host를 쓰지 않는다:
String location = CANONICAL_ORIGIN + request.getRequestURI()
        + (query != null && !query.isBlank() ? "?" + query : "");
```
제약:
- `kraft.io.kr` → `https://www.kraft.io.kr` 301 유지, `kraft.io.kr:443`도 동일.
- `www.*`, localhost/사설망/IP는 통과(기존 동작 유지).
- 그 외 임의 호스트는 **리다이렉트 없이 통과**.
- 테스트 추가(기존 `WwwRedirectFilterTest`에): `evil.com`/`kraft.io.kr.evil.com`/`sub.kraft.io.kr` 통과(200), 포트 포함 apex 301.

### Task B2+B3: upsert 견고성 (통합 작업) — 핵심

파일: `feature/winningnumber/application/WinningNumberPersister.java`
(+ 신규 협력자 1개 권장)

문제 요약(§0-A, §0-D):
- INSERT 충돌이 커밋 시점에 터져 기존 catch가 무력.
- 재시도가 단일 트랜잭션 내부라 rollback-only로 무효.

권장 구조: **재시도 루프는 비트랜잭션**, **각 시도는 새 트랜잭션**.

1) 신규 `WinningNumberUpsertExecutor` (시도 1회 = 트랜잭션 1개):
```java
@Component
public class WinningNumberUpsertExecutor {
    private final WinningNumberRepository repository;
    private final Clock clock;
    // 생성자 주입 ...

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpsertOutcome upsertOnce(WinningNumber wn) {
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findById(wn.round())
            .map(existing -> {
                var incoming = WinningNumberMapper.toEntity(wn, now);
                if (isSame(existing, incoming)) return UpsertOutcome.UNCHANGED;
                existing.updateFrom(incoming, now);
                return UpsertOutcome.UPDATED;
            })
            .orElseGet(() -> {
                repository.saveAndFlush(WinningNumberMapper.toEntity(wn, now)); // flush로 충돌 즉시 표면화
                return UpsertOutcome.INSERTED;
            });
    }
    // isSame(...)은 기존 Persister에서 이동
}
```

2) `WinningNumberPersister.upsert`는 트랜잭션 제거 + 외부 재시도:
```java
public UpsertOutcome upsert(WinningNumber wn) {
    long started = System.nanoTime();
    UpsertOutcome outcome = UpsertOutcome.FAILED;
    for (int attempt = 1; attempt <= UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK; attempt++) {
        try {
            outcome = executor.upsertOnce(wn);   // 새 트랜잭션
            break;
        } catch (DataIntegrityViolationException ex) {
            // 동시 INSERT 충돌: 다음 시도의 재조회에서 기존 행 발견 → UNCHANGED로 확정
            outcome = UpsertOutcome.UNCHANGED;
            // 마지막 시도가 아니면 continue, 마지막이면 UNCHANGED로 종료
            if (attempt < UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK) continue;
            break;
        } catch (OptimisticLockingFailureException ex) {
            if (attempt == UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK) {
                if (meterRegistry != null) {
                    meterRegistry.counter("kraft.winningnumber.optimistic_lock.failure").increment();
                }
                outcome = UpsertOutcome.FAILED;
            }
        }
    }
    recordDbSaveLatency(started, /* mode from outcome */);
    return outcome;
}
```

제약:
- `UpsertOutcome`(INSERTED/UPDATED/UNCHANGED/FAILED) 의미 유지.
- 최대 2회 시도(`UPSERT_MAX_RETRIES_ON_OPTIMISTIC_LOCK`) 유지.
- `kraft.winningnumber.optimistic_lock.failure` 메트릭, `kraft.winningnumber.db.save.latency` 타이머 유지.
- 회차별 트랜잭션 격리 유지(한 회차 실패가 다른 회차 롤백 금지).
- 신규 외부 의존성 없음(자체 `REQUIRES_NEW`). Spring Retry는 승인 시에만.
- **주의**: `REQUIRES_NEW`는 self-invocation으로 동작하지 않는다. 반드시 **별도 빈**(`WinningNumberUpsertExecutor`)으로 분리해 프록시 경유 호출.
- 빈 배선: `WinningNumberUpsertExecutor`는 `@Component`(생성자 주입) 또는 `LottoCollectionConfiguration`에 `@Bean` 추가. `Persister` 생성자에 executor 주입.

테스트 영향 및 추가:
- 기존 `WinningNumberPersisterTest`는 `new WinningNumberPersister(repository, clock, meterRegistry)`로 직접 생성하고 `repository`를 mock한다. executor 분리 후에는 **executor를 주입하는 생성자**가 필요하다(테스트용 패키지-프라이빗 생성자 유지).
- 기존 케이스 보존: same→UNCHANGED, changed→UPDATED, missing→INSERTED, insert-conflict→UNCHANGED, optimistic-retry-success→UPDATED, optimistic-exhausted→FAILED(+메트릭 1).
- mock 기반 단위 테스트는 `REQUIRES_NEW` 경계를 검증하지 못하므로, 가능하면 **Testcontainers(MariaDB) 통합 테스트 1개**로 동시 INSERT/버전 충돌의 재시도-확정을 검증.

> 참고: 수집은 `CollectionRunState` 뮤텍스 + ShedLock으로 직렬화되어 동일 회차 동시 쓰기는 드뭅니다. 본 작업은 "방어 코드가 실제로 방어하도록" 만드는 견고성 강화입니다.

### Task B5: rate-limit 문서/구현 정합 (문서 1줄)

`CLAUDE.md`의 "per-IP token bucket" → 실제 구현(`FixedWindowCounter`)에 맞춰
`fixed-window rate limiter, keyed per (client IP, request URI)`로 정정.

> `CLAUDE.md`/`AGENTS.md`는 `.gitignore` 대상(비커밋 문서)이므로 **로컬 문서 수정**으로 처리. 코드/커밋 변경 아님.
> token bucket 재구현은 불필요한 변경이므로 권장하지 않음.

### Task B6: 메트릭 null-가드 제거 (선택)

`if (meterRegistry == null) return;` 반복(`DhLotteryApiClient`, `LottoRecommender`,
`WinningNumberPersister`, `WinningStatisticsCacheService` 등)을 no-op
`MeterRegistry`(예: `SimpleMeterRegistry`) 주입으로 대체해 가드 제거.
제약: 메트릭 이름/태그/값 불변, 기존 메트릭 테스트로 무변경 검증.

### Task B7: `RequiredConfigValidator`(약 338줄) 분리 (선택)

검증 책임(필수 프로퍼티/profile 정책/JDBC URL/네트워크 preflight)을 책임별
검증기로 분리. 기동 검증 동작·실패 메시지·실패 시점 동일 유지, 신규 정책 추가 금지.

### Task B8: 사소한 일관성 정리 (해당 파일 수정 시에만)

1. `DhLotteryApiClient.count(...)`가 호출자 varargs를 in-place 수정(`tags[1] = "other"`)
   → `tags.clone()` 후 수정.
2. `OpsController.mapCircuitBreakerStates`의 inline FQN
   (`java.util.stream.Collectors`, `java.util.TreeMap`) → import 정리.

순수 가독성, 동작 변경 0, 신규 테스트 불필요.

---

## 3. 오너 수동 작업 — CI/CD (AGENTS.md상 에이전트 수정 금지)

`.github/workflows/**` 수정이 필요하므로 **오너가 직접** 처리.

- **`upload-artifact@v7` 버전**: 현재 공개 최신은 **v6**(v7 미확인). 워크플로의 여러
  `actions/upload-artifact@v7`(및 `setup-java@v5`)이 실제 resolve되는지 확인하고
  안정 버전(`@v6`) 또는 커밋 SHA로 핀 고정.
- **Trivy 게이트**: 현재 `trivy fs build/libs/app.jar --severity HIGH,CRITICAL --exit-code 0`
  (리포트 전용). main 브랜치는 `--exit-code 1`로 게이트화 검토(`continue-on-error` 분기 존재).
- **actionlint / shellcheck**(선택): `scripts/deploy/*.sh` 7개 + 워크플로 사전 검증.

---

## 4. 오너 결정 — 배포 / 운영 / SEO (에이전트 작업 아님)

- **배포 다운타임**: `scripts/deploy/build-and-up.sh`가 `docker compose down --remove-orphans`
  후 `up` → 순간 중단. (`rollback.sh`, `wait-readiness.sh`, `smoke-test.sh`, previous/current
  이미지 태깅은 이미 존재.) `down` 제거 후 `up -d --build --remove-orphans`로 근접 무중단 검토.
- **HSTS**: `hsts-enabled` 기본 false. HTTPS/리다이렉트/인증서 자동갱신/하위도메인 정책
  확인 후 운영에서 `KRAFT_SECURITY_HSTS_ENABLED=true`.
- **rate-limit 키 정책**: 키가 `IP + URI`라 경로별 예산 분리. IP당 전체 예산이 의도면
  IP 단일 키로, 경로별이 의도면 현행 유지(문서화). 동작 변경 전 의도 확정.
- **SEO 보강(선택)**: `base.html`에 og:image(1200×630) + Twitter card + `WebSite` JSON-LD.
  HTMX `/fragments/**`에 `X-Robots-Tag: noindex` 검토(robots.txt는 이미 `/admin`,`/ops` Disallow).

---

## 5. 명시적 제외 (하지 말 것)

- 통계 쿼리(`findBallFrequencies`, `findPrizeHitsByNumbers`) 인덱싱/최적화 — 약 1,200행, 캐시 적용, 연 ~52행 증가. 실익 없음.
- `LottoCollectionCommandService`의 `@Transactional(NOT_SUPPORTED)` 설계 변경 — 회차별 독립 트랜잭션은 의도된 정상 설계.
- 추천 도메인(`ExclusionRule`, `LottoCombination`, 규칙 순서) 및 공개 DTO 형태 변경.
- 캐시 무효화/외부 API 공통화/fetch log 보관 "추가" — 이미 구현됨(§0-B).
- `OpsCollectionFacade.withLock`의 `catch (Throwable)` 변경 — 컴파일 정합상 올바른 처리(§0-D).
- 프런트엔드 프레임워크/번들러 도입.

---

## 6. 구현 순서

1. B1 `WwwRedirectFilter` (보안, 최우선)
2. B2+B3 upsert 견고성 통합 (executor 분리 + 외부 재시도)
3. B5 rate-limit 문서 정정(로컬 문서)
4. B6 no-op MeterRegistry (선택)
5. B8 사소한 정리(해당 파일 수정 시)
6. B7 `RequiredConfigValidator` 분리 (여유 시)

(§3·§4는 오너가 병렬로 결정/처리)

---

## 7. 검증

각 의미 있는 변경 후:
```bash
./gradlew test
```
광범위 검증:
```bash
./gradlew check
./gradlew check -PstrictStatic=true
```
B2+B3은 mock 단위 테스트만으로 트랜잭션 경계를 못 잡으므로, **Testcontainers(MariaDB) 통합 테스트**로 동시 INSERT/버전 충돌의 재시도-확정 경로를 검증한다(Flyway/DDL 호환 포함).

---

## 8. 완료 기준

- 공개 동작·엔드포인트·응답 형식 불변.
- `WwwRedirectFilter`가 apex만 고정 목적지로 301, 임의 호스트로 리다이렉트하지 않음(테스트 포함).
- upsert가 시도별 `REQUIRES_NEW` 트랜잭션 + 비트랜잭션 외부 재시도로 동작:
  - INSERT 충돌이 `saveAndFlush`로 표면화되고 재조회로 `UNCHANGED` 확정.
  - 낙관적 락 충돌이 새 트랜잭션에서 재시도, 소진 시 `FAILED`(+메트릭).
  - 기존 6개 케이스 + 통합 테스트 통과.
- rate-limit 설명이 구현과 일치(로컬 문서).
- (선택) 메트릭 null 가드 제거, `RequiredConfigValidator` 분리 — 동작 차이 없음.
- `OpsCollectionFacade`는 변경하지 않음.
- `./gradlew check` 통과(필요 시 `-PstrictStatic=true`).
- §0-B/§5 항목은 재작업하지 않음.

---

## 9. 권장 커밋 순서

```text
fix: restrict www redirect to apex host with fixed destination
fix: upsert winning number per-attempt in a new transaction with retry
refactor: inject no-op meter registry and drop null guards     # 선택
refactor: clean up dhlottery varargs and ops controller imports
refactor: split required config validator into focused validators  # 선택
```
관련 없는 변경을 한 커밋에 묶지 않습니다. (rate-limit 문서 정정은 비커밋 문서이므로 커밋 대상 아님)

---

## 10. 최종 요약 형식 (에이전트용)

```text
Summary:
- ...

Behavior changes:
- None, or list exact changes.

Tests:
- ./gradlew test: passed/failed/not run
- ./gradlew check: passed/failed/not run

Notes:
- B4 철회(ShedLock executeWithLock는 throws Throwable).
- B2+B3은 REQUIRES_NEW + 외부 재시도로 통합, 별도 빈 분리 필수.
- Owner-scope(CI/CD, deploy, HSTS, SEO)는 의도적으로 미수정.
```
