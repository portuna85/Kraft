# kLo 개선 작업 목록 v2

## 평가 기준

| 우선순위 | 기준 |
|----------|------|
| 높음 | 사용자 체감 직접, 구현 비용 낮음 |
| 중간 | 기능 가치 있음, 구현 비용 보통 |
| 낮음 | 가치는 있으나 비용 대비 효과 낮음 |

---

## 1순위 — 즉시 반영 권장

### 1-1. 추천 번호 클립보드 복사 버튼

현재 set-card에 복사 기능이 없어 사용자가 직접 번호를 입력해야 한다.
각 set-card 우측에 "복사" 버튼 추가, 클릭 시 `1, 8, 14, 23, 36, 42` 형식으로 클립보드에 복사.

- 수정 파일: `fragments/recommend-card.html`, `main.js`
- 복잡도: 낮음

### 1-2. 다음 추첨 D-day 표시

홈 또는 최신 회차 페이지에 다음 추첨일(토요일)까지 남은 일수 표시.

- 수정 파일: `HomeController` 또는 `LatestRoundController`, `home.html` / `latest.html`
- 로직: `LottoDrawSchedule`에 이미 회차 계산 로직 있음, 다음 추첨일 계산만 추가
- 복잡도: 낮음

### 1-3. 최신 회차 — 이월 적립금 표시

1등 당첨자가 없을 때 이월 금액 표시. `WinningNumberEntity.firstAccumAmount` 필드가 이미 존재하나 UI에 미노출.

- 수정 파일: `WinningNumberDto`, `LatestRoundController`, `latest.html`
- 복잡도: 낮음

### 1-4. 뉴스 페이지 — 등급 필터 탭

`/news` 페이지에 공식·언론·일반 탭 필터 추가. 이미 등급 분류 로직이 있으므로 쿼리 파라미터 + DB 필터만 추가.

- 수정 파일: `NewsQueryService`, `NewsArticleRepository`, `NewsController`, `news.html`
- 복잡도: 낮음~중간

---

## 2순위 — 단기 개선

### 2-1. 번호별 연속 미출현 회수 표시

빈도 페이지에서 각 번호가 마지막으로 출현한 회차와 현재까지 연속 미출현 회수를 함께 표시.
"냉각 번호" 개념이지만 오해 방지 주의 문구와 함께 제공.

- 수정 파일: `WinningNumberRepository`(쿼리), `WinningStatisticsCacheService`, `FrequencyViewModel`, `frequency-card.html`
- 복잡도: 중간

### 2-2. 이전/다음 회차 빠른 이동

최신 회차 및 회차 상세 페이지에 이전·다음 회차 이동 버튼 추가.

- 수정 파일: `LatestRoundController`, `latest.html`, `rounds.html`
- 복잡도: 낮음

### 2-3. 추천 이력 — 세션 내 생성 번호 목록

같은 세션에서 생성한 번호 조합을 `localStorage`에 저장해 하단에 목록으로 표시.
서버 변경 없이 순수 JS로 구현 가능.

- 수정 파일: `recommend-card.html`, JS 신규 파일
- 복잡도: 낮음

### 2-4. 회차 상세 페이지 OG/구조화 데이터 강화

각 회차 URL(`/rounds?round=1234`)에 `schema.org/Dataset` 또는 Event 마크업 추가.
검색 엔진 노출 향상.

- 수정 파일: `SeoController` 또는 `rounds.html` head 영역
- 복잡도: 낮음

---

## 3순위 — 중장기 개선

### 3-1. 서비스 상태 공개 페이지

`/status` 페이지에서 최신 수집 성공 여부, DB 연결 상태, 캐시 상태를 사용자 친화적으로 표시.
현재 `/actuator/health`는 운영용으로만 노출됨.

- 수정 파일: `InfoPageController`, 신규 `status.html`
- 복잡도: 중간

### 3-2. 당첨번호 RSS/JSON 피드 제공

`/feed.xml` 또는 `/api/rounds/latest.json`으로 최신 N회차 당첨번호를 구조화 형식으로 제공.
타 서비스 연동 및 검색 엔진 수집 편의 향상.

- 수정 파일: `SeoController` 확장 또는 신규 `FeedController`
- 복잡도: 중간

### 3-3. 캐시 워밍 전략

서버 기동 후 주요 캐시(빈도 요약, 패턴 통계, 동반 출현)를 ApplicationReadyEvent에서 선로드.
현재는 첫 요청 시 캐시 미스로 응답 지연 발생.

- 수정 파일: 신규 `CacheWarmupRunner`, `WinningStatisticsCacheService`
- 복잡도: 낮음

### 3-4. 번호 조합 즐겨찾기

사용자가 번호 조합을 `localStorage`에 저장하고 분석 페이지에서 불러오는 기능.
서버 변경 없이 순수 프론트엔드로 구현 가능.

- 수정 파일: `analysis.html`, JS 신규 파일
- 복잡도: 중간

### 3-5. 수집 실패 알림

연속 N회 수집 실패 시 설정된 이메일 또는 웹훅으로 알림 전송.

- 수정 파일: `WinningNumberAutoCollectScheduler`, 신규 `AlertService`, `application.yml`
- 복잡도: 중간~높음

---

## 개선 체크리스트

| 구분 | 항목 | 우선순위 | 상태 |
|------|------|:--------:|------|
| UX | 추천 번호 클립보드 복사 | 높음 | 미반영 |
| UX | 다음 추첨 D-day 표시 | 높음 | 미반영 |
| 데이터 | 이월 적립금 표시 | 높음 | 미반영 |
| 뉴스 | 등급 필터 탭 | 높음 | 미반영 |
| 통계 | 연속 미출현 회수 표시 | 중간 | 미반영 |
| UX | 이전/다음 회차 이동 | 중간 | 미반영 |
| UX | 세션 내 추천 이력 | 중간 | 미반영 |
| SEO | 회차 구조화 데이터 | 중간 | 미반영 |
| 운영 | 서비스 상태 페이지 | 낮음 | 미반영 |
| API | 당첨번호 피드 제공 | 낮음 | 미반영 |
| 성능 | 캐시 워밍 전략 | 낮음 | 미반영 |
| UX | 번호 조합 즐겨찾기 | 낮음 | 미반영 |
| 운영 | 수집 실패 알림 | 낮음 | 미반영 |
