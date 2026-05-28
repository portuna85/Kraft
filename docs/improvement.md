# Code Quality Improvement Report / 코드 품질 개선 보고서

## Scope / 범위

This document focuses only on **backend and frontend code quality**. It intentionally excludes product completeness, CI/CD maturity, deployment reliability, runtime operations, business viability, and repository presentation unless they directly affect code maintainability.

이 문서는 **백엔드와 프론트엔드 코드 품질**만 다룹니다. 제품 완성도, CI/CD 성숙도, 배포 신뢰성, 운영 안정성, 비즈니스 타당성, 저장소 소개 품질은 코드 유지보수성에 직접 영향을 주는 경우를 제외하고 평가 대상에서 제외합니다.

Reviewed areas:

- Backend Java code under `src/main/java`
- Public controllers, ops controllers, application services, domain objects, API clients, filters, and configuration classes
- Thymeleaf templates under `src/main/resources/templates`
- Static JavaScript and CSS under `src/main/resources/static`
- Code compression and consolidation opportunities

검토 영역:

- `src/main/java` 하위 백엔드 Java 코드
- public controller, ops controller, application service, domain object, API client, filter, configuration class
- `src/main/resources/templates` 하위 Thymeleaf 템플릿
- `src/main/resources/static` 하위 정적 JavaScript/CSS
- 코드 압축 및 통폐합 가능성

---

# English Report

## Executive Assessment

| Area | Score | Verdict |
|---|---:|---|
| Backend code quality | 76 / 100 | Structurally sound, but not yet lean. Good domain modeling and service separation, with some orchestration and metrics leakage. |
| Frontend code quality | 68 / 100 | Functionally careful and accessibility-aware, but fragmented and verbose. Consolidation would improve readability. |
| Expected score after targeted consolidation | ~80 / 100 | Achievable without changing business behavior. |

Overall, the codebase is above average for a personal Spring Boot project. It is not a shallow CRUD example. The backend has meaningful domain validation, explicit service boundaries, retry/circuit-breaker handling, and operational endpoints. The frontend shows care around HTMX fallback, CSP compatibility, accessibility, responsive layout, and progressive enhancement.

The main issue is not absence of structure. The issue is that some parts are now **over-structured**, while others are **overloaded**. A targeted cleanup should focus on reducing ceremony, extracting orchestration from controllers, and consolidating small view fragments.

---

## Backend Code Quality

### Strengths

#### 1. Domain model has clear invariants

`LottoCombination` is a good domain object. It validates nulls, exact size, valid range, duplication, and normalizes number order at construction time.

Positive effects:

- Service code can trust `LottoCombination` instances.
- Invalid state is rejected early.
- Sorting and uniqueness are centralized.

This should be preserved.

#### 2. Public web controller is reasonably thin

`HomeController` mainly handles request parameters, invokes services, and populates the model. It does not contain meaningful business logic.

This is acceptable controller design.

#### 3. Recommendation logic is conceptually well separated

The recommendation path has recognizable layers:

- `RecommendService`: request-level normalization, DTO conversion, exception mapping, metrics
- `LottoRecommender`: generation loop, duplicate filtering, rule evaluation, timeout behavior
- `ExclusionRule` implementations: individual filtering rules
- `RecommendRuleConfig`: explicit rule ordering

The design is understandable and extensible.

#### 4. External API handling is defensive

The API client code distinguishes HTTP errors, blank bodies, HTML responses, non-JSON responses, network errors, parser failures, retries, request timeout, and circuit-breaker state.

This is better than a naive `RestClient.get().retrieve()` implementation.

---

### Weaknesses

#### 1. `OpsController` is overloaded

`OpsController` currently owns too many responsibilities:

- fetch log failure reason summary
- recent failure logs
- combined failure overview
- retention status
- collection status
- manual collection trigger
- missing-round collection trigger
- recommendation metrics
- circuit-breaker snapshots
- ShedLock execution wrapper

This makes the controller a mixed routing, orchestration, and operational coordination layer.

Recommended options:

Option A: split by endpoint family:

```text
OpsFetchLogController
OpsCollectionController
OpsMetricsController
OpsCircuitBreakerController
```

Option B: keep one external controller but extract orchestration:

```text
OpsController
  -> OpsCollectionFacade
  -> OpsFetchLogFacade
  -> OpsMetricsFacade
```

Preferred approach: **Option B first**, then split controllers only if the file remains large.

#### 2. `catch (Throwable)` should be removed

The lock wrapper catches `Throwable` and wraps it in `RuntimeException`. This is too broad because it can catch JVM-level errors such as `OutOfMemoryError` or `StackOverflowError`.

Recommended replacement:

```java
catch (Exception e) {
    throw new CollectionLockException("collection lock failed", e);
}
```

Or catch ShedLock-specific exceptions if available.

#### 3. Metrics code leaks into core logic

`RecommendService`, `LottoRecommender`, and API clients directly record metrics. This is functional, but it adds noise to business logic.

Recommended extraction:

```text
RecommendMetricsRecorder
ExternalApiMetricsRecorder
```

Target shape:

```java
metrics.recordLatency(startedAtNanos);
metrics.recordRuleRejected(ruleName);
metrics.recordTimeout(reason);
```

This keeps behavior unchanged while making the main flow easier to read.

#### 4. API client remains too large

`DhLotteryApiClient` handles URI construction, raw fetch, status validation, body validation, response classification, parsing, retry handling, circuit-breaker integration, metric recording, and failure normalization.

Some of this complexity is justified. However, the class would be easier to maintain if provider-specific logic were separated from generic operational behavior.

Recommended consolidation:

```text
ExternalApiCallExecutor
ApiResponseValidator
ApiFailureClassifier
ExternalApiMetricsRecorder
```

Do not split too aggressively. The goal is readability, not class count inflation.

#### 5. Optional dependency handling is repetitive

Several classes use `ObjectProvider`, nullable `MeterRegistry`, or nullable `CacheManager` patterns. This is acceptable, but repeated null checks reduce clarity.

Recommended direction:

- Use small no-op collaborators for metrics where useful.
- Keep `ObjectProvider` only at configuration boundaries.
- Avoid passing optional infrastructure into domain-like logic when a no-op implementation would be simpler.

---

## Backend Compression and Consolidation Plan

| Priority | Target | Action | Expected result |
|---:|---|---|---|
| 1 | `OpsController` | Extract lock/orchestration into `OpsCollectionFacade` | Smaller controller, clearer responsibility |
| 2 | `catch (Throwable)` | Replace with narrower exception handling | Safer failure behavior |
| 3 | Recommendation metrics | Extract `RecommendMetricsRecorder` | Cleaner recommendation flow |
| 4 | API metrics/failure handling | Extract shared API operational helpers | Less repeated operational code |
| 5 | Optional infra dependencies | Introduce no-op collaborators where useful | Fewer null checks |

Keep the following as-is:

- `LottoCombination`
- `ExclusionRule` strategy structure
- feature-sliced package layout
- explicit rule ordering in `RecommendRuleConfig`

---

## Frontend Code Quality

### Strengths

#### 1. Layout and fragment composition are directionally good

`base.html` centralizes meta tags, canonical URL, robots behavior, manifest, theme setup, navigation, bottom navigation, and global script loading.

`home.html` composes the main page from smaller sections, using HTMX to lazy-load dynamic cards.

This is a reasonable server-rendered frontend architecture.

#### 2. Accessibility is intentionally considered

The frontend uses:

- `aria-busy`
- `aria-live`
- `aria-expanded`
- `aria-controls`
- visually hidden live regions
- focus movement after dynamic content loads
- accessible table sorting announcement

This is better than most lightweight Thymeleaf/HTMX interfaces.

#### 3. CSP-compatible JavaScript/CSS strategy

The application avoids inline behavior and uses static JavaScript files. HTMX inline indicator styles are disabled and replaced with static `.htmx-indicator` CSS.

This is a good baseline.

#### 4. JavaScript is defensive

`fragment-loader.js` handles:

- duplicate in-flight request suppression
- HTMX event integration
- fallback `fetch()` loading when HTMX is unavailable
- retry backoff
- error states
- live region announcements
- focus handling after reload

This is functionally careful code.

---

### Weaknesses

#### 1. Fragment files contain unnecessary boilerplate

Small Thymeleaf fragments such as `recommend-card.html`, `frequency-card.html`, `latest-card.html`, and `round-search-card.html` include full document boilerplate:

```html
<!doctype html>
<html>
<body>
...
</body>
</html>
```

For fragment-only files, this is unnecessary. It increases visual noise.

Recommended shape:

```html
<div th:fragment="recommend-card">
  ...
</div>
```

#### 2. Some home fragments are too small to justify separate files

`latest-card.html` and `round-search-card.html` are small, home-only fragments. They can be consolidated.

Recommended consolidation:

```text
templates/fragments/home-static-cards.html
  - latest-card
  - round-search-card
```

Keep separate files for dynamic HTMX sections:

```text
recommend-card.html
frequency-card.html
rounds-list.html
lotto-ball.html
```

#### 3. `admin-ops.html` is too large

The admin ops page includes filter form, JSON links, retention status, failure reason mobile cards, failure reason table, failure log mobile cards, failure log table, and pagination in one template.

This should be split.

Recommended fragments:

```text
templates/admin/ops-filter.html
templates/admin/retention-card.html
templates/admin/failure-reasons.html
templates/admin/failure-logs.html
templates/admin/pagination.html
```

A less invasive option is to keep one file but define internal Thymeleaf fragments inside it.

#### 4. `fragment-loader.js` is doing too much

The file is valuable and should not be deleted. However, it currently mixes:

- state management
- retry/backoff policy
- fallback fetch implementation
- HTMX event handling
- focus management
- accessibility announcements

Recommended internal organization:

```text
State helpers
Retry helpers
Accessibility helpers
Fallback loader
HTMX event bindings
Retry button binding
```

Do not split into many files unless a bundling step is introduced.

#### 5. `app.css` combines public UI and ops UI

Keeping one CSS file is acceptable because the project does not appear to have a frontend build pipeline. But public-page styles and ops-dashboard styles should be clearly grouped.

Recommended ordering:

```text
1. tokens
2. base/layout
3. navigation
4. shared components
5. public page sections
6. ops dashboard sections
7. utilities and accessibility helpers
8. responsive overrides
```

---

## Frontend Compression and Consolidation Plan

| Priority | Target | Action | Expected result |
|---:|---|---|---|
| 1 | Fragment boilerplate | Remove `doctype/html/body` from fragment-only files | Less noise |
| 2 | Home static cards | Merge `latest-card` and `round-search-card` into one fragment file | Fewer tiny files |
| 3 | `admin-ops.html` | Split into 3-5 fragments | Much better maintainability |
| 4 | `fragment-loader.js` | Reorganize internally by concern | Easier review and future changes |
| 5 | `app.css` | Group public and ops styles more explicitly | Lower navigation cost |

Keep the following:

- `lotto-ball.html`
- `recommend-card.html`
- `frequency-card.html`
- `rounds-list.html`
- CSP-compatible static JS/CSS approach
- accessibility live-region and focus behavior

---

## Recommended Implementation Order

1. Remove unnecessary `doctype/html/body` wrappers from fragment-only templates.
2. Merge `latest-card.html` and `round-search-card.html` into `home-static-cards.html`.
3. Extract `OpsCollectionFacade` and move ShedLock execution out of `OpsController`.
4. Replace `catch (Throwable)` with narrower exception handling.
5. Extract `RecommendMetricsRecorder`.
6. Reorganize `fragment-loader.js` internally without changing behavior.
7. Split `admin-ops.html` into fragments.
8. Extract shared API operational helpers only after the controller and template cleanup are done.

---

## Completion Criteria

This improvement cycle can be considered complete when:

- Public behavior remains unchanged.
- Existing backend tests still pass.
- Existing E2E smoke tests still pass.
- `OpsController` is visibly smaller and no longer owns lock orchestration details.
- No application code catches `Throwable` for normal control flow.
- Fragment-only templates no longer contain full document boilerplate.
- `admin-ops.html` is split or internally organized into clear fragments.
- Recommendation and API metrics code is less intrusive in core business flow.

---

# 한국어 보고서

## 종합 평가

| 영역 | 점수 | 판정 |
|---|---:|---|
| 백엔드 코드 품질 | 76 / 100 | 구조는 양호하지만 아직 날렵하지 않음. 도메인 모델과 서비스 분리는 좋으나 일부 orchestration과 metrics 코드가 섞여 있음 |
| 프론트엔드 코드 품질 | 68 / 100 | 기능·접근성·CSP 대응은 좋으나 템플릿과 정적 파일이 다소 산재함 |
| 통폐합 후 기대 점수 | 약 80 / 100 | 기능 변경 없이 리팩토링만으로 달성 가능 |

전체적으로 이 코드는 개인 Spring Boot 프로젝트 기준으로 평균 이상입니다. 단순 CRUD 예제는 아닙니다. 백엔드는 도메인 검증, 서비스 분리, 외부 API 장애 처리, 운영 엔드포인트를 갖추고 있습니다. 프론트엔드는 HTMX fallback, CSP 호환성, 접근성, 반응형 레이아웃, progressive enhancement를 의식하고 있습니다.

핵심 문제는 구조가 없다는 것이 아닙니다. 오히려 일부는 **너무 잘게 나뉘어 있고**, 일부는 **한 파일에 너무 많은 책임이 몰려 있습니다**. 개선 방향은 ceremony를 줄이고, controller에서 orchestration을 빼고, 작은 view fragment를 통폐합하는 것입니다.

---

## 백엔드 코드 품질

### 장점

#### 1. 도메인 객체의 불변 조건이 명확함

`LottoCombination`은 좋은 도메인 객체입니다. 생성 시점에 null, 개수, 범위, 중복을 검증하고 정렬까지 수행합니다.

효과:

- service 계층에서 `LottoCombination`을 신뢰할 수 있음
- 잘못된 상태를 초기에 차단함
- 정렬과 중복 검증이 한 곳에 모임

이 구조는 유지하는 것이 좋습니다.

#### 2. public web controller는 비교적 얇음

`HomeController`는 request parameter를 받고, service를 호출하고, model에 값을 넣는 역할에 집중합니다. 의미 있는 비즈니스 로직은 거의 없습니다.

controller 설계로는 양호합니다.

#### 3. 추천 로직의 책임 분리가 비교적 명확함

추천 경로는 다음 계층으로 나뉩니다.

- `RecommendService`: 요청 단위 정규화, DTO 변환, 예외 매핑, metrics
- `LottoRecommender`: 생성 loop, 중복 제거, rule 평가, timeout 처리
- `ExclusionRule` 구현체: 개별 제외 규칙
- `RecommendRuleConfig`: 명시적 rule 순서 정의

구조 의도는 분명하고 확장성도 있습니다.

#### 4. 외부 API 장애 대응 코드가 방어적임

API client는 HTTP 오류, blank body, HTML 응답, non-JSON 응답, network 오류, parser 실패, retry, request timeout, circuit breaker 상태를 구분합니다.

단순한 `RestClient` 호출보다 훨씬 신중한 구현입니다.

---

### 약점

#### 1. `OpsController`가 과밀함

`OpsController`는 현재 너무 많은 책임을 갖고 있습니다.

- fetch log failure reason summary
- recent failure logs
- combined failure overview
- retention status
- collection status
- manual collection trigger
- missing-round collection trigger
- recommendation metrics
- circuit-breaker snapshots
- ShedLock 실행 wrapper

이로 인해 controller가 단순 routing 계층을 넘어 운영 orchestration 계층까지 담당합니다.

권장 선택지:

선택지 A: endpoint 계열별 분리

```text
OpsFetchLogController
OpsCollectionController
OpsMetricsController
OpsCircuitBreakerController
```

선택지 B: 외부 controller는 유지하고 orchestration만 추출

```text
OpsController
  -> OpsCollectionFacade
  -> OpsFetchLogFacade
  -> OpsMetricsFacade
```

우선 권장안은 **선택지 B**입니다. 그 후에도 파일이 크면 controller를 나누면 됩니다.

#### 2. `catch (Throwable)`은 제거해야 함

lock wrapper에서 `Throwable`을 잡고 `RuntimeException`으로 감쌉니다. 이는 너무 넓습니다. `OutOfMemoryError`, `StackOverflowError` 같은 JVM 수준 오류까지 잡을 수 있습니다.

권장 변경:

```java
catch (Exception e) {
    throw new CollectionLockException("collection lock failed", e);
}
```

가능하면 ShedLock 관련 예외만 구체적으로 잡는 것이 더 좋습니다.

#### 3. metrics 코드가 핵심 로직에 섞여 있음

`RecommendService`, `LottoRecommender`, API client가 metrics를 직접 기록합니다. 동작상 문제는 없지만 비즈니스 흐름을 읽기 어렵게 만듭니다.

권장 추출:

```text
RecommendMetricsRecorder
ExternalApiMetricsRecorder
```

목표 형태:

```java
metrics.recordLatency(startedAtNanos);
metrics.recordRuleRejected(ruleName);
metrics.recordTimeout(reason);
```

기능은 그대로 두면서 main flow를 더 짧고 명확하게 만들 수 있습니다.

#### 4. API client가 여전히 큼

`DhLotteryApiClient`는 URI 구성, raw fetch, status 검증, body 검증, response classification, parsing, retry handling, circuit breaker, metric, failure normalization을 모두 처리합니다.

일부 복잡성은 정당하지만, provider-specific logic과 generic operational behavior를 더 분리하면 유지보수성이 좋아집니다.

권장 통폐합:

```text
ExternalApiCallExecutor
ApiResponseValidator
ApiFailureClassifier
ExternalApiMetricsRecorder
```

단, 과도하게 쪼개지는 것은 피해야 합니다. 목표는 class 수 증가가 아니라 가독성 개선입니다.

#### 5. optional infrastructure 의존성 처리가 반복됨

여러 클래스에서 `ObjectProvider`, nullable `MeterRegistry`, nullable `CacheManager` 패턴이 반복됩니다. 허용 가능한 방식이지만 null check가 반복되어 코드가 흐려집니다.

권장 방향:

- metrics에는 no-op collaborator를 도입
- `ObjectProvider`는 configuration boundary에만 유지
- domain-like logic에는 optional infra를 직접 넘기지 않기

---

## 백엔드 압축·통폐합 계획

| 우선순위 | 대상 | 작업 | 기대 효과 |
|---:|---|---|---|
| 1 | `OpsController` | lock/orchestration을 `OpsCollectionFacade`로 추출 | controller 축소, 책임 명확화 |
| 2 | `catch (Throwable)` | 더 좁은 예외 처리로 변경 | 안전한 실패 처리 |
| 3 | 추천 metrics | `RecommendMetricsRecorder` 추출 | 추천 flow 가독성 향상 |
| 4 | API metrics/failure handling | 공통 API 운영 helper 추출 | 반복 운영 코드 감소 |
| 5 | optional infra dependency | 필요한 곳에 no-op collaborator 도입 | null check 감소 |

유지할 것:

- `LottoCombination`
- `ExclusionRule` 전략 구조
- feature-sliced package layout
- `RecommendRuleConfig`의 명시적 rule ordering

---

## 프론트엔드 코드 품질

### 장점

#### 1. layout과 fragment 조립 방향은 좋음

`base.html`은 meta tag, canonical URL, robots 처리, manifest, theme setup, navigation, bottom navigation, global script loading을 중앙에서 관리합니다.

`home.html`은 주요 화면을 작은 section들로 조립하고, 동적 카드는 HTMX로 lazy-load합니다.

server-rendered frontend 구조로는 합리적입니다.

#### 2. 접근성을 의식하고 있음

프론트엔드는 다음 요소를 사용합니다.

- `aria-busy`
- `aria-live`
- `aria-expanded`
- `aria-controls`
- visually hidden live region
- 동적 content load 후 focus 이동
- table sorting announce

가벼운 Thymeleaf/HTMX UI 기준으로는 좋은 편입니다.

#### 3. CSP 호환 JS/CSS 전략이 좋음

inline behavior를 피하고 정적 JavaScript 파일을 사용합니다. HTMX inline indicator style도 비활성화하고 static `.htmx-indicator` CSS로 대체합니다.

좋은 기본 방향입니다.

#### 4. JavaScript가 방어적으로 작성됨

`fragment-loader.js`는 다음을 처리합니다.

- 중복 요청 방지
- HTMX event integration
- HTMX 부재 시 `fetch()` fallback
- retry backoff
- error state
- live region announcement
- reload 후 focus 처리

단순히 “작동만 하는 JS”가 아니라, 사용성까지 신경 쓴 코드입니다.

---

### 약점

#### 1. fragment 파일의 boilerplate가 불필요하게 큼

`recommend-card.html`, `frequency-card.html`, `latest-card.html`, `round-search-card.html` 같은 작은 fragment 파일들이 전체 문서 구조를 포함합니다.

```html
<!doctype html>
<html>
<body>
...
</body>
</html>
```

fragment 전용 파일이라면 불필요합니다. 시각적 noise만 늘립니다.

권장 형태:

```html
<div th:fragment="recommend-card">
  ...
</div>
```

#### 2. 일부 home fragment는 별도 파일로 두기엔 작음

`latest-card.html`과 `round-search-card.html`은 작고 home 전용입니다. 통합해도 됩니다.

권장 통합:

```text
templates/fragments/home-static-cards.html
  - latest-card
  - round-search-card
```

동적 HTMX 영역은 별도 유지합니다.

```text
recommend-card.html
frequency-card.html
rounds-list.html
lotto-ball.html
```

#### 3. `admin-ops.html`이 너무 큼

admin ops page는 filter form, JSON link, retention status, failure reason mobile card, failure reason table, failure log mobile card, failure log table, pagination을 모두 한 파일에 포함합니다.

분리 대상입니다.

권장 fragment:

```text
templates/admin/ops-filter.html
templates/admin/retention-card.html
templates/admin/failure-reasons.html
templates/admin/failure-logs.html
templates/admin/pagination.html
```

파일 수를 늘리기 싫다면 한 파일 안에서 Thymeleaf fragment만이라도 내부 분리하는 방식을 사용할 수 있습니다.

#### 4. `fragment-loader.js`가 너무 많은 일을 함

이 파일은 가치가 있으므로 삭제 대상은 아닙니다. 다만 현재 다음 책임이 섞여 있습니다.

- 상태 관리
- retry/backoff 정책
- fallback fetch 구현
- HTMX event handling
- focus management
- accessibility announcement

권장 내부 구조:

```text
State helpers
Retry helpers
Accessibility helpers
Fallback loader
HTMX event bindings
Retry button binding
```

frontend build pipeline이 없다면 여러 파일로 나누기보다 내부 정리만 우선하는 것이 낫습니다.

#### 5. `app.css`에 public UI와 ops UI가 섞여 있음

현재 구조에서 CSS 파일 하나를 유지하는 것은 괜찮습니다. 그러나 public page style과 ops dashboard style은 더 명확히 구분해야 합니다.

권장 순서:

```text
1. tokens
2. base/layout
3. navigation
4. shared components
5. public page sections
6. ops dashboard sections
7. utilities and accessibility helpers
8. responsive overrides
```

---

## 프론트엔드 압축·통폐합 계획

| 우선순위 | 대상 | 작업 | 기대 효과 |
|---:|---|---|---|
| 1 | fragment boilerplate | fragment-only 파일에서 `doctype/html/body` 제거 | noise 감소 |
| 2 | home static cards | `latest-card`와 `round-search-card` 통합 | 작은 파일 수 감소 |
| 3 | `admin-ops.html` | 3~5개 fragment로 분리 | 유지보수성 대폭 향상 |
| 4 | `fragment-loader.js` | concern 기준으로 내부 재정리 | review와 수정이 쉬워짐 |
| 5 | `app.css` | public/ops style 구역 명확화 | 탐색 비용 감소 |

유지할 것:

- `lotto-ball.html`
- `recommend-card.html`
- `frequency-card.html`
- `rounds-list.html`
- CSP 호환 static JS/CSS 방식
- live-region 및 focus 기반 접근성 처리

---

## 권장 구현 순서

1. fragment-only template에서 불필요한 `doctype/html/body` wrapper 제거
2. `latest-card.html`과 `round-search-card.html`을 `home-static-cards.html`로 통합
3. `OpsCollectionFacade`를 만들고 ShedLock 실행 로직을 `OpsController` 밖으로 이동
4. `catch (Throwable)`을 더 좁은 예외 처리로 변경
5. `RecommendMetricsRecorder` 추출
6. `fragment-loader.js`를 동작 변경 없이 내부 구조만 정리
7. `admin-ops.html`을 fragment로 분리
8. controller/template 정리 이후 API 공통 운영 helper 추출

---

## 완료 기준

이 개선 cycle은 다음 조건을 만족하면 완료로 볼 수 있습니다.

- public behavior가 변경되지 않음
- 기존 백엔드 테스트 통과
- 기존 E2E smoke 테스트 통과
- `OpsController`가 눈에 띄게 작아지고 lock orchestration 세부사항을 직접 갖지 않음
- 일반 control flow에서 `Throwable`을 잡지 않음
- fragment-only template에서 전체 문서 boilerplate 제거
- `admin-ops.html`이 분리되거나 내부 fragment로 명확히 조직됨
- 추천/API metrics 코드가 core business flow에 덜 침투함
