# improvement.md — 백엔드·운영 성숙도 개선 계획 (2026-05 라운드)

> **평가 기준일:** 2026-05-31
> **스택:** Java 25 · Spring Boot 4.0.x · MariaDB · Thymeleaf/HTMX
> **이전 라운드:** 웹 디자인 개선(STEP 0–6) 및 백엔드 B1–B8 완료 → 본 문서로 재작성
> **추적:** 이행 현황은 `docs/complement.md` 에 기록

---

## 0. 범위

### 0-A. 본 계획에서 제외 (오너 결정 — 의도된 선택)

| 제외 항목 | 사유 |
|---|---|
| Java 25 / Spring Boot 4 마이그레이션 | 의도된 스택 선택. 변경 대상 아님 |
| 문서화 정책 (README 단순화 등) | 의도된 문서 전략. 변경 대상 아님 |

### 0-B. 철회된 지적 (검증 결과 결함 아님)

| 항목 | 검증 결과 |
|---|---|
| `SmokLottoApiClient` 오타 의심 | **철회.** Javadoc상 `kraft.api.client=smok` 스모크 테스트 클라이언트로 의도된 식별자(mock/smok/real 4글자 토큰 정합). 클래스명이 설정 토큰을 의도적으로 미러링 |

### 0-C. 본 계획의 대상 (냉정 평가에서 도출된 실제 개선점)

| 우선순위 | 영역 | 핵심 | 가치 | 규모 |
|:--:|---|---|:--:|:--:|
| **P1** | 관찰 가능성(Observability) | 메트릭이 수집만 되고 외부로 노출 안 됨 | 높음 | 중 |
| **P2** | Ops 감사 추적(Audit trail) | 상태 변경 액션의 감사 기록 부재 | 높음 | 중 |
| **P3** | 품질 게이트 강제 + 커버리지 | 정적분석·커버리지 게이트가 opt-in이라 미강제 | 높음 | 소~중 |
| **P4** | 복원력 컴포넌트 빌드 vs 도입 | 자체 구현 서킷브레이커/레이트리미터의 유지 방침 미정 | 중 | 소 |
| **P5** | 가치 명제 투명성 | 추천 로직의 수학적 한계가 UI에 미반영 | 낮음 | 소 |

---

## P1 — 관찰 가능성 (Observability)

> **문제 요약:** `RecommendMetricsRecorder` 가 Micrometer 메트릭을 수집하지만, Prometheus 레지스트리 의존성도 없고 `/actuator/prometheus` 도 노출되지 않아 **메트릭이 어디로도 나가지 않는다.** 수집 파이프라인·서킷브레이커는 메트릭 자체가 없다.

### O1. Prometheus 레지스트리 추가 + 엔드포인트 노출 (보호)

- **현황**
  - `build.gradle.kts` 에 `micrometer-registry-prometheus` 없음 (actuator의 micrometer-core만 존재)
  - `application.yml` → `management.endpoints.web.exposure.include: health,info` (prometheus 미포함)
- **변경**
  - `build.gradle.kts`: `implementation("io.micrometer:micrometer-registry-prometheus")` 추가
  - `application-prod.yml`: `management.endpoints.web.exposure.include` 에 `prometheus` 추가
  - **보안**: `/actuator/**` 는 기존 `ActuatorAccessFilter`(IP allowlist)로 이미 보호됨 → `/actuator/prometheus` 가 allowlist 적용 범위에 포함되는지 확인하고, 누락 시 경로 추가
- **완료 기준**
  - allowlist 내부에서 `GET /actuator/prometheus` → `kraft_recommend_*` 메트릭 노출
  - allowlist 외부에서 403/404
  - 노출 변경에 대한 `ActuatorAccessFilterTest` 시나리오 추가

### O2. `/ops/recommend/stats` 메트릭 정합성 (검증 중 발견)

- **현황** — 기록측 `RecommendMetricsRecorder` 와 조회측 `RecommendMetricsQueryService` 의 메트릭 이름이 어긋나 있어, `/ops/recommend/stats` 의 여러 필드가 **항상 0** 으로 보고된다
  - 조회측이 찾지만 **기록측이 전혀 적재하지 않는** 메트릭: `kraft.recommend.timeout.count`, `kraft.recommend.attempt.count`, `kraft.recommend.rejection.count`, `kraft.recommend.rejection.by.rule`
    → `RecommendStatsDto.timeoutCount / attemptCount / rejectionCount / rejectionsByRule` 가 항상 0 (허위 지표)
  - 기록측이 적재하지만 **조회측이 노출하지 않는** 메트릭: `kraft.recommend.request.count`
  - 정상: `kraft.recommend.generation.latency`(count/mean/max), `kraft.recommend.generation.failure{reason}` — `recordFailure` 가 이미 사유 태깅함
  - 부수: `RecommendMetricsQueryService.empty()` 는 호출되지 않는 죽은 메서드
- **변경**
  - 메트릭 이름을 기록/조회가 공유하는 단일 상수로 통일
  - 추천 경로(`LottoRecommender` / `ConstraintAwareLottoNumberGenerator`)에서 시도 횟수·규칙별 제외(rejection) 카운트를 **실제로 기록**하거나, 측정이 어려운 필드는 DTO/스냅샷에서 **제거**해 "0 고정" 허위 지표를 없앤다
  - 타임아웃은 `generation.failure{reason}` 로 이미 구분되므로 `timeout.count` 중복 여부 검토 후 정리
  - 사용하지 않는 `empty()` 제거
- **완료 기준**
  - `/ops/recommend/stats` 모든 필드가 실제 활동을 반영(테스트로 검증), 또는 측정 불가 필드 제거
  - 기록측·조회측 메트릭 이름 불일치 0건

### O3. 수집 파이프라인·서킷브레이커 도메인 메트릭

- **현황**
  - `ApiCircuitBreaker` 상태는 `/ops/circuit-breakers`(`OpsCircuitBreakerStatusDto`)로만 조회 가능 → 시계열 추세 불가
  - `LottoSingleDrawCollector` / `LottoRangeCollector` / `WinningNumberPersister` 의 성공·실패·지연 메트릭 없음
- **변경**
  - 서킷브레이커 상태를 Micrometer **Gauge** 로 노출 (`kraft.api.circuit.state` — 0=CLOSED,1=HALF_OPEN,2=OPEN), 상태 전이 카운터(`kraft.api.circuit.transitions{from,to}`)
    - `ApiCircuitBreaker.StateTransitionListener` 가 이미 존재 → 이 훅에 메트릭 기록을 연결
  - 수집 결과 카운터(`kraft.collect.outcome{result=inserted|updated|skipped|failed}`), fetch 지연 타이머
- **완료 기준**
  - 서킷브레이커 강제 open 시 gauge/transition 메트릭이 변화 (단위 테스트)
  - 수집 1회 실행 시 outcome 카운터 증가

### O4. (선택) 분산 추적

- 단일 서비스라 우선순위 낮음. 도입 시 `micrometer-tracing-bridge-otel` + OTLP exporter, `RequestIdFilter` 의 request-id 를 trace baggage 로 전파
- **선검증 필수**: Spring Boot 4.0.x 와 micrometer-tracing 버전 호환성

### O5. (선택) Grafana 대시보드 + 알림

- 프로비저닝 JSON(대시보드) + alert rule (서킷브레이커 OPEN 지속, 추천 타임아웃 급증, 수집 실패 연속) 저장소 포함
- P1 핵심(O1–O3) 완료 후 진행

---

## P2 — Ops 감사 추적 (Audit Trail)

> **문제 요약:** `/ops/collect` 등 상태 변경 액션에 IP allowlist + Bearer 토큰 접근 제어는 있으나, **누가·언제·무엇을·결과를** 남기는 감사 기록이 없다. Flyway V1–V14 에 audit 테이블 부재(확정).

### A1. 상태 변경 Ops 액션 DB 감사 기록

- **변경**
  - Flyway `V15__create_ops_audit_log.sql`: `ops_audit_log(id, occurred_at, request_id, client_ip, action, target, outcome, detail)` (+ `occurred_at` 인덱스)
  - `OpsAuditLogEntity` / `OpsAuditLogRepository` / `OpsAuditService`
  - `OpsController` / `OpsCollectionFacade` 의 상태 변경 핸들러에서 감사 기록 1건 적재
    - `request_id`: 기존 `RequestIdFilter` 와 상관
    - `client_ip`: 기존 `ClientIpResolver` 재사용
    - 토큰 식별자(주체)는 토큰 원문 금지 → 해시/라벨만
  - 모든 기록 값은 `LogSanitizer` 통과 (CR/LF·민감정보 제거)
- **완료 기준**
  - `POST /ops/collect` 성공·실패 각각 감사 레코드 1건 생성 (통합 테스트)
  - 토큰 원문이 DB·로그 어디에도 남지 않음

### A2. 구조화 감사 로그 라인

- **변경**: DB와 별개로 `logback-spring.xml` 에 audit 전용 로거(JSON 또는 key=value) — 외부 로그 수집기 연동 대비
- **결정 포인트**: 보존/조회 요건이 가벼우면 **로그 전용**으로 축소해 A1의 DB 테이블을 생략하는 선택지도 검토 (over-engineering 회피)

---

## P3 — 품질 게이트 강제 + 커버리지

> **문제 요약:** Checkstyle·SpotBugs·JaCoCo가 **구성은 되어 있으나 전부 opt-in** 이다. `strictStatic`/`strictCoverage` 기본값이 `false` 라, 플래그 없이 실행하면 정적분석은 `ignoreFailures=true`, 커버리지 검증은 `check` 의존성에서 제외된다 → **CI가 이 플래그를 넘기지 않으면 게이트가 사실상 장식.**

### G1. CI에서 게이트 강제 (최우선 — 고가치/저비용)

- **현황** (`build.gradle.kts`)
  - `ignoreFailures = !strictStatic` (Checkstyle/SpotBugs)
  - `if (strictCoverage) dependsOn("jacocoTestCoverageVerification")`
  - 두 플래그 모두 기본 `false`
- **변경**
  - `ci.yml` 의 검증 단계가 `./gradlew check -PstrictStatic=true -PstrictCoverage=true` 를 호출하는지 **확인**하고, 누락 시 추가
  - (CI 워크플로 수정은 오너 승인 영역 → 변경 전 합의)
- **완료 기준**: 정적분석 위반·커버리지 미달 시 CI 빌드 실패

### T1. 고위험 분기 테스트 보강

- **대상 (분기 커버리지 BRANCH 0.59 기준 가장 취약한 경로)**
  - `ApiCircuitBreaker`: `CLOSED→OPEN→HALF_OPEN→CLOSED` 전이, half-open 호출 상한, open 지속시간 경계
  - `PublicRateLimitFilter$FixedWindowCounter`: 윈도우 롤오버 경계, 한도 초과 직후 reset
  - `WinningNumberUpsertExecutor`: 재시도 분기별 outcome
- **완료 기준**: 위 클래스 분기 커버리지가 목표치 이상

### T2. 동시성 테스트 (자체 구현 `synchronized` 경로)

- **현황**: `ApiCircuitBreaker`, `FixedWindowCounter` 모두 `synchronized` 기반인데 다중 스레드 시나리오 테스트 부재
- **변경**: 멀티스레드 부하에서 상태 일관성·한도 정확성 검증 테스트 추가
- **완료 기준**: N-스레드 동시 호출에서 카운트/상태가 결정적 불변식 유지

### T3. 커버리지 임계값 단계적 상향

- **현황** (`build.gradle.kts`): LINE 0.76 · BRANCH **0.59** · METHOD 0.80 · CLASS 0.90
- **변경**: T1–T2 반영 후 BRANCH 0.59 → 0.68(1차) → 0.75(목표), LINE 0.76 → 0.80
- **완료 기준**: 상향된 임계값으로 `check -PstrictCoverage=true` 통과

---

## P4 — 복원력 컴포넌트: 빌드 vs 도입 (ADR)

> **문제 요약:** 서킷브레이커·레이트리미터를 검증된 라이브러리(Resilience4j/Bucket4j) 대신 직접 구현했다. 작고 테스트 가능하지만 edge case(half-open 동시성 등) 책임을 자체 부담한다. **유지 방침을 명문화**한다.

### R1. 결정 기록(ADR) 작성

- **변경**: `docs/adr/0001-resilience-build-vs-adopt.md` 신설
- **권고 (초안)**: **현행 자체 구현 유지** + P3-T2 동시성 테스트로 보강
  - 근거: (1) 코드가 작고 의존성 0, (2) 이미 `LongSupplier nanoTime` 주입으로 테스트 가능, (3) Spring Boot 4.0.x + Java 25 는 최신 스택이라 Resilience4j/Bucket4j **호환성 선검증 비용**이 큼
  - 재검토 트리거: 정책 복잡도 증가(슬라이딩 윈도우, 분산 레이트리밋 등) 시 라이브러리 채택 재평가
- **완료 기준**: ADR 머지, README/CLAUDE의 복원력 서술과 정합

---

## P5 — 가치 명제 투명성 (낮은 우선순위)

> **문제 요약:** 로또는 독립 시행이라 과거 패턴 필터링은 당첨 확률을 올리지 못한다. `ExclusionRule` 주석은 이를 정직하게 명시("당첨 확률을 높이기 위한 것이 아니라 편향된 조합을 회피")하지만, **이 정직함이 UI에는 드러나지 않는다.**

### V1. UI 면책 + 재프레이밍

- **변경**
  - 추천 카드 인근에 짧은 안내: "확률 예측이 아니라, 많은 사람이 고르는 편향된 조합(생일·연속수·등차 등)을 피해 **당첨 시 분할 가능성을 낮추는** 도구"
  - 실제 방어 가능한 가치(인기 조합 회피 → 공동 당첨 시 분배 인원 감소)로 메시지 정렬
- **완료 기준**: 과장 없는 안내 문구 노출, 접근성(WCAG) 위반 없음

---

## 검증 메모 (신뢰도 표기)

| 항목 | 상태 |
|---|---|
| Prometheus 의존성/노출 부재 | **확정** (`build.gradle.kts`, `application.yml` 직접 확인) |
| 추천 stats 기록/조회 메트릭 불일치 | **확정** (`RecommendMetricsRecorder` ↔ `RecommendMetricsQueryService` 대조) |
| `smok` 의도된 식별자 | **확정** (`SmokLottoApiClient` Javadoc: smok95.github.io 미러 + `LottoApiClientConfig.SMOK_TOKENS`) |
| Ops 감사 **DB 테이블** 부재 | **확정** (Flyway V1–V14 목록에 audit 테이블 없음) |
| Ops 감사 **로그 라인** 부재 | **확정** (`OpsController`·`OpsCollectionFacade` 에 `log.` 호출 없음 — 수동 트리거 흔적 0) |
| 정적분석·커버리지 게이트 opt-in | **확정** (`build.gradle.kts` 분석) |
| `ApiCircuitBreakerTest` 깊이 | **확정** — `@Test` 단 1개(`opensAndRecovers`). 경계·동시성 케이스 없음(T1–T2 근거) |
| CI가 strict 플래그 전달 여부 | **미확인** — `ci.yml` 확인 후 G1 확정 |

---

## 권장 진행 순서

1. **G1** (게이트 강제) — 가장 저비용·고효과, 이후 모든 변경의 안전망
2. **O1 + O2** (메트릭 노출 + 죽은 메트릭 수정) — 관찰 가능성 즉시 개선
3. **A1/A2** (감사 추적) — 운영 신뢰성
4. **O3 + T1/T2** (도메인 메트릭 + 위험 분기/동시성 테스트)
5. **R1** (ADR), **T3** (임계값 상향)
6. **P5 / O4 / O5** (선택·여유 시)

---

## 이행 체크리스트

```
[x] G1  CI에서 strictStatic/strictCoverage 강제 — ci.yml:58 사전 완료 확인
[x] O1  micrometer-registry-prometheus + /actuator/prometheus 노출(allowlist 보호)
[x] O2  /ops/recommend/stats 기록/조회 메트릭 정합성 정리(허위 0 지표 제거)
[x] O3  서킷브레이커 전이 카운터(kraft.api.circuit_breaker.transitions) + 수집 outcome/fetch 지연 메트릭
[x] A1  (결정) DB 테이블 생략 — A2 로그 전용으로 충분
[x] A2  OpsCollectionFacade: kraft.audit 로거로 구조화 감사 로그 기록
[x] T1  ApiCircuitBreakerTest: 1개→9개 (경계 + disabled + half-open 분기)
[x] T2  ApiCircuitBreakerTest 동시성 + FixedWindowCounter 경계/동시성 테스트
[x] T3  임계값 최대 상향 — LINE 0.76→0.82 · BRANCH 0.59→0.67 · METHOD 0.80→0.88 · CLASS 0.90→0.97
[x] R1  복원력 빌드 vs 도입 ADR — docs/adr/0001-resilience-build-vs-adopt.md
[x] V1  추천 카드 면책 문구 추가 (편향 회피 + 공동 당첨 분배 설명)
[x] O4  micrometer-tracing-bridge-otel + OTLP exporter, RequestIdFilter → span tag 전파
[x] O5  Grafana/Prometheus docker-compose observability 프로파일 + 대시보드 프로비저닝
```
