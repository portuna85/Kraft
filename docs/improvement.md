# Kraft 백엔드·프론트엔드 개선사항 (통합본)

> 본 문서는 GPT가 작성한 개선안과, 실제 소스코드를 직접 읽고 검증한 추가 분석을 **통합·교차검증**한 결과다.
> 각 항목 끝의 출처 표기:
> - `[GPT·검증]` GPT가 제기했고 실제 코드로 사실 확인됨
> - `[GPT·정정]` GPT가 제기했으나 실제 코드/버전 기준으로 일부 수정 필요
> - `[추가]` GPT 문서에 없던, 코드 검증으로 새로 발견한 항목

## 분석 기준

- 기존 대화 내용 미반영
- **제외**: CI/CD, GitHub Actions, Docker/Compose, Caddy, Prometheus/Grafana/Alertmanager, 배포·마이그레이션 스크립트
- **대상**: 백엔드 `src/main/java`·`src/main/resources`(+`templates/admin`), 프론트엔드 `web/src`·`web/package.json`·`web/next.config.ts`
- 규모: 백엔드 92개 Java 파일(약 3,876 LOC), 프론트엔드 54개 TS/TSX 파일(약 3,138 LOC)
- 스택: Java 25 · Spring Boot 4.1.0 · MariaDB · Flyway · Thymeleaf(admin) / Next.js 16.2.9 · React 19.2.7

---

## 1. 총평

기능 단위 분리는 양호하다. DTO/JPA/Flyway, rate limit, CSP nonce, ISR revalidation, Caffeine 캐시, 운영 API, 관리자 화면, 서킷브레이커, ShedLock, 감사 로그까지 운영 요소가 갖춰져 있다.

다만 **실제로 깨져 있거나 운영 위험이 높은 항목**이 먼저 보인다. 우선순위 요약:

| 순위 | 영역 | 핵심 문제 | 출처 |
|---:|---|---|---|
| P0 | 백엔드 | 관리자 `dashboard.html`·`rounds.html`이 현재 DTO에 없는 필드 참조 → 렌더링 500 | GPT·검증 |
| P1 | 백엔드 | 통계 rebuild가 read-only 트랜잭션에 합류 → flush 누락 가능 | GPT·검증 |
| P1 | 백엔드 | 운영 API가 Spring Security 체인 밖 + 토큰 비교가 비상수시간 | GPT·검증 |
| P1 | 백엔드 | admin lockout/audit가 프록시 IP(`getRemoteAddr`) 사용 + XFF 신뢰 조건 부재 | GPT·검증 |
| P1 | 백엔드 | Flyway 마이그레이션이 어떤 테스트로도 검증 안 됨 + 보너스 중복 시 500 | 추가 + GPT |
| P1 | 프론트 | 운영 화면이 호출하는 `/ops-api/*`가 Next 라우트/리라이트로 정의돼 있지 않음 | GPT·검증 |
| P2 | 프론트 | BFF 프록시 중복·timeout 부재·JSON 파싱 실패 시 status 손실 | GPT·검증 |
| P2 | 프론트 | `OpsDashboardClient`/`SavedNumbersClient`에 try/catch 부재 → 오류 시 로딩 고착 | GPT·검증 |
| P2 | 프론트 | 합계 구간(sum bucket) 문자열 정렬로 화면 순서 뒤바뀜 | GPT·검증 |

---

# 2. 백엔드

## P0

### 2.1 관리자 Thymeleaf 템플릿 ↔ DTO 불일치 (렌더링 500) `[GPT·검증]`

**가장 시급.** API DTO가 record로 리팩터링됐으나 관리자 템플릿 일부가 옛 필드명을 그대로 참조한다. 프론트 `rounds/page.tsx`는 이미 새 필드(`items`)를 쓰고 있어, **템플릿만 갱신 누락**된 것이 확증된다.

현재 DTO:

```java
record WinningNumberResponse(int round, LocalDate drawDate, List<Integer> numbers,
                             int bonusNumber, long firstPrizeAmount, ...) {}
record WinningNumberListResponse(List<WinningNumberResponse> items, int page,
                                 int size, long totalElements, int totalPages) {}
```

깨진 참조(실제 확인):

| 파일 | 잘못된 참조 | 현재 DTO 필드 |
|---|---|---|
| `dashboard.html` | `latest.num1`~`num6`, `latest.bonusNum` | `latest.numbers`(List), `latest.bonusNumber` |
| `rounds.html` | `rounds.content`, `rounds.number` | `rounds.items`, `rounds.page` |
| `rounds.html` | `r.num1`~`num6`, `r.bonusNum` | `r.numbers`, `r.bonusNumber` |
| `audit.html` | (정상) | 컨트롤러가 실제 `Page<AdminAuditLog>` 반환 → `content`/`number` 유효 |

존재하지 않는 속성 접근 시 Thymeleaf는 `SpelEvaluationException`(EL1008E)을 던지며 페이지 렌더가 중단된다. 즉 **`/admin/dashboard`의 당첨번호 카드와 `/admin/rounds` 전체가 500**으로 떨어진다.

**개선 A — 템플릿을 현재 DTO에 맞게 수정:**

```html
<!-- dashboard.html: 당첨 번호 -->
<p>
  <span th:each="n, stat : ${latest.numbers}">
    <span th:text="${n}"></span><span th:if="${!stat.last}">, </span>
  </span>
  <span th:text="' + ' + ${latest.bonusNumber}"></span>
</p>
```

```html
<!-- rounds.html -->
<tr th:each="r : ${rounds.items}">
  <td th:text="${r.round}"></td>
  <td th:text="${r.drawDate}"></td>
  <td>
    <span th:each="n, stat : ${r.numbers}">
      <span th:text="${n}"></span><span th:if="${!stat.last}">, </span>
    </span>
    <span th:text="' + ' + ${r.bonusNumber}"></span>
  </td>
</tr>
<!-- 페이지네이션: rounds.number → rounds.page -->
<a th:if="${rounds.page > 0}" th:href="@{/admin/rounds(page=${rounds.page - 1})}">이전</a>
<span th:text="${rounds.page + 1} + ' / ' + ${rounds.totalPages} + ' 페이지'"></span>
```

**개선 B(권장) — 관리자 전용 ViewModel 분리:** 공개 API 응답과 관리자 화면의 요구가 다르므로, API DTO를 화면에 직접 묶지 않는 편이 장기적으로 안전하다.

```java
public record AdminRoundView(int round, LocalDate drawDate,
                             int num1,int num2,int num3,int num4,int num5,int num6,
                             int bonusNumber) {
    public static AdminRoundView from(WinningNumberResponse r) {
        var n = r.numbers();
        return new AdminRoundView(r.round(), r.drawDate(),
            n.get(0),n.get(1),n.get(2),n.get(3),n.get(4),n.get(5), r.bonusNumber());
    }
}
```

**근본 원인이 회귀 미탐인 점도 중요:** `build.gradle.kts`의 JaCoCo 설정이 `AdminController`·`AdminLoginHandler` 등을 커버리지에서 제외("smoke test로 커버")하지만 정작 관리자 화면 스모크 테스트가 없다. → MockMvc로 `/admin/dashboard`·`/admin/rounds` 200 렌더를 검증하는 테스트를 추가하면 이런 템플릿-DTO 드리프트를 자동 탐지할 수 있다.

---

## P1

### 2.2 통계 summary 재계산의 트랜잭션 분리 `[GPT·검증]`

`WinningStatisticsCacheService`는 클래스 레벨 `@Transactional(readOnly = true)`다. 그 안에서 summary가 비면 쓰기 작업 `summaryRebuilder.rebuildAllSummaries()`를 호출한다.

```java
@Transactional(readOnly = true)
public class WinningStatisticsCacheService {
    public FrequencyStatsResponse getFrequencyStats() {
        ...
        if (summaries.isEmpty()) { summaryRebuilder.rebuildAllSummaries(); ... }
    }
}
```

`StatisticsSummaryRebuilder.rebuildAllSummaries()`는 `@Transactional`(기본 propagation `REQUIRED`)이라 **이미 열린 read-only 트랜잭션에 합류**한다. Hibernate는 read-only 트랜잭션에서 `FlushMode.MANUAL`로 전환하므로, `saveAll(...)`이 영속성 컨텍스트에만 반영되고 **flush/commit 시 DB에 반영되지 않을 수 있다.** 직후 같은 트랜잭션의 JPQL 조회도 auto-flush가 막혀 빈 결과를 받는다. (`@CacheEvict`는 그대로 실행되어 더 헷갈린다.)

정상 운영에서는 `StatisticsRefreshListener`가 수집 이벤트의 `AFTER_COMMIT`(독립 트랜잭션)에서 채워주므로 가려져 있지만, **summary가 빈 상태에서 통계 API가 먼저 호출되는 콜드 경로**에서 재현된다.

**개선:** rebuild를 독립 쓰기 트랜잭션으로 분리하거나(권장), read 경로에서 rebuild를 호출하지 않는다.

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
@CacheEvict(value = {STATS_FREQUENCY, STATS_PATTERN, STATS_COMPANION}, allEntries = true)
public void rebuildAllSummaries() { ... }
```

권장 구조: ① 회차 수집/수정 완료 → ② 별도 쓰기 트랜잭션에서 재계산 → ③ 캐시 evict → ④ read API는 조회만.

### 2.3 운영 API 인증 보강 `[GPT·검증]` + `[추가]`

운영 엔드포인트(`/ops/**`)의 보호가 토큰 헤더 비교에만 의존한다.

```java
if (token == null || !expected.equals(token)) { throw new ApiException(...); }
```

확인된 문제:
- **Spring Security 체인 밖**: `WebSecurityConfig`는 `/api/**`·`/actuator/**`, `AdminSecurityConfig`는 `/admin/**`만 매칭한다. `/ops/**`는 어떤 SecurityFilterChain에도 매칭되지 않아 인증/인가 필터가 적용되지 않는다.
- **비상수시간 비교**: `String.equals`는 타이밍 공격에 노출된다.
- **rate limit 미적용**: `PublicRateLimitFilter`는 `/api/`만 대상이라 `/ops`는 무제한 → 토큰 브루트포스 표면.

**개선:**

```java
private boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) return false;
    return MessageDigest.isEqual(a.getBytes(UTF_8), b.getBytes(UTF_8));
}
```

추가로 ① `/ops/*`에도 rate limit, ② 토큰 실패 audit 기록, ③ 보안 체인에서 `/ops`를 명시적으로 다루기. 리버스 프록시 제한이 있더라도 앱 레벨 방어를 둔다.

### 2.4 클라이언트 IP 처리 일관화 + XFF 신뢰 조건 `[GPT·검증]` + `[추가]`

두 가지가 얽혀 있다.

**(a) admin 모듈이 프록시 IP를 사용한다.** 앱 전반은 `ClientIpResolver`(XFF 파싱)를 쓰지만 `AdminLockoutFilter`·`AdminLoginHandler`·`AdminController`(그리고 `RequestIdFilter`의 접속 로그)는 `req.getRemoteAddr()`만 쓴다. Caddy/Next 프록시 뒤에서는 모두 **프록시 IP**가 되어 → ① 감사 로그의 IP가 실제 클라이언트가 아니고, ② lockout 키가 `username + 프록시IP` → 사실상 username 단위(IP 구분 무의미).

**(b) `ClientIpResolver`가 즉시 피어 신뢰 여부와 무관하게 XFF를 신뢰한다.**

```java
public String resolve(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For"); // 무조건 읽음
    if (xff != null && !xff.isBlank()) { ... 우→좌 순회 ... }
    return request.getRemoteAddr();
}
```

`getRemoteAddr()`(실제 직전 피어)가 신뢰 프록시인지 먼저 확인하지 않는다. 백엔드 포트가 직접 노출되거나 신뢰 CIDR(`172.28.0.0/16`, /16로 넓음)이 느슨하면, 임의 XFF를 회전시켜 IP 기반 rate limit을 우회할 수 있다.

**개선:** 직전 피어가 신뢰 프록시일 때만 XFF를 채택하고, admin도 동일 resolver를 쓴다.

```java
public String resolve(HttpServletRequest request) {
    String remote = request.getRemoteAddr();
    if (!isTrustedProxy(remote)) return remote;              // 신뢰 피어가 아니면 XFF 무시
    String xff = request.getHeader("X-Forwarded-For");
    if (xff == null || xff.isBlank()) return remote;
    String[] parts = xff.split(",");
    for (int i = parts.length - 1; i >= 0; i--) {
        String c = parts[i].trim();
        if (!c.isBlank() && !isTrustedProxy(c)) return c;
    }
    return remote;
}
```

### 2.5 Flyway 마이그레이션 미검증 + 도메인 검증 누락 → prod 500 `[추가]` + `[GPT·검증]`

두 문제가 같은 뿌리(로컬/테스트 ≠ prod 스키마)를 공유한다.

- **마이그레이션이 어떤 테스트로도 검증되지 않는다.** `build.gradle.kts`에 Testcontainers(mariadb)가 선언돼 있으나 이를 쓰는 테스트 클래스가 없다(死 의존성). 통합 테스트는 `@ActiveProfiles("test")` → H2 in-memory + `ddl-auto: create-drop` + `flyway.enabled: false`. 즉 prod의 MariaDB Flyway 스키마(CHECK 제약·DESC 인덱스·타입 매핑)와 Hibernate `ddl-auto: validate`의 정합성이 한 번도 검증되지 않는다.
- **보너스 번호 중복 검증이 서비스에 없다.** `WinningNumberCommandService.upsert`는 `LottoNumberCodec.normalize`(본번호 6개만 검사)만 호출하고 `bonusNumber`가 본번호와 겹치는지 보지 않는다. prod에선 V1의 `chk_winning_numbers_bonus` CHECK가 막아 `DataIntegrityViolationException` → (전용 핸들러 없음) → `handleUnexpected` → **generic 500**. 반면 로컬 H2(ddl-auto, 엔티티에 `@Check` 없음)는 CHECK 제약이 생성되지 않아 **조용히 통과** → 전형적 드리프트.

**개선:**
1. Testcontainers MariaDB + Flyway로 마이그레이션·`validate` 스모크 테스트 추가(死 의존성 활성화).
2. 서비스 레벨에서 미리 400 처리:

```java
public WinningNumberResponse upsert(WinningNumberUpsertRequest request) {
    var normalized = lottoNumberCodec.normalize(request.numbers());
    if (normalized.contains(request.bonusNumber())) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BONUS_NUMBER",
            "보너스 번호는 당첨 번호 6개와 중복될 수 없습니다.");
    }
    ...
}
```

추가 검증 권장(범위·날짜·금액 상한 등): `drawDate` 미래 제한, `round` 중복/역행 정책, 금액 필드 상한.

---

## P2

### 2.6 통계 재계산 시 stale row 제거 `[GPT·검증]`

`StatisticsSummaryRebuilder`에서 frequency는 1~45 전체를 갱신하지만, **pattern/companion은 새로 계산된 key만 upsert**하고 더 이상 등장하지 않는 기존 row를 0으로 만들거나 삭제하지 않는다.

```java
// upsertPatternRows / rebuildCompanions: 현재 key만 update/insert, 사라진 key는 그대로 잔존
```

odd/high/sum bucket 키는 고정이라 영향이 작지만, **회차가 수정·삭제되어 어떤 동반 쌍이 더 이상 등장하지 않게 되면** 그 쌍의 옛 카운트가 `companion_pair_summary`에 남아 `getCompanionStats`(co_count 상위 100)에 과대 집계로 노출된다.

**개선(스냅샷 재생성):** 현재 데이터 규모에서 비용이 작으므로 안정성을 우선한다.

```java
patternStatsSummaryRepository.deleteAllInBatch();
companionPairSummaryRepository.deleteAllInBatch();
patternStatsSummaryRepository.saveAll(newPatternRows);
companionPairSummaryRepository.saveAll(newCompanionRows);
```

### 2.7 `dataChanged` 항상 true → 불필요한 전체 재계산 `[추가]`

`WinningNumberCollectionService.collectRound`가 항상 `new WinningNumbersCollectedEvent(round, true)`를 발행한다. 동일 데이터 재수집(upsert가 실제로 아무것도 바꾸지 않음)에도 매번 ① 통계 전체 재계산, ② 6개 경로 ISR revalidate 네트워크 호출이 발생한다.

**개선:** `upsert`가 실제 변경 여부(신규/필드 변경)를 반환하고, 그 값으로 이벤트의 `dataChanged`를 채운다.

### 2.8 Actuator 노출 범위 재검토 `[GPT·검증]`

```java
.securityMatcher("/api/**", "/actuator/**")
.authorizeHttpRequests(a -> a.anyRequest().permitAll())   // actuator 전체 permitAll
```
```yaml
management.endpoints.web.exposure.include: health,info,prometheus
```

`/actuator/prometheus`(내부 메트릭)·`/actuator/info`가 앱 레벨에서 무인증 허용이다. 네트워크가 잘못 노출되면 메트릭/빌드 정보가 새어나간다.

**개선:** 앱 레벨에서 health만 공개하고 prometheus는 내부망/리버스 프록시 allowlist로 제한(스크래핑 주체가 인증할 수 없다면 네트워크 레벨 제한이 현실적). 최소한 `info` 공개 여부를 재검토한다.

### 2.9 CORS·ops 호스트 게이트의 fail-open 기본값 `[GPT·검증]` + `[추가]`

- `CorsConfig`: `kraft.public-base-url` 미설정 시 `allowedOriginPatterns`가 `*`로 열림.
- (프론트) `proxy.ts`: `KRAFT_OPS_ALLOWED_HOST` 미설정 시 호스트 검증 자체를 건너뜀.

운영에서 env 누락 시 보호가 조용히 사라진다.

**개선:** prod 프로파일에서 필수 env 미설정 시 기동 실패(fail-fast).

```java
if (isProd && (publicBaseUrl == null || publicBaseUrl.isBlank()))
    throw new IllegalStateException("KRAFT_PUBLIC_BASE_URL is required in prod");
```

### 2.10 device token 모델 보강 `[GPT·검증]`

프론트 생성 토큰을 `X-Device-Token`으로 받아 SHA-256 해시로 저장한다. 이 토큰은 사실상 저장 번호 접근 권한이며 localStorage에 있어 XSS 시 탈취 가능하다.

**단기:** 길이/형식 검증, 너무 짧은 토큰 거부, rate limit.

```java
if (token == null || token.length() < 32 || token.length() > 128)
    throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DEVICE_TOKEN", "잘못된 기기 식별 토큰입니다.");
```

**중기:** 서버 발급 opaque token + `HttpOnly`/`Secure`/`SameSite` 쿠키.
> 주의: SHA-256 → HMAC+서버 pepper로 바꾸면 해시가 달라져 기존 저장 데이터 접근이 끊긴다. 적용 시 마이그레이션/병행 검증 필요.

### 2.11 기타 P2 `[추가]`

- **lockout/rate-limit이 인메모리(Caffeine)**: 재시작 시 소실, 다중 인스턴스 미공유 → 스케일아웃 시 우회 가능(현재 단일 인스턴스면 무방).
- **`STATS_FREQUENCY` 캐시 키 혼용**: `getFrequencyStats()`(키 없음=`SimpleKey.EMPTY`)와 `getFrequencyStatsByLimit(#limit)`가 같은 캐시 이름을 공유한다. 타입이 달라 충돌은 없으나 `maximumSize=4`에 4종이 빠듯하고 의미가 섞인다 → 별도 캐시 이름 권장.

## P3 `[추가]`

- **관리자 초기 계정 부트스트랩 경로 부재** `[GPT·검증]`: `V7__admin_users.sql`은 빈 테이블만 만들고, 최초 관리자 생성 코드가 없다. → one-time bootstrap 커맨드(권장) 또는 env 기반 1회 생성(생성 후 env 제거) 중 하나를 명확화·문서화. 매 기동 자동 생성은 지양.
- **`raw_json` 컬럼 미사용**: 매퍼가 항상 `null`을 넘겨 V4의 TEXT 컬럼이 늘 NULL. 보존 목적이면 원문 저장, 아니면 제거.
- **`idx_wn_draw_date`(V8) 사용처 불명**: draw_date로 정렬/필터하는 쿼리가 없다(모두 round_no 기준, unique 인덱스 역방향 사용). EXPLAIN으로 필요성 재검토. 또한 MariaDB 버전에 따라 인덱스의 `DESC`가 무시될 수 있다.
- **H2가 prod 번들에 포함**: `runtimeOnly("com.h2database:h2")` 무조건 → prod JAR에 H2 동봉(콘솔 CVE 이력). 콘솔 비활성이라 위험은 낮으나 불필요 → `testRuntimeOnly` 또는 dev 프로파일로 분리.
- **`@EnableScheduling` 중복**: `Application`과 `SchedulerLockConfig` 양쪽 선언(무해, 정리 권장).

---

# 3. 프론트엔드

## P1

### 3.1 `/ops-api/*` 호출 경로가 코드상 정의되지 않음 `[GPT·검증]`

`OpsDashboardClient`가 다음을 호출한다.

```ts
fetch("/ops-api/summary"); fetch("/ops-api/collect/latest");
fetch(`/ops-api/collect/${roundNumber}`); fetch("/ops-api/rounds");
```

그러나 `app/`에 `/ops-api/*` route handler가 없고 `next.config.ts`에도 rewrite가 없다(현재 `next.config.ts`는 `/data-source` 301만 정의). 즉 **리버스 프록시(Caddy)에만 의존**하며, 로컬 `next dev`/preview에서는 운영 화면이 동작하지 않고 환경마다 동작이 달라진다.

**개선 B(권장) — `next.config.ts` rewrite로 명시:**

```ts
async rewrites() {
  return [{ source: "/ops-api/:path*",
            destination: `${process.env.KRAFT_BACKEND_INTERNAL_URL}/ops/:path*` }];
}
```

또는 **개선 A** — `app/ops-api/.../route.ts` 핸들러로 직접 프록시(아래 3.3의 공통 유틸 재사용).
> 운영 토큰이 브라우저에서 직접 다뤄지는 구조이므로, 화면 접근 자체를 인증으로 보호하고 토큰 장기 저장을 피하며 Origin/Referer 검증·rate limit을 함께 검토한다.

### 3.2 `proxy.ts` 미들웨어 적용 확인 `[추가]`

CSP nonce 주입과 `/ops` 호스트 게이팅이 `web/src/proxy.ts`의 `proxy` 함수에 들어 있다. Next 16의 proxy(구 middleware) 규약과 일치하는지 확인이 필요하다. 만약 규약 불일치로 호출되지 않으면 **CSP 헤더와 ops 호스트 격리가 조용히 사라진다.** 빌드 로그/응답 헤더(`Content-Security-Policy`, `x-nonce`)로 실제 적용 여부를 검증할 것. (파일 위치는 `web/src/proxy.ts`로 src 루트에 있어 위치 자체는 적절.)

---

## P2

### 3.3 BFF 프록시 중복·timeout 부재·status 손실 `[GPT·검증]`

다음 route가 거의 동일한 프록시 로직을 반복한다: `api/v1/numbers/recommend`, `api/v1/saved`, `api/v1/saved/[id]`, `api/v1/stats/analysis`, `api/v1/stats/frequency`.

공통 문제(코드 확인):
- `const backendBaseUrl = process.env... ?? "http://backend:8080"` 반복(`lib/api.ts` 포함 6곳).
- **timeout 없음**: `lib/api.ts`의 서버 함수는 `AbortSignal.timeout(5000)`을 쓰지만, BFF route들은 `await fetch(...)`에 시그널이 없어 백엔드 지연 시 그대로 매달린다.
- **status/바디 손실 가능**: `const data = await res.json()` — 백엔드가 빈 바디/비JSON을 줄 때 throw → catch → generic 502가 되어 실제 status가 사라진다.

**개선 — 공통 유틸 `web/src/lib/backend-proxy.ts`:**

```ts
const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

async function safeJson(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return { message: text }; }
}

export async function proxyBackend(path: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 5000);
  try {
    const res = await fetch(`${backendBaseUrl}${path}`, { ...init, signal: controller.signal, cache: "no-store" });
    return Response.json(await safeJson(res), { status: res.status });
  } catch {
    return Response.json({ code: "BACKEND_UNAVAILABLE", message: "백엔드 요청에 실패했습니다." }, { status: 502 });
  } finally { clearTimeout(timer); }
}
```

route는 `return proxyBackend("/api/v1/...", { method, headers, body })` 한 줄로 축약된다. `X-Device-Token`/`X-Request-Id`는 init.headers로 전달.

### 3.4 클라이언트 네트워크 예외 처리 `[GPT·검증]`

`OpsDashboardClient`의 `loadSummary`/`collectLatest`/`collectRound`/`submitManualEntry`와 `SavedNumbersClient`의 `handleSubmit`/`handleDelete`는 **try/catch가 없다.**

```ts
const response = await fetch("/ops-api/summary", {...});
const payload = await readJson<OpsSummary>(response);
setLoadingAction(null);   // ← fetch/json이 throw하면 이 줄이 실행되지 않음
```

`fetch`/`json`이 throw하면 이후 `setLoadingAction(null)`이 실행되지 않아 **버튼이 영구 비활성(로딩 고착)**되고 미처리 예외가 발생한다. (반면 `RecommendClient`·`AnalysisClient`는 try/catch가 있어 양호 → 일관성 부재.)

**개선 — 모든 action을 동일 패턴으로:**

```ts
setLoading(true); setMessage("");
try {
  const res = await fetch(...);
  const payload = await safeJson(res);
  if (!res.ok) { setMessage(payload.message ?? "요청에 실패했습니다."); return; }
  ...
} catch { setMessage("네트워크 오류가 발생했습니다."); }
finally { setLoading(false); }
```

### 3.5 device token 공통화 + fallback 강화 `[GPT·검증]` + `[추가]`

`recommend-client.tsx`(`deviceStorageKey`)와 `saved-numbers-client.tsx`(`storageKey`)에 동일 로직·동일 키 값(`"kraft-device-token"`)이 복붙돼 있다(상수명만 다름). 한쪽 키만 바뀌면 저장/추천 디바이스가 어긋난다. 또 `Math.random()` fallback은 식별 토큰으로 약하다.

**개선 — `web/src/lib/device-token.ts`로 단일화:**

```ts
const storageKey = "kraft-device-token";
export function getDeviceToken(): string {
  const existing = window.localStorage.getItem(storageKey);
  if (existing) return existing;
  const created = crypto.randomUUID?.() ?? createRandomToken();
  window.localStorage.setItem(storageKey, created);
  return created;
}
function createRandomToken(): string {
  const b = new Uint8Array(32); crypto.getRandomValues(b);
  return Array.from(b, x => x.toString(16).padStart(2, "0")).join("");
}
```

### 3.6 합계 구간(sum bucket) 정렬 오류 `[GPT·검증]`

`stats/page.tsx`의 `PatternSection`이 문자열 정렬을 쓴다.

```ts
const sorted = [...buckets].sort((a, b) => a.bucketKey.localeCompare(b.bucketKey));
```

odd/high(키 `0`~`6`)는 문제없지만, **합계 구간 키**(`21-65`,`66-110`,`111-155`,`156-200`,`201-255`)는 문자열 정렬 시 다음처럼 뒤바뀐다.

```
111-155, 156-200, 201-255, 21-65, 66-110   (잘못)
```

(백엔드 `findByStatTypeOrderByBucketKeyAsc`도 `bucket_key` 문자열 정렬이라 동일 결함을 공유한다.)

**개선 — 명시적 순서 적용:**

```ts
const SUM_ORDER = ["21-65","66-110","111-155","156-200","201-255"];
function sortBuckets(b: PatternBucket[], order?: string[]) {
  return order
    ? [...b].sort((x,y) => order.indexOf(x.bucketKey) - order.indexOf(y.bucketKey))
    : [...b].sort((x,y) => Number(x.bucketKey) - Number(y.bucketKey)); // odd/high
}
```

추가로 `/info/methodology` 설명의 구간 정의가 실제 경계(`21-65` 등, 45 단위)와 일치하는지 확인한다.

### 3.7 입력값 검증 UX 공통화 `[GPT·검증]`

추천/저장 입력은 잘못된 값을 조용히 제거한다(`.filter(v => !Number.isNaN(v) && v > 0)`). `1, 2, abc, 3` 입력 시 무엇이 틀렸는지 알려주지 못한다. (`AnalysisClient`는 6개·NaN 검사로 안내 메시지를 띄움 → 통일 필요.)

**개선 — 공통 validator `web/src/lib/lotto-validation.ts`:** 개수·정수·1~45·중복을 검사해 `{ ok, numbers | message }`를 반환하고 추천/저장/분석/운영 입력에 공통 적용.

### 3.8 프론트 테스트 보강 `[GPT·검증]` + `[추가]`

현재 테스트는 동작이 아닌 존재만 확인한다. `api-url.test.ts`는 env 폴백을 테스트한다면서 `expect(typeof getPublicBaseUrl).toBe("function")`만 단언해 **항상 통과(no-op)**한다. 정확성 핵심인 `tax.ts`와 `api.ts` 에러 처리에는 테스트가 없다.

**개선:** route handler(status/body 전달), `BackendError`(code/message/status 보존), validator(범위/중복/개수), 컴포넌트 성공/실패 UX, sum bucket 정렬, ops 실패 시 로딩 해제 테스트 추가.

### 3.9 세금 계산 임계값 검증 `[추가]`

`tax.ts`의 22% 구간 하한이 `2,000,000`원이다. 한국 로또 기타소득세 비과세 기준(5만원)과 불일치 소지가 있다. 현재 화면은 1등(항상 3억 초과)에만 적용해 33% 분기가 맞게 동작하므로 **즉시 버그는 아니나**, 소액(2·3등)에 재사용되면 과세 누락 표시가 된다. 현행 소득세법 기준으로 임계값을 확인할 것.
> (세무 수치 자체는 단정하지 않는다 — 적용 전 법령 확인 권장.)

## P3

- **SSR 페이지 폴백 불일치** `[GPT·정정]`: `stats`/`frequency`/`companion`/`rounds` 페이지는 `await ...`만 하고 try/catch가 없어 백엔드 장애 시 라우트의 `error.tsx` 경계로 넘어간다. `latest`/`rounds/[round]`는 자체 폴백 UI를 가진다. **`error.tsx`가 존재해 잡아주므로 깨지는 것은 아니고, UX 일관성 항목**이다. 공개 핵심 페이지에 통일된 폴백 정책을 적용할지 결정.
- **`useTransition`을 비동기 로딩에 사용** `[GPT·정정]`: GPT는 결함으로 봤으나, **이 프로젝트의 React 19.2.7에서는 async transition이 정식 기능(Actions)**이라 `isPending`이 비동기 완료까지 유지된다 → 결함 아님. 다만 transition 내부에서 예외를 잡는 것은 필요(3.4와 연계). `frequency-filter-client.tsx`는 내부 try/catch가 있어 양호.
- **sitemap에 `/recommend` 누락** `[GPT·검증]` + `[추가]`: 공개·인덱스 가능한데 sitemap에 없다(불일치). 노출 대상이면 추가, 아니면 의도 명확화. `/saved`는 noindex+제외가 적절(현행 유지).
- **`frequency` BFF의 `limit` 미인코딩** `[추가]`: `?limit=${limit}` 직접 보간(백엔드 화이트리스트가 막아 위험 낮음) → `encodeURIComponent` 권장.
- **revalidate 시크릿 비상수시간 비교** `[추가]`: `revalidate/route.ts`의 `secret !== env` 타이밍(영향 낮음).
- **`rounds/[round]` 백엔드 5xx도 `notFound()` 처리** `[추가]`: 서버 오류가 404로 표시됨 → 에러/없음 구분 권장(낮음).
- **ISR `revalidate` + `AbortSignal.timeout` 혼용** `[추가]`: `lib/api.ts` GET이 `next:{revalidate}`와 signal을 함께 쓴다 → Next 캐시 적용/경고 여부 확인.

---

# 4. 우선 적용 순서

**1단계 — 깨졌거나 운영 장애 직결:**
1. 관리자 템플릿 DTO 불일치 수정(2.1) + 관리자 화면 스모크 테스트
2. 통계 rebuild 트랜잭션 분리(2.2)
3. `/ops-api/*` 라우트/리라이트 명시(3.1)
4. ClientIpResolver XFF 신뢰 조건 + admin IP 일관화(2.4)
5. 보너스 중복 서비스 검증(2.5)

**2단계 — 운영 안정성:**
1. 운영 API constant-time + rate limit + 보안 체인 명시(2.3)
2. Actuator/prometheus 보호(2.8), CORS·ops 게이트 fail-fast(2.9)
3. Flyway 마이그레이션 Testcontainers 검증(2.5)
4. BFF 공통 프록시(timeout·safeJson·status 보존)(3.3), 클라이언트 try/catch 통일(3.4)
5. 통계 stale row 제거(2.6), `dataChanged` 정확화(2.7)

**3단계 — UX·유지보수:**
1. 로또 번호 validator 공통화(3.7), device token 모듈화(3.5)
2. sum bucket 정렬(3.6), sitemap·tax 확인(3.8/3.9)
3. 테스트 보강(2.1 스모크 / 3.8), 그 외 P3 정리

---

# 5. 체크리스트

## 백엔드
- [ ] `dashboard.html`·`rounds.html`을 현재 DTO(`numbers`/`bonusNumber`/`items`/`page`)로 수정 — `audit.html`은 변경 불필요
- [ ] 관리자 전용 ViewModel 도입 검토
- [ ] `/admin/dashboard`·`/admin/rounds` 렌더 200 스모크 테스트 추가
- [ ] `rebuildAllSummaries()`에 `REQUIRES_NEW` 적용(또는 read 경로에서 rebuild 제거)
- [ ] 운영 토큰 비교 `MessageDigest.isEqual`로 변경
- [ ] `/ops/*` rate limit + 토큰 실패 audit + 보안 체인 명시
- [ ] `ClientIpResolver`: 직전 피어가 신뢰 프록시일 때만 XFF 사용
- [ ] admin lockout/audit(+RequestIdFilter 로그)도 `ClientIpResolver` 사용
- [ ] Testcontainers MariaDB + Flyway 스모크 테스트(死 의존성 활성화)
- [ ] `upsert`에 보너스↔본번호 중복 400 검증
- [ ] pattern/companion 재계산 시 stale row 제거(스냅샷 재생성)
- [ ] `WinningNumbersCollectedEvent.dataChanged`를 실제 변경 여부로
- [ ] `/actuator/prometheus`·`info` 접근 제한
- [ ] prod CORS/ops 호스트 env 누락 시 fail-fast
- [ ] device token 길이/형식 검증
- [ ] 관리자 초기 계정 부트스트랩 절차 명확화·문서화
- [ ] (정리) raw_json/idx_wn_draw_date/H2 prod 번들/`@EnableScheduling` 중복

## 프론트엔드
- [ ] `web/src/lib/backend-proxy.ts` 생성(timeout·safeJson·status 보존) 후 BFF route 일괄 적용
- [ ] `/ops-api/*` rewrite 또는 route handler 추가
- [ ] `proxy.ts`가 실제 적용되는지(CSP/nonce 헤더) 검증
- [ ] `OpsDashboardClient`·`SavedNumbersClient` action에 try/catch/finally
- [ ] `web/src/lib/device-token.ts`로 토큰 로직 단일화 + `crypto.getRandomValues` fallback
- [ ] `web/src/lib/lotto-validation.ts` 공통 validator 적용(추천/저장/분석/운영)
- [ ] `stats/page.tsx` sum bucket 명시적 정렬(+백엔드 정렬도 정리), methodology 문구 일치
- [ ] `/recommend` sitemap 포함 여부 결정
- [ ] route/component/validator/BackendError/tax 테스트 보강
- [ ] (낮음) SSR 폴백 정책 통일, `limit` 인코딩, revalidate 시크릿 비교, 5xx vs notFound 구분
- [ ] (정정 반영) `useTransition`은 React 19에서 정상 — 결함 처리하지 말 것

---

# 6. 결론

먼저 고칠 5가지:

1. **관리자 템플릿(`dashboard.html`·`rounds.html`)을 현재 DTO에 맞추거나 ViewModel을 도입한다** — 현재 두 화면은 렌더링 500 상태다.
2. **`StatisticsSummaryRebuilder.rebuildAllSummaries()`를 독립 쓰기 트랜잭션으로 분리**해 read-only flush 누락을 막는다.
3. **운영 API를 constant-time 비교·rate limit·보안 체인으로 보강**하고, `ClientIpResolver`가 신뢰 피어에서 온 XFF만 신뢰하도록 고친다(admin IP 일관화 포함).
4. **`/ops-api/*` 처리 위치를 Next 코드(rewrite 또는 route handler)로 명시**해 환경 의존성을 제거한다.
5. **Flyway 마이그레이션을 Testcontainers로 검증**하고, 보너스 중복 등 도메인 규칙을 서비스 레벨에서 400으로 처리해 로컬(H2)과 prod(MariaDB) 드리프트를 없앤다.

이후 BFF 프록시 공통화·클라이언트 예외 처리·입력 검증·정렬·테스트 보강을 순차 진행한다. GPT 분석의 핵심 지적(특히 관리자 템플릿 불일치, 합계 구간 정렬, 운영 경로)은 코드로 모두 사실 확인되었고, `useTransition`/SSR 폴백 항목은 React 19·`error.tsx` 기준으로 경중을 조정했다.
