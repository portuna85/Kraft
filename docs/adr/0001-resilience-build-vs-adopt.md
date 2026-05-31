# ADR-0001: 복원력 컴포넌트 — 자체 구현 유지 vs 라이브러리 채택

| 항목 | 내용 |
|---|---|
| **상태** | 수락(Accepted) |
| **결정일** | 2026-05-31 |
| **검토 예정** | 슬라이딩 윈도우 레이트리밋 또는 분산 레이트리밋 요건 발생 시 |

---

## 맥락

현재 프로젝트에는 두 가지 복원력 컴포넌트가 직접 구현되어 있다.

| 컴포넌트 | 클래스 | 방식 |
|---|---|---|
| 서킷브레이커 | `ApiCircuitBreaker` | `closed → open → half-open` 3상태, `synchronized` |
| 레이트리미터 | `PublicRateLimitFilter$FixedWindowCounter` | Fixed-window, IP×URI 키, `synchronized` |

대안 라이브러리:
- **Resilience4j** — 서킷브레이커, 슬라이딩 윈도우 레이트리밋, 타임아웃, Bulkhead 지원
- **Bucket4j** — 토큰 버킷/리키버킷 레이트리밋, Redis 분산 지원

## 결정

**현행 자체 구현을 유지한다.**

## 근거

1. **의존성 최소화**: 라이브러리 채택 시 Resilience4j 기준 2~3개 추가 JAR, Spring Boot 4.0.x + Java 25 조합에서 **호환성 검증 비용**이 크다.

2. **코드 규모**: `ApiCircuitBreaker` 120줄, `FixedWindowCounter` 30줄. 라이브러리 학습·설정 비용보다 현행 구현 이해 비용이 낮다.

3. **테스트 가능성**: `LongSupplier nanoTime` 주입으로 시간을 완전 제어할 수 있어 결정적 단위 테스트가 가능하다. T2 동시성 테스트(멀티스레드 상태 일관성)까지 검증 완료.

4. **정책 단순성**: 현재 정책이 단순하다(fixed-window, 3상태 CB). 라이브러리의 추가 기능(슬라이딩 윈도우, Bulkhead, 이벤트 발행)은 현 요건에서 불필요.

5. **메트릭 배선 완료**: `ApiCircuitBreakerRegistry`가 Micrometer Gauge(`kraft.api.circuit_breaker.state`)와 전이 카운터(`kraft.api.circuit_breaker.transitions`)를 직접 등록 — 라이브러리 없이 관찰 가능성 확보.

## 재검토 트리거 (이 조건 중 하나 충족 시 ADR 재개)

- 레이트리밋을 **Redis 분산** 환경으로 확장해야 할 때
- **슬라이딩 윈도우** 또는 **IP 외 기준**(사용자 계정, API 키) 레이트리밋이 필요할 때
- `ApiCircuitBreaker` 버그 수정이 반복적으로 발생해 유지 비용이 라이브러리 전환 비용을 초과할 때

## 기각된 대안

| 대안 | 기각 이유 |
|---|---|
| Resilience4j 즉시 채택 | Spring Boot 4 호환성 미검증, 과도한 의존성 추가 |
| Bucket4j (레이트리밋만 교체) | Fixed-window로 충분, 전환 이점이 비용 미초과 |
