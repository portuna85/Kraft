# complement.md — improvement.md 이행 현황

> **업데이트:** 2026-05-31

## 완료

### G1 — CI 품질 게이트
- [x] `ci.yml:58` 에 `-PstrictStatic=true -PstrictCoverage=true` 이미 적용됨 — **사전 완료 확인**

### O1 — Prometheus 레지스트리 + 엔드포인트 노출
- [x] `build.gradle.kts`: `micrometer-registry-prometheus` 의존성 추가
- [x] `application-prod.yml`: `prometheus` 엔드포인트 노출 추가
- [x] `/actuator/**` 는 기존 `ActuatorAccessFilter`(IP allowlist)가 보호 — `/actuator/prometheus` 포함

### O2 — /ops/recommend/stats 메트릭 정합성 수정
- [x] `RecommendStatsDto`: 허위 0 필드(`timeoutCount`, `attemptCount`, `rejectionCount`, `rejectionsByRule`) 제거
- [x] `RecommendStatsDto`: 실제 기록되는 `requestedSetCount` 필드 추가
- [x] `RecommendMetricsQueryService`: 기록측(`RecommendMetricsRecorder`)과 일치하는 메트릭명으로 수정
- [x] 사용하지 않는 `empty()` 메서드 제거

### A2 — Ops 구조화 감사 로깅 (DB 테이블 없이 로그 전용)
- [x] `OpsCollectionFacade`: `collectLatest(requestId, clientIp)` / `collectMissing(requestId, clientIp)` 시그니처 변경
- [x] `OpsCollectionFacade`: `AUDIT_LOG` (logger `kraft.audit`) 통해 action/outcome/requestId/clientIp/collected 구조화 기록
- [x] `OpsController`: `RequestIdFilter` MDC에서 requestId, `ClientIpResolver`에서 clientIp 추출 후 facade 전달
- [x] `OpsController`: `KraftSecurityProperties` 주입으로 trustedProxies 참조

### T1+T2 — 고위험 분기 + 동시성 테스트
- [x] `ApiCircuitBreakerTest`: 1개 → 9개 테스트
  - 경계: open 지속시간 1ns 미달, 정확히 만료, half-open 최대 호출 상한, half-open 실패 재오픈, 성공 후 실패 카운터 리셋
  - disabled 상태 불변
  - 동시성: N 스레드 동시 실패 → open 전이 일관성, acquire/failure 동시 호출 상태 일관성
- [x] `PublicRateLimitFilterTest`: `FixedWindowCounter` 단위 테스트 추가
  - 한도 초과 직후 remaining=0, retryAfter>0
  - 윈도우 롤오버 시 카운트 리셋
  - 동시성: N 스레드 동시 요청 시 허가 수 ≤ maxRequests

## 미이행 (의도적 제외 또는 선택 보류)

- [ ] **O3** 서킷브레이커 state gauge + 수집 outcome 메트릭 — 규모 중간, 향후 진행
- [ ] **T3** 커버리지 임계값 상향 (BRANCH 0.59→0.68→0.75) — T1/T2 반영 후 재측정 필요
- [ ] **R1** 복원력 ADR — 방침 명문화 보류
- [ ] **V1** 추천 UI 면책/재프레이밍 — 낮은 우선순위
- [ ] **O4** 분산 추적 — 선택
- [ ] **O5** Grafana 대시보드 + 알림 — 선택, O1–O3 완료 후
