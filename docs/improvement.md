# Codex / Claude Refactoring Brief

## Goal

Refactor the backend and frontend code quality of this repository without changing public behavior.

The purpose of this work is **code quality improvement**, not feature development, product expansion, performance tuning, or ML-style fine-tuning.

Primary goals:

- Reduce unnecessary structure and boilerplate.
- Consolidate small or fragmented frontend templates where appropriate.
- Extract overloaded backend orchestration from controllers.
- Keep domain logic stable.
- Keep existing API behavior, response DTOs, routes, and user-visible behavior unchanged.

## Context

현재 목표는 기능 추가가 아니라 코드 품질 개선입니다.  
The project already has a reasonable Spring Boot structure, but some areas are too fragmented while others are overloaded.

Important context:

- Backend code is generally stronger than frontend structure.
- Domain modeling is acceptable and should not be aggressively changed.
- `OpsController` is the main backend cleanup target.
- Thymeleaf fragments contain unnecessary full-document boilerplate.
- `admin-ops.html` is too large and should be split or internally organized.
- JavaScript and CSS should be reorganized carefully without adding a frontend build pipeline.

Use this document as an implementation guide for Codex, Claude Code, or another coding agent.

---

## Working Rules

### Do

- Work in small, reviewable changes.
- Preserve public behavior.
- Prefer refactoring over rewriting.
- Keep class, method, and file names clear and conventional.
- Run relevant tests after each meaningful change.
- Update tests only when the refactoring changes internal structure but not behavior.
- Keep commits focused.

### Do Not

- Do not add new product features.
- Do not change public endpoint paths.
- Do not rename public DTOs unless strictly necessary.
- Do not change response payload shape.
- Do not introduce a frontend framework.
- Do not introduce a bundler unless explicitly requested.
- Do not perform broad performance optimization unrelated to this refactoring.
- Do not treat this as ML fine-tuning.

---

## Backend Tasks

### Task 1: Extract collection orchestration from `OpsController`

Refactor `OpsController` so that it no longer owns collection lock orchestration directly.

Recommended target:

```text
OpsController
  -> OpsCollectionFacade
```

`OpsCollectionFacade` should own:

- ShedLock execution
- manual collection lock handling
- overlap handling
- collection command delegation

`OpsController` should remain responsible for:

- HTTP mapping
- request parameter normalization
- response header handling
- response DTO return

Constraints:

- Keep `/ops/collect` behavior unchanged.
- Keep `/ops/collect/missing` behavior unchanged.
- Keep existing response DTOs unchanged.
- Keep existing tests passing.

---

### Task 2: Remove broad `catch (Throwable)` handling

Replace broad `catch (Throwable)` control flow with narrower exception handling.

Preferred shape:

```java
catch (Exception e) {
    throw new CollectionLockException("collection lock failed", e);
}
```

Or catch ShedLock-specific exceptions if the dependency exposes a useful specific type.

Constraints:

- Do not swallow JVM-level errors.
- Do not convert `Error` subclasses into normal application exceptions.
- Add or update a focused test if the current test suite does not cover lock failure behavior.

---

### Task 3: Extract recommendation metrics recording

The recommendation path currently mixes business flow and metrics recording.

Introduce a small collaborator such as:

```text
RecommendMetricsRecorder
```

It may own:

- recommendation latency metric
- requested count metric
- generation failure metric
- rejection count/rate metric if appropriate

Target style:

```java
metrics.recordLatency(startedAtNanos);
metrics.recordRequestedCount(count);
metrics.recordFailure(reason);
```

Constraints:

- Do not change recommendation generation behavior.
- Do not change metric names unless necessary.
- Keep no-op behavior when `MeterRegistry` is unavailable.

---

### Task 4: Reduce API client operational noise

Review external API clients for repeated operational code.

Potential extraction targets:

```text
ExternalApiCallExecutor
ApiResponseValidator
ApiFailureClassifier
ExternalApiMetricsRecorder
```

This should be done conservatively.

Good refactoring:

- separates provider-specific parsing from generic retry/circuit/metric behavior
- reduces repeated failure handling
- keeps error semantics unchanged

Bad refactoring:

- creates too many tiny classes
- hides simple logic behind unnecessary abstractions
- changes exception behavior unintentionally

Constraints:

- Do not change external API request semantics.
- Do not change failure classification unless explicitly intended.
- Keep existing API client tests passing.

---

### Backend Code That Should Mostly Stay As-Is

Do not aggressively rewrite these unless required by the tasks above:

- `LottoCombination`
- `ExclusionRule` implementations
- `RecommendRuleConfig` rule ordering
- feature-sliced package layout
- public DTO records/classes

These areas are already acceptable and provide useful structure.

---

## Frontend Tasks

### Task 1: Remove unnecessary fragment boilerplate

Several Thymeleaf fragment-only files include full document wrappers:

```html
<!doctype html>
<html>
<body>
...
</body>
</html>
```

For fragment-only templates, remove this boilerplate and keep only the fragment root.

Preferred shape:

```html
<div th:fragment="recommend-card">
  ...
</div>
```

Candidate files:

```text
src/main/resources/templates/fragments/recommend-card.html
src/main/resources/templates/fragments/frequency-card.html
src/main/resources/templates/fragments/latest-card.html
src/main/resources/templates/fragments/round-search-card.html
src/main/resources/templates/fragments/rounds-list.html
src/main/resources/templates/fragments/lotto-ball.html
```

Constraints:

- Keep Thymeleaf fragment names unchanged.
- Keep HTMX target behavior unchanged.
- Keep rendered markup semantically equivalent.

---

### Task 2: Consolidate small home-only fragments

`latest-card.html` and `round-search-card.html` are small and home-specific.

Consolidate them into:

```text
src/main/resources/templates/fragments/home-static-cards.html
```

The new file may contain both fragments:

```html
<div th:fragment="latest-card">
  ...
</div>

<div th:fragment="round-search-card">
  ...
</div>
```

Then update references in `home.html`.

Constraints:

- Keep fragment names unchanged if possible.
- Do not change rendered output.
- Do not merge dynamic HTMX fragments into this file.

Keep separate:

```text
recommend-card.html
frequency-card.html
rounds-list.html
lotto-ball.html
```

---

### Task 3: Split or internally organize `admin-ops.html`

`admin-ops.html` is too large and mixes multiple UI sections.

Preferred split:

```text
src/main/resources/templates/admin/ops-filter.html
src/main/resources/templates/admin/retention-card.html
src/main/resources/templates/admin/failure-reasons.html
src/main/resources/templates/admin/failure-logs.html
src/main/resources/templates/admin/pagination.html
```

Less invasive alternative:

- Keep `admin-ops.html`
- Define clear internal Thymeleaf fragments inside the same file
- Recompose the page using those fragments

Choose the approach that minimizes risk while improving readability.

Constraints:

- Keep `/admin/ops` behavior unchanged.
- Keep all existing query parameters unchanged.
- Keep mobile and desktop views equivalent.
- Keep sorting and message toggle behavior working.

---

### Task 4: Reorganize `fragment-loader.js` internally

Do not rewrite it from scratch. Reorganize it by concern.

Suggested internal order:

```text
1. State variables
2. Accessibility helpers
3. Retry/backoff helpers
4. UI state helpers
5. Target resolution helpers
6. Fallback fetch loader
7. HTMX event bindings
8. Retry button binding
```

Constraints:

- Do not change public behavior.
- Keep HTMX fallback behavior.
- Keep retry backoff behavior.
- Keep live region announcements.
- Keep focus handling after dynamic content loads.

---

### Task 5: Reorganize `app.css` sections

Keep one CSS file unless a build pipeline is explicitly introduced later.

Recommended section order:

```text
1. Tokens
2. Base and layout
3. Navigation
4. Shared components
5. Public page sections
6. Ops dashboard sections
7. Utilities and accessibility helpers
8. Responsive overrides
```

Constraints:

- Do not change visual design intentionally.
- Do not remove dark mode behavior.
- Do not remove mobile safe-area handling.
- Do not break HTMX indicator styles.

---

## Implementation Order

Follow this order unless tests or conflicts require adjustment:

1. Remove unnecessary `doctype/html/body` wrappers from fragment-only templates.
2. Merge `latest-card.html` and `round-search-card.html` into `home-static-cards.html`.
3. Extract `OpsCollectionFacade` and move lock orchestration out of `OpsController`.
4. Replace `catch (Throwable)` with narrower exception handling.
5. Extract `RecommendMetricsRecorder`.
6. Reorganize `fragment-loader.js` internally without behavior changes.
7. Split or internally fragment `admin-ops.html`.
8. Review API client operational code and extract helpers only if it clearly improves readability.

---

## Verification

After each meaningful backend change, run relevant tests.

Minimum verification:

```bash
./gradlew test
```

For broader verification:

```bash
./gradlew check
```

For frontend behavior, run the existing E2E smoke flow if available in the current environment:

```bash
npm run test:e2e
```

If E2E cannot be run locally, document that clearly in the final summary.

---

## Done Criteria

This refactoring cycle is complete when:

- Public behavior remains unchanged.
- Existing backend tests pass.
- Existing E2E smoke tests pass, or the inability to run them is documented.
- `OpsController` is visibly smaller and no longer owns lock orchestration details.
- No normal application control flow catches `Throwable`.
- Fragment-only templates no longer contain full document boilerplate.
- Small home-only fragments are consolidated.
- `admin-ops.html` is split or internally organized into clear sections.
- `fragment-loader.js` is easier to review without behavior changes.
- `app.css` has clearer public/ops/style-section organization.
- Recommendation and API metrics code is less intrusive in core business flow.

---

## Suggested Commit Strategy

Use small commits. Recommended sequence:

```text
docs: clarify code quality refactoring plan
refactor: simplify thymeleaf fragment templates
refactor: consolidate home static fragments
refactor: extract ops collection facade
refactor: narrow ops lock exception handling
refactor: extract recommendation metrics recorder
refactor: organize fragment loader script
refactor: split admin ops template sections
refactor: reduce external api client operational noise
```

Do not combine unrelated backend and frontend changes in one large commit unless the repository workflow requires it.

---

## Final Summary Format for Coding Agents

When the work is complete, summarize in this format:

```text
Summary:
- ...
- ...

Behavior changes:
- None, or list exact changes.

Tests:
- ./gradlew test: passed/failed/not run
- ./gradlew check: passed/failed/not run
- npm run test:e2e: passed/failed/not run

Notes:
- Any skipped verification or follow-up risk.
```
