# complement.md — improvement.md 이행 현황

## 완료

- [x] **B1** `WwwRedirectFilter` 오픈 리다이렉트 차단 — apex 전용 allowlist + 고정 목적지 + CR/LF sanitize, 테스트 추가
- [x] **B2+B3** upsert 견고성 — `WinningNumberUpsertExecutor`(REQUIRES_NEW) 분리 + 비트랜잭션 외부 재시도, 단위·통합 테스트 추가
- [x] **B5** rate-limit 문서 정정 — CLAUDE.md "token bucket" → "fixed-window rate limiter, keyed per (client IP, request URI)"
- [x] **B6** MeterRegistry null-가드 제거 — `getIfAvailable(SimpleMeterRegistry::new)` 주입으로 전환, 11개 파일 null 가드 삭제
- [x] **B8** 사소한 일관성 정리 — `DhLotteryApiClient.count()` varargs clone, `OpsController` inline FQN → import

## 미이행 (의도적 제외)

- [ ] **B4** ~~락 예외 축소~~ — ShedLock `throws Throwable` 시그니처로 인해 철회 (improvement.md §0-D)
- [x] **B7** `RequiredConfigValidator` 분리 — `JdbcConnectivityValidator`, `ProdConfigValidator`, `ProfilePolicyValidator`로 책임 분리; 오케스트레이터 138줄로 축소
- [ ] **CI/CD·배포·HSTS·SEO** — 오너 수동 작업 (improvement.md §3·§4)
