# Code Quality Improvement Report

## Scope

This document focuses only on **backend and frontend code quality**. It intentionally excludes product completeness, CI/CD maturity, deployment reliability, runtime operations, business viability, and repository presentation unless they directly affect code maintainability.

Reviewed areas:

- Backend Java code under `src/main/java`
- Public controllers, ops controllers, application services, domain objects, API clients, filters, and configuration classes
- Thymeleaf templates under `src/main/resources/templates`
- Static JavaScript and CSS under `src/main/resources/static`
- Code compression and consolidation opportunities

---

## Executive Assessment

| Area | Score | Verdict |
|---|---:|---|
| Backend code quality | 76 / 100 | Structurally sound, but not yet lean. Good domain modeling and service separation, with some orchestration and metrics leakage. |
| Frontend code quality | 68 / 100 | Functionally careful and accessibility-aware, but fragmented and verbose. Consolidation would improve readability. |
| Expected score after targeted consolidation | ~80 / 100 | Achievable without changing business behavior. |

Overall, the codebase is above average for a personal Spring Boot project. It is not a shallow CRUD example. The backend has meaningful domain validation, explicit service boundaries, retry/circuit-breaker handling, and operational endpoints. The frontend shows care around HTMX fallback, CSP compatibility, accessibility, responsive layout, and progressive enhancement.

The main issue is not the absence of structure. The issue is that some parts are now **over-structured**, while others are **overloaded**. A targeted cleanup should focus on reducing ceremony, extracting orchestration from controllers, and consolidating small view fragments.

---

## Backend Code Quality

### Strengths

#### 1. Clear domain invariants

`LottoCombination` is a good domain object. It validates nulls, exact size, valid range, duplication, and normalizes number order at construction time.

Positive effects:

- Service code can trust `LottoCombination` instances.
- Invalid state is rejected early.
- Sorting and uniqueness are centralized.

This structure should be preserved.

#### 2. Reasonably thin public web controller

`HomeController` mainly handles request parameters, invokes services, and populates the model. It does not contain meaningful business logic.

This is acceptable controller design.

#### 3. Conceptually well-separated recommendation logic

The recommendation path has recognizable layers:

- `RecommendService`: request-level normalization, DTO conversion, exception mapping, metrics
- `LottoRecommender`: generation loop, duplicate filtering, rule evaluation, timeout behavior
- `ExclusionRule` implementations: individual filtering rules
- `RecommendRuleConfig`: explicit rule ordering

The design is understandable and extensible.

#### 4. Defensive external API handling

The API client code distinguishes HTTP errors, blank bodies, HTML responses, non-JSON responses, network errors, parser failures, retries, request timeout, and circuit-breaker state.

This is stronger than a naive `RestClient.get().retrieve()` implementation.

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

Alternatively, catch ShedLock-specific exceptions if available.

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
| 5 | Optional infrastructure dependencies | Introduce no-op collaborators where useful | Fewer null checks |

Keep the following as-is:

- `LottoCombination`
- `ExclusionRule` strategy structure
- feature-sliced package layout
- explicit rule ordering in `RecommendRuleConfig`

---

## Frontend Code Quality

### Strengths

#### 1. Directionally good layout and fragment composition

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
- accessible table sorting announcements

This is better than most lightweight Thymeleaf/HTMX interfaces.

#### 3. CSP-compatible JavaScript/CSS strategy

The application avoids inline behavior and uses static JavaScript files. HTMX inline indicator styles are disabled and replaced with static `.htmx-indicator` CSS.

This is a good baseline.

#### 4. Defensive JavaScript

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

For fragment-only files, this is unnecessary and increases visual noise.

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

Do not split it into many files unless a bundling step is introduced.

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
