# kLo Improvement Complement

## P1 — 완료

- [x] X7: 405 핸들러 추가 (GlobalExceptionHandler, OpsExceptionHandler)
- [x] X9: PublicRateLimitFilter Caffeine 캐시 maximumSize 설정
- [x] X3: Clock 일원화 (DhLotteryApiClient, LottoApiClientConfig, OpsController, WinningNumberFrequencySummaryEntity)
- [x] bottom-nav.js listener/observer 누수 수정 (코드 내 이미 수정됨)
- [x] WinningNumberEntity rawJson 4000자 절단 — 의도적 캡 확인, 상수화, 테스트 추가
- [x] ExclusionRule 평가 순서 테스트 (PastWinningRule 마지막 보장)

## P2 — 완료

- [x] X8: 미사용 코드 제거 (saveIfAbsent, boolean resolve 오버로드, repo 미사용 메서드, ErrorCode 미사용 상수)
- [x] X6: trust-forwarded-for 무효 설정 제거 (KraftSecurityProperties, application.yml)
- [x] IpAllowlist 중복 제거 (ActuatorAccessFilter ↔ OpsAccessFilter 공통 추출)
- [x] X4: DhLotteryApiClient → LottoDrawSchedule.expectedRound() 위임 (FIRST_DRAW_DATE, expectedRoundAsOf 제거)
- [x] X2: 텔레스코핑 생성자 정리 (DhLotteryApiClient 8→3, SmokLottoApiClient 8→3)
- [x] admin-ops.js/html 정렬 "현재 페이지" 레이블 추가
- [x] CI -PstrictStatic -PstrictCoverage 검증 (이미 적용됨)
- [x] DHLOTTERY_TOKENS/SMOK_TOKENS 단일 출처화 (LottoApiClientConfig.prodAllowedClientTokens())

## P3 — 완료

- [x] robots.txt /admin, /ops Disallow 추가
- [x] theme.js i18n (Light/Dark → 라이트/다크 한국어)
- [x] CollectResponse Javadoc 수정 (ABSOLUTE_MAX_ROUNDS_PER_CALL → maxPerRun)
- [x] MAX_ROUND=3000 / MAX_COUNT=10 단일 출처화 (템플릿, 컨트롤러, JS)
- [x] validation.js 동적 바인딩 (min/max를 DOM 속성에서 읽도록)
- [x] CSP unsafe-inline 제거
- [x] LottoCombination 이중 copyOf 제거 (.sorted().toList()만으로 충분)
- [x] WinningNumberAutoCollectScheduler 이중 가드 주석 추가
- [x] X5: 십의 자리/볼 색상 버킷 차이 문서화 (SingleDecadeRule, LottoBallHelper)
- [x] WinningNumberEntityTest rawJson 절단 테스트 추가
- [x] LottoCollectionCommandService truncated → reachedEnd 가독성 개선
- [x] permissions-policy 환경변수 매핑 추가 (KRAFT_SECURITY_PERMISSIONS_POLICY)
- [x] @ValidRound / RoundValidator 미사용 제거
- [x] WinningNumbersCollectedEvent 3-arg 오버로드 제거
- [x] LottoFetchLogEntity 매직 숫자 상수화 (MESSAGE_MAX_LENGTH, RAW_RESPONSE_MAX_LENGTH, FAILURE_REASON_MAX_LENGTH)

---

## 미완료 항목 및 후속 계획

### [P2-X1] AbstractLottoApiClient 공통 베이스 추출

**배경:** `DhLotteryApiClient`(315줄)와 `SmokLottoApiClient`(256줄)가 `fetch()` 루프,
`handleRestClientException`, `handleClientException`, `isRetriable`, `sleepBackoff`,
`count()` 등 약 60%의 로직을 중복 보유.
Smok은 Dh보다 per-reason 실패 메트릭 세분화가 부족한데, 공통 베이스가 생기면 자동으로 동기화됨.

**설계 (template method 패턴):**

```
AbstractLottoApiClient (신규)
 ├── protected final 필드: restClient, baseUrl, clock, retrySupport, meterRegistry, circuitBreaker
 ├── public final fetch(int round)          ← 공통 루프 (템플릿 메서드)
 ├── private: handleRestClientException, handleClientException, sleepBackoff, isRetriable
 ├── protected: count(String metricName, String... tags)  ← Dh가 ALLOWED_FAILURE_REASONS 필터로 오버라이드
 ├── abstract: executeAttempt(int round, long deadline)   ← 클라이언트별 단일 호출 로직
 ├── abstract: clientName()                ← "dhlottery" / "smok" (메트릭·로그 prefix 파생)
 ├── abstract: timeoutMessage(int round)
 └── protected hook: onClientExceptionFailure(LottoApiClientException ex)
                     ← Dh만 per-reason 메트릭 기록, 기본은 no-op
```

**영향 파일:**
- 신규: `AbstractLottoApiClient.java` (~110줄)
- 수정: `DhLotteryApiClient.java` (extends AbstractLottoApiClient, 공통 로직 제거 → 약 -120줄)
- 수정: `SmokLottoApiClient.java` (extends AbstractLottoApiClient, 공통 로직 제거 → 약 -100줄)
- 수정: `DhLotteryApiClientTest.java` (ScriptedDhLotteryApiClient super() 인자 조정)
- 수정: `SmokLottoApiClientTest.java` (ScriptedSmokLottoApiClient super() 인자 조정)

**예상 효과:** 전체 코드 약 220줄 순감, 두 클라이언트 간 메트릭 동작 자동 동기화.

---

### [P2] FetchFailureReasonSupport ↔ LottoFetchLogEntity 중복 제거

**배경:** `LottoFetchLogEntity.resolveFailureReason`(infrastructure)이
`FetchFailureReasonSupport.extractReason`(application)과 동일한 "reason=X; ..." 파싱 로직을
독립 구현 중 (~8줄 중복).

**아키텍처 이슈:** `infrastructure → application` 의존은 역방향이 되어 주의 필요.
같은 `feature/winningnumber` bounded context 내이므로 허용 가능하다고 판단.

**계획:**
1. `FetchFailureReasonSupport`를 `public` 클래스로, `extractReason`을 `public static`으로 노출
2. `LottoFetchLogEntity.resolveFailureReason`을 `extractReason` 위임 래퍼로 축소:
   ```java
   private static String resolveFailureReason(LottoFetchStatus status, String message) {
       if (status != LottoFetchStatus.FAILED || message == null || message.isBlank()) return null;
       String reason = FetchFailureReasonSupport.extractReason(message);
       return "other".equals(reason) ? null : truncate(reason, FAILURE_REASON_MAX_LENGTH);
   }
   ```
3. `extractReason`의 ';' 없는 경우 파싱 동작을 엔티티 기존 동작과 정렬

**영향 파일:**
- 수정: `FetchFailureReasonSupport.java` (class → public, extractReason → public static)
- 수정: `LottoFetchLogEntity.java` (resolveFailureReason을 위임 래퍼로 축소)
- 수정: `LottoFetchLogEntityTest.java` (동작 변경 없음, 회귀 확인)

---

### [P3] 벤더 라이브러리 버전 트래킹

**배경:** `static/` 디렉토리에 벤더링된 `bootstrap.min.css`, `htmx.min.js` 파일에 버전 정보 없음.
보안 업데이트 시 어느 버전인지 추적 불가.

**계획:** 각 벤더 파일 첫 줄에 버전 주석 추가 (5분 작업, 빌드 파이프라인 변경 없음):
```css
/* Bootstrap v5.3.x | MIT License | https://getbootstrap.com */
```
```js
// htmx v1.x.x | https://htmx.org | @license BSD-2-Clause
```

**영향 파일:** `static/css/bootstrap.min.css`, `static/js/htmx.min.js`

---

### [P3] app.css 분리 — 보류

**판단:** 850줄 단일 파일 분리는 CSS 선택자 순서 의존성, 빌드 파이프라인 변경, 테마 변수 참조 등
리스크 대비 ROI가 낮음. 기능 추가·대규모 리팩토링 시점에 함께 진행 권장.
