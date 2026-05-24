# kLo 개선 작업 완료 이력

> `docs/improvement.md` 기준 구현 완료 항목을 기록합니다.  
> 우선순위 기준: `P0 → P1 → P2 → P3`

---

## 완료 항목 요약

### P0
- CSP 인라인 스크립트 충돌 해소
  - `base.html` 인라인 스크립트를 정적 JS 파일로 분리
  - `script-src 'self'` 정책과 충돌 제거

### P1
- 프론트 JS 모듈 분리
  - `theme.js`, `validation.js`, `bottom-nav.js`, `fragment-loader.js`, `cdn-check.js`, `main.js`
- 추천 카드 lazy-load 적용
  - `/fragments/recommend` 기반 HTMX 로딩
- 수동 수집 보호 강화
  - `OpsController` 수동 collect에 분산 락 적용
- 추천 규칙 실현 가능성 검증
  - `decadeThreshold` 하한 보정 및 시작 시 검증

### P2
- 통계 집계 DB 쿼리 전환
  - `UNION ALL + GROUP BY` 기반 빈도 집계
- API 헤더 설정 외부화
  - `userAgent`, `referer`, `acceptLanguage`
- Trusted Proxy CIDR 적용
  - `IpRange` + `ClientIpResolver` 확장
- 시간 컬럼 정합성 개선
  - `DATETIME(6)` 마이그레이션 적용
- Checkstyle 규칙 강화
- 캐시 TTL/사이즈 설정 분리
  - `KraftCacheProperties` 도입
- 통계 캐시 갱신 비동기화
- JSON 에러 `retryable` 필드 확장
- SEO URL 검증 강화
- `rawJson/raw_response` 길이 제한
- `findMaxRound()` 반복 호출 최소화
- CI main push에 strictCoverage 적용
- `/ops/collect/status` 엔드포인트 추가

---

## 후속 진행 (단계별)

### 1단계 완료: E2E CI 연동
- GitHub Actions에 `e2e-smoke` Job 추가
- 앱 기동(`test` 프로파일) → readiness 확인 → Playwright smoke 실행
- E2E 로그/리포트 아티팩트 업로드

검증:
- `npm run -s test:e2e -- --list`
- `./gradlew.bat test --tests com.kraft.lotto.web.OpsPageControllerTest`

### 2단계 완료: 핵심 템플릿 텍스트 복구
- 템플릿 핵심 영역 깨짐 문자열 정리
- 렌더/테스트 영향 확인

검증:
- `./gradlew.bat test --tests com.kraft.lotto.web.HomeControllerTest --tests com.kraft.lotto.web.HomeControllerWebMvcTest --tests com.kraft.lotto.web.OpsPageControllerTest`

### 3단계 완료: 한글 인코딩 복구 및 템플릿 전체 정리
- 영어 치환분 되돌림
- 한국어 문구를 UTF-8로 복구
- 남은 템플릿(`admin-ops`, `error`, 각 fragments) 정리 완료

검증:
- `./gradlew.bat test --tests com.kraft.lotto.web.HomeControllerTest --tests com.kraft.lotto.web.HomeControllerWebMvcTest --tests com.kraft.lotto.web.OpsPageControllerTest --tests com.kraft.lotto.web.OpsControllerTest`
- 템플릿 깨짐 문자열 잔존 검색(`rg -F`) 확인
