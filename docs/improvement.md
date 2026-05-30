# kraft-lotto 웹 디자인 개선 태스크

> **스택:** Spring Boot + Thymeleaf + CSS + JS (HTMX)  
> **서비스:** https://www.kraft.io.kr  
> **실행 환경:** Claude Code (CLI) — IntelliJ Terminal

---

## STEP 0 — 작업 전 필수 파악 (건너뛰지 말 것)

아래 파일들을 **반드시 먼저 읽은 뒤** 작업을 시작한다.  
필드명·클래스명·fragment 경로를 임의로 가정하지 않는다.

```
읽어야 할 파일 목록:

1. src/main/resources/templates/layout/base.html
   → <link> CSS 로드 목록, nav 구조, th:fragment 이름, HTMX 설정 확인

2. src/main/resources/templates/fragments/ (디렉터리 전체)
   → 기존 fragment 파일명/이름 확인

3. src/main/resources/templates/index.html
   → 추천 세트 루프 변수명, 최신 회차 변수명 확인

4. src/main/resources/templates/frequency.html
   → 빈도 데이터 루프 변수명 확인

5. src/main/resources/templates/rounds.html
   → 회차 루프 변수명, 페이지 변수명 확인

6. src/main/resources/static/css/ (디렉터리 전체)
   → 기존 CSS 파일 목록 및 공통 변수(CSS custom properties) 확인

7. src/main/java/**/controller/ (Controller 파일 전체)
   → 각 페이지에서 Model로 넘기는 attribute명, DTO 클래스명 확인

8. 확인한 DTO/ViewModel 클래스의 실제 필드명 확인
   → numbers, bonus, round, drawDate, firstPrize, winnerCount 등 
      실제 필드명이 다를 수 있으므로 getter/field 기준으로 확인
```

> ⚠️ 위 파일들을 읽기 전에는 어떤 코드도 작성하지 않는다.

---

## STEP 1 — 번호 볼 Fragment 생성

### 1-1. 파일 위치 결정

- `src/main/resources/templates/fragments/` 안에 이미 `ball.html` 또는 유사 파일이 있으면 해당 파일에 추가한다.
- 없으면 `fragments/ball.html` 을 새로 생성한다.

### 1-2. Fragment 코드

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>

<!-- 일반 번호 볼 -->
<span th:fragment="ball(num)"
      th:class="${num <= 10 ? 'ball ball-1-10'
               : num <= 20 ? 'ball ball-11-20'
               : num <= 30 ? 'ball ball-21-30'
               : num <= 40 ? 'ball ball-31-40'
               : 'ball ball-41-45'}"
      th:text="${num}">0</span>

<!-- 보너스 번호 볼 (outline 강조) -->
<span th:fragment="bonus(num)"
      th:class="${num <= 10 ? 'ball ball-bonus ball-1-10'
               : num <= 20 ? 'ball ball-bonus ball-11-20'
               : num <= 30 ? 'ball ball-bonus ball-21-30'
               : num <= 40 ? 'ball ball-bonus ball-31-40'
               : 'ball ball-bonus ball-41-45'}"
      th:text="${num}">0</span>

</body>
</html>
```

> **주의:** `th:classappend` 는 사용하지 않는다. `th:class` 에 전체 클래스를 한 번에 작성한다.  
> Thymeleaf 조건식은 반드시 `${...}` 하나의 표현식 안에서 삼항 연산자로 처리한다.

### 1-3. 볼 CSS 추가

기존 공통 CSS 파일(STEP 0에서 확인한 파일)에 아래를 추가한다.  
별도 파일로 분리할 경우 파일명은 `lotto-ball.css` 로 하고, `base.html` `<head>` 의 기존 CSS 링크 바로 아래에 로드한다.

```css
/* =====================
   로또 번호 볼
   ===================== */
.ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.82rem;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0,0,0,.45);
  flex-shrink: 0;
  user-select: none;
  line-height: 1;
}

/* 범위별 색상 (공식 로또 색상 기준) */
.ball-1-10  { background: radial-gradient(circle at 38% 38%, #ffe066, #f5a623); }
.ball-11-20 { background: radial-gradient(circle at 38% 38%, #7dd8ff, #1a7fc1); }
.ball-21-30 { background: radial-gradient(circle at 38% 38%, #ff9b9b, #e03030); }
.ball-31-40 { background: radial-gradient(circle at 38% 38%, #d0d0d0, #7a7a7a); }
.ball-41-45 { background: radial-gradient(circle at 38% 38%, #d4f07a, #7db300); }

/* 보너스 볼 강조 */
.ball-bonus {
  box-shadow: 0 0 0 2.5px rgba(255,255,255,.55);
}

/* 볼 행 컨테이너 */
.ball-row {
  display: flex;
  align-items: center;
  gap: .4rem;
  flex-wrap: wrap;
}

.bonus-sep {
  opacity: .35;
  font-size: .85rem;
  margin: 0 .1rem;
}
```

### 1-4. 검증

볼 fragment 작성 후, 아래 테스트 스니펫을 `index.html` 에 임시 추가해 렌더링을 확인하고 제거한다.

```html
<!-- 임시 볼 테스트 (확인 후 반드시 제거) -->
<div style="display:flex;gap:.5rem;padding:1rem;">
  <th:block th:each="n : ${#numbers.sequence(1,45)}">
    <th:block th:replace="~{fragments/ball :: ball(${n})}" />
  </th:block>
</div>
```

---

## STEP 2 — index.html 개선

### 2-1. 추천 번호 세트 카드 UI

STEP 0에서 확인한 **실제 루프 변수명**으로 아래 구조를 적용한다.  
(아래 `set`, `set.numbers`, `set.bonus` 는 예시이며, 실제 필드명이 다르면 수정한다.)

```html
<div class="sets-grid" id="sets-result">
  <div th:each="set, stat : ${sets}" class="set-card">
    <span class="set-index" th:text="'#' + ${stat.count}"></span>
    <div class="ball-row">
      <th:block th:each="n : ${set.numbers}">
        <th:block th:replace="~{fragments/ball :: ball(${n})}" />
      </th:block>
      <span class="bonus-sep">+</span>
      <th:block th:if="${set.bonus != null}"
                th:replace="~{fragments/ball :: bonus(${set.bonus})}" />
    </div>
  </div>
</div>
```

```css
/* 추천 번호 세트 */
.sets-grid { display: flex; flex-direction: column; gap: .65rem; margin-top: 1rem; }

.set-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: rgba(255,255,255,.04);
  border: 1px solid rgba(255,255,255,.1);
  border-radius: 12px;
  padding: .75rem 1.1rem;
  transition: background .2s;
}
.set-card:hover { background: rgba(255,255,255,.08); }

.set-index {
  min-width: 2rem;
  font-size: .75rem;
  opacity: .4;
  font-weight: 600;
}
```

### 2-2. 최신 회차 히어로 카드

STEP 0에서 확인한 **실제 Model attribute명 및 DTO 필드명**으로 수정한다.  
필드가 없으면 Controller에서 ViewModel로 가공 후 사용한다 (STEP 4 참고).

```html
<section class="latest-card" th:if="${latest != null}">
  <div class="latest-header">
    <div class="latest-title-row">
      <span class="latest-round" th:text="${latest.round} + '회'"></span>
      <!-- uncollected 필드가 실제로 없으면 이 badge 블록 전체를 제거한다 -->
      <span class="latest-badge" th:if="${latest.uncollected}">미수집</span>
    </div>
    <span class="latest-date"
          th:text="${#temporals.format(latest.drawDate, 'yyyy.MM.dd')}"></span>
  </div>

  <div class="ball-row" style="margin:.75rem 0;">
    <th:block th:each="n : ${latest.numbers}">
      <th:block th:replace="~{fragments/ball :: ball(${n})}" />
    </th:block>
    <span class="bonus-sep">+</span>
    <th:block th:replace="~{fragments/ball :: bonus(${latest.bonus})}" />
  </div>

  <div class="latest-meta">
    <div class="meta-item">
      <span class="meta-label">1등 당첨금</span>
      <!-- firstPrize 필드가 Number 타입이면 아래 사용, String이면 th:text="${latest.firstPrize} + '원'" -->
      <span class="meta-value"
            th:text="${#numbers.formatDecimal(latest.firstPrize, 0, 'COMMA', 0, 'POINT')} + '원'">
      </span>
    </div>
    <div class="meta-item">
      <span class="meta-label">당첨자</span>
      <span class="meta-value" th:text="${latest.winnerCount} + '명'"></span>
    </div>
  </div>
</section>
```

```css
/* 최신 회차 히어로 카드 */
.latest-card {
  background: linear-gradient(135deg, #0d3060 0%, #1a5096 100%);
  border: 1px solid rgba(255,255,255,.13);
  border-radius: 16px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}
.latest-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: .5rem;
}
.latest-title-row { display: flex; align-items: center; gap: .5rem; }
.latest-round  { font-size: 1.1rem; font-weight: 700; }
.latest-date   { font-size: .8rem; opacity: .5; }
.latest-badge  {
  font-size: .65rem; padding: .15rem .45rem;
  background: rgba(245,166,35,.2); color: #f5a623;
  border: 1px solid rgba(245,166,35,.4); border-radius: 20px;
}
.latest-meta {
  display: flex;
  gap: 2.5rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(255,255,255,.1);
}
.meta-item  { display: flex; flex-direction: column; gap: .2rem; }
.meta-label { font-size: .68rem; opacity: .45; text-transform: uppercase; letter-spacing: .04em; }
.meta-value { font-size: .95rem; font-weight: 600; }
```

---

## STEP 3 — frequency.html 개선

### 3-1. Controller 수정 (FrequencyController)

기존 Controller에서 각 `FrequencyItem` (실제 클래스명 확인)에 `percent` 와 `rank` 를 추가 계산해서 넘긴다.  
**기존 DTO 클래스는 수정하지 않는다.** 대신 Controller에서 별도 가공한다.

```java
// Controller 내 빈도 조회 처리 예시
// (실제 변수명/메서드명은 기존 코드에 맞춰 수정할 것)
List<FrequencyItem> frequencies = frequencyService.getAll();

int max = frequencies.stream()
    .mapToInt(FrequencyItem::getCount)  // 실제 getter명 확인
    .max()
    .orElse(1);

// 정렬(번호 오름차순 기준 rank 계산용 복사본)
List<FrequencyItem> sortedByCount = frequencies.stream()
    .sorted(Comparator.comparingInt(FrequencyItem::getCount).reversed())
    .collect(Collectors.toList());

// FrequencyViewModel(또는 Map)으로 가공해서 넘기기
List<Map<String, Object>> viewItems = new ArrayList<>();
for (FrequencyItem item : frequencies) {
    Map<String, Object> vm = new LinkedHashMap<>();
    vm.put("number",  item.getNumber());   // 실제 getter 확인
    vm.put("count",   item.getCount());
    vm.put("percent", (item.getCount() * 100.0) / max);
    vm.put("rank",    sortedByCount.indexOf(item) + 1);
    viewItems.add(vm);
}

model.addAttribute("frequencies", viewItems);
model.addAttribute("currentPage", "frequency");
```

> **대안:** `FrequencyViewModel` 레코드/클래스를 따로 만드는 것이 더 깔끔하다.  
> `record FrequencyViewModel(int number, int count, double percent, int rank) {}`

### 3-2. 템플릿 구조

```html
<section>
  <h2>번호 출현 빈도</h2>

  <!-- 정렬 버튼 (JS 제어) -->
  <div class="freq-controls">
    <button class="btn-sort active" data-sort="number">번호순</button>
    <button class="btn-sort" data-sort="count-desc">많은 순</button>
    <button class="btn-sort" data-sort="count-asc">적은 순</button>
  </div>

  <ul class="freq-list" id="freq-list">
    <li th:each="item : ${frequencies}"
        class="freq-item"
        th:attr="data-number=${item.number}, data-count=${item.count}">
      <th:block th:replace="~{fragments/ball :: ball(${item.number})}" />
      <div class="freq-bar-wrap">
        <div class="freq-bar"
             th:style="'width:' + ${item.percent} + '%'"
             th:classappend="${item.rank <= 5 ? 'freq-bar--top' : (item.rank >= 41 ? 'freq-bar--low' : '')}">
        </div>
      </div>
      <span class="freq-count" th:text="${item.count} + '회'"></span>
    </li>
  </ul>
</section>
```

```css
/* 빈도 막대 */
.freq-controls { display: flex; gap: .4rem; margin-bottom: 1rem; }
.btn-sort {
  padding: .3rem .85rem; border-radius: 20px;
  border: 1px solid rgba(255,255,255,.2);
  background: transparent; color: inherit;
  font-size: .8rem; cursor: pointer; transition: all .15s;
}
.btn-sort.active,
.btn-sort:hover { background: rgba(255,255,255,.15); border-color: rgba(255,255,255,.4); }

.freq-list  { list-style: none; padding: 0; display: flex; flex-direction: column; gap: .45rem; }
.freq-item  { display: flex; align-items: center; gap: .75rem; }

.freq-bar-wrap {
  flex: 1; height: 10px;
  background: rgba(255,255,255,.08);
  border-radius: 4px; overflow: hidden;
}
.freq-bar {
  height: 100%; border-radius: 4px;
  background: #5bc8f5;
  transition: width .5s cubic-bezier(.4,0,.2,1);
}
.freq-bar--top  { background: linear-gradient(90deg, #f5a623, #ffd84d); }
.freq-bar--low  { background: linear-gradient(90deg, #c0392b, #e74c3c); }

.freq-count {
  min-width: 3.8rem; text-align: right;
  font-size: .78rem; opacity: .6;
  font-variant-numeric: tabular-nums;
}
```

```javascript
// 빈도 정렬 (바닐라 JS — 외부 라이브러리 사용 금지)
document.querySelectorAll('.btn-sort').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.btn-sort').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    const list = document.getElementById('freq-list');
    const items = [...list.querySelectorAll('.freq-item')];
    const sort = btn.dataset.sort;

    items.sort((a, b) => {
      if (sort === 'number')     return +a.dataset.number - +b.dataset.number;
      if (sort === 'count-desc') return +b.dataset.count  - +a.dataset.count;
      if (sort === 'count-asc')  return +a.dataset.count  - +b.dataset.count;
      return 0;
    });

    items.forEach(item => list.appendChild(item));
  });
});
```

---

## STEP 4 — rounds.html 개선

### 4-1. Controller 수정 (RoundsController)

```java
model.addAttribute("currentPage", "rounds");
model.addAttribute("pageSizes", List.of(20, 50, 100));  // 템플릿에서 배열 직접 생성 금지
model.addAttribute("currentSize", size);                // 현재 선택된 페이지 크기
```

### 4-2. 페이지 크기 버튼 그룹

```html
<!-- 기존 <select> 또는 드롭다운을 아래로 교체 -->
<div class="page-size-group">
  <th:block th:each="s : ${pageSizes}">
    <a th:href="@{/rounds(size=${s}, page=1)}"
       th:text="${s}"
       th:classappend="${currentSize == s ? ' active' : ''}"
       class="page-size-btn">
    </a>
  </th:block>
</div>
```

### 4-3. 회차 카드 목록

```html
<div class="round-list">
  <div th:each="round : ${rounds}" class="round-item">
    <div class="round-info">
      <span class="round-no" th:text="${round.round} + '회'"></span>
      <!-- drawDate 타입이 LocalDate면 temporals, String이면 th:text="${round.drawDate}" -->
      <span class="round-date"
            th:text="${#temporals.format(round.drawDate, 'yyyy-MM-dd')}"></span>
    </div>
    <div class="ball-row">
      <th:block th:each="n : ${round.numbers}">
        <th:block th:replace="~{fragments/ball :: ball(${n})}" />
      </th:block>
      <span class="bonus-sep">+</span>
      <th:block th:replace="~{fragments/ball :: bonus(${round.bonus})}" />
    </div>
  </div>
</div>
```

```css
/* 페이지 크기 버튼 */
.page-size-group { display: flex; gap: .4rem; margin-bottom: 1rem; }
.page-size-btn {
  padding: .3rem .9rem; border-radius: 20px;
  border: 1px solid rgba(255,255,255,.2);
  font-size: .82rem; cursor: pointer;
  text-decoration: none; color: inherit;
  transition: all .15s;
}
.page-size-btn.active,
.page-size-btn:hover { background: rgba(255,255,255,.15); border-color: rgba(255,255,255,.4); }

/* 회차 목록 */
.round-list { display: flex; flex-direction: column; gap: .5rem; }
.round-item {
  display: flex; align-items: center;
  justify-content: space-between; gap: 1rem;
  padding: .7rem 1rem;
  background: rgba(255,255,255,.03);
  border: 1px solid rgba(255,255,255,.07);
  border-radius: 10px;
}
.round-info { display: flex; flex-direction: column; min-width: 5rem; }
.round-no   { font-size: .88rem; font-weight: 600; }
.round-date { font-size: .72rem; opacity: .4; margin-top: .1rem; }
```

---

## STEP 5 — base.html 모바일 탭바

### 5-1. Controller 공통 처리

모든 Controller (Home, Frequency, Rounds)에 아래를 추가한다.

```java
// 각 Controller @GetMapping 메서드 내
model.addAttribute("currentPage", "home");       // "home" | "frequency" | "rounds"
```

### 5-2. 탭바 HTML

`base.html` (또는 공통 레이아웃 파일) `</body>` 직전에 삽입한다.

```html
<!-- 모바일 하단 탭바 -->
<nav class="tab-bar" aria-label="하단 탭 내비게이션">
  <a th:href="@{/}"
     th:classappend="${currentPage == 'home' ? ' active' : ''}"
     class="tab-item" aria-label="번호 추천">
    <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/>
      <line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>
    </svg>
    <span class="tab-label">추천</span>
  </a>
  <a th:href="@{/frequency}"
     th:classappend="${currentPage == 'frequency' ? ' active' : ''}"
     class="tab-item" aria-label="번호 빈도">
    <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
      <line x1="6" y1="20" x2="6" y2="14"/>
    </svg>
    <span class="tab-label">빈도</span>
  </a>
  <a th:href="@{/rounds}"
     th:classappend="${currentPage == 'rounds' ? ' active' : ''}"
     class="tab-item" aria-label="회차 목록">
    <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
      <polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/>
      <line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
    </svg>
    <span class="tab-label">회차</span>
  </a>
</nav>
```

### 5-3. 탭바 CSS

```css
/* 모바일 하단 탭바 */
.tab-bar {
  display: none;
  position: fixed;
  bottom: 0; left: 0; right: 0;
  height: 60px;
  background: rgba(6, 29, 58, 0.96);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255,255,255,.1);
  z-index: 999;
  padding-bottom: env(safe-area-inset-bottom); /* iPhone 노치 대응 */
}
.tab-item {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  flex: 1; gap: .2rem;
  text-decoration: none;
  color: rgba(255,255,255,.4);
  transition: color .15s;
  height: 100%;
}
.tab-item.active,
.tab-item:hover { color: #fff; }
.tab-icon  { width: 1.35rem; height: 1.35rem; }
.tab-label { font-size: .62rem; }

@media (max-width: 640px) {
  .tab-bar { display: flex; }
  body { padding-bottom: calc(60px + env(safe-area-inset-bottom)); }
  /* 상단 nav가 모바일에서 불필요하면 숨김 (기존 nav selector 확인 후 수정) */
  /* .top-nav { display: none; } */
}
```

> `base.html` 에서 기존 nav의 CSS 클래스명을 확인한 뒤 모바일 숨김 여부를 결정한다.

---

## STEP 6 — 다크/라이트 모드 대응

기존 CSS에 다크/라이트 모드 토글이 구현되어 있다면 볼 CSS 가독성을 확인한다.

```css
/* 라이트 모드에서 볼 텍스트 그림자 강화 (필요한 경우) */
@media (prefers-color-scheme: light) {
  .ball { text-shadow: 0 1px 3px rgba(0,0,0,.6); }
}

/* JS 토글 방식이면 (예: data-theme="light" on <html>) */
[data-theme="light"] .ball { text-shadow: 0 1px 3px rgba(0,0,0,.6); }
```

---

## 금지 사항

| 금지 | 이유 |
|------|------|
| CI/CD 파일 수정 | 배포 파이프라인 영향 |
| `README.md` 수정 | 문서 정책 |
| 외부 JS/CSS 라이브러리 추가 | 번들 크기 / 보안 |
| 기존 API DTO 필드 변경 | 하위 호환성 |
| HTMX `hx-swap`, `hx-target` 속성 제거 | 기존 동작 파괴 |
| 템플릿에서 `${#arrays.toArray(...)}` 배열 직접 생성 | Controller에서 넘길 것 |
| `th:classappend` + `th:class` 동시 사용 | Thymeleaf 충돌 |

---

## 작업 완료 체크리스트

```
[ ] STEP 0 — 모든 대상 파일 읽기 완료 (작업 전 필수)
[ ] STEP 1 — fragments/ball.html 생성, ball() / bonus() fragment 정상 동작
[ ] STEP 1 — 볼 CSS (.ball, .ball-1-10 ~ .ball-41-45, .ball-bonus) 추가
[ ] STEP 2 — index.html 추천 세트 set-card 레이아웃 적용
[ ] STEP 2 — index.html 최신 회차 latest-card 적용
[ ] STEP 3 — FrequencyController에 percent/rank 계산 추가
[ ] STEP 3 — frequency.html 막대 시각화 + 정렬 버튼 적용
[ ] STEP 4 — RoundsController에 pageSizes, currentSize 추가
[ ] STEP 4 — rounds.html 카드형 목록 + 페이지 크기 버튼 그룹 적용
[ ] STEP 5 — 전체 Controller에 currentPage attribute 추가
[ ] STEP 5 — base.html 모바일 탭바 추가 + CSS 적용
[ ] ./gradlew test — 테스트 통과
[ ] ./gradlew bootRun — 실행 확인
[ ] /, /frequency, /rounds 각 페이지 렌더링 오류 없음
[ ] 모바일 320px~640px 반응형 확인
[ ] 다크/라이트 모드 전환 시 볼 가독성 확인
[ ] HTMX 요청 (번호 생성 등) 기존 동작 유지 확인
```
