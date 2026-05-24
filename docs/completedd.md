# 작업 완료 기록

## 2026-05-24 P2/P1 진행
- 단위: 수집 오케스트레이션 분리
  - `CollectionRunState` 추가: 동시 실행 제어/상태 조회 책임 분리
  - `CollectionEventNotifier` 추가: 수집 결과 이벤트 발행 책임 분리
  - `LottoCollectionCommandService`는 orchestration 중심으로 단순화
- 단위: 운영 페이지 UX 개선
  - `admin-ops.html`에 `Reset` 버튼 추가
  - reason quick filter(`timeout/http/network`) 버튼 추가
  - `admin-ops.js`에 quick filter submit 동작 추가
- 단위: 접근성 보강
  - 운영 테이블 정렬 시 `aria-live` 안내(`ops-sort-announcer`) 추가
- 단위: 보안/프록시 신뢰 경계 테스트 보강
  - `OpsAccessFilterTest`: 비신뢰 프록시의 `X-Forwarded-For` 무시 검증 추가
  - `ActuatorAccessFilterTest`: 신뢰/비신뢰 프록시 분기 검증 추가
- 단위: 회귀 테스트 보강
  - `LottoCollectionCommandServiceTest`: running -> idle 상태 전이 검증 추가
  - `OpsPageControllerTest`: quick filter/reset 렌더링 검증 추가

## 검증
- `./gradlew.bat test --tests com.kraft.lotto.web.OpsPageControllerTest --tests com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandServiceTest --tests com.kraft.lotto.support.OpsAccessFilterTest --tests com.kraft.lotto.support.ActuatorAccessFilterTest`
- `python scripts/check_utf8.py`

## 2026-05-24 P2 잔여 추가 진행
- 단위: 운영 API 접근 제어 시나리오 보강
  - `OpsApiAccessScenarioTest` 추가
  - `GET /ops/collect/status` 기준으로 다음 시나리오 검증
    - 토큰 없음 -> 401
    - 잘못된 토큰 -> 401
    - allowlist 외 IP -> 403
    - 정상 토큰 + 허용 IP -> 200

- 단위: 모바일 viewport E2E 확장
  - Playwright 프로젝트 mobile-chrome(Pixel 7) 추가
  - 모바일 전용 smoke 추가: 하단 네비 노출, /admin/ops 필터 UI 렌더링
  - 검증: npx playwright test (18 passed, 2 skipped)


- 단위: fetch log retention 상태 가시화(P2)
  - API 추가: GET /ops/fetch-logs/retention-status
  - 응답: enabled, retentionDays, deleteBatchSize, cron, zone, cutoff, totalLogs, purgeEligibleLogs, oldest/newest fetchedAt
  - 운영 페이지에 Open Retention JSON 링크 추가
  - 테스트 추가: LottoFetchLogQueryServiceTest, OpsApiAccessScenarioTest(retention-status)
  - 검증: 관련 gradle test + UTF-8 check 통과

