# Completion Record

Date: 2026-05-24

Based on `docs/improvement.md` and `docs/completed.md`, implemented and verified the following:

- Fixed failing cache configuration test by supplying `KraftCacheProperties` in `CacheConfigTest`.
- Removed silent clamping in public `HomeController` fragment pagination path:
  - `page` now uses `@Min(0)`
  - `size` now uses `@Min(1) @Max(100)`
  - direct validated values are passed to query service.
- Added validation exception handling in ops JSON advice:
  - `OpsExceptionHandler` now handles `ConstraintViolationException` and returns `REQUEST_VALIDATION_ERROR`.
- Hardened SEO base URL handling:
  - `SeoController` now only accepts absolute `http/https` URL with host and without query/fragment/userinfo.
  - XML output now escapes `loc` values safely.
- Removed ops dashboard exposure in PWA shortcut:
  - `manifest.json` no longer includes `/admin/ops` shortcut.

Verification:

- Targeted tests passed:
  - `CacheConfigTest`
  - `HomeControllerTest`
  - `HomeControllerWebMvcTest`
  - `OpsControllerTest`
- Full test suite passed:
  - `./gradlew.bat test`

---

Additional progress (incomplete items follow-up):

- Replaced `OpsPageController` clamp-style parameter normalization with validation constraints:
  - `reasonLimit/logLimit`: `@Min(1) @Max(2000)`
  - `page`: `@Min(0)`
  - `pageSize`: `@Min(5) @Max(100)`
  - out-of-range requests now return `400` instead of silent correction.
- Rewrote `OpsPageControllerTest` to align with validation policy:
  - valid request binds model and calls service with original validated values.
  - invalid range request returns `400`.
- Rewrote `tests/e2e/smoke.spec.js` with clean readable assertions:
  - home render
  - latest card
  - lazy-load recommend/frequency/rounds
  - CSP console violation check
  - `round=1` success and out-of-range `round=9999` returns `400`
  - mobile bottom nav visibility

Verification for this follow-up:

- `node --check tests/e2e/smoke.spec.js`
- `./gradlew.bat test --tests com.kraft.lotto.web.OpsPageControllerTest --tests com.kraft.lotto.web.OpsControllerTest --tests com.kraft.lotto.web.HomeControllerWebMvcTest`

---

Step-by-step progress update:

Step 1 completed: CI Playwright smoke integration

- Added `e2e-smoke` job to `.github/workflows/ci.yml` after `build-test`.
- Job provisions JDK + Node, installs Playwright Chromium, starts app with `test` profile, waits for readiness, runs `npm run test:e2e`, uploads E2E artifacts.
- Verified test discovery locally:
  - `npm run -s test:e2e -- --list` (9 tests listed)
- Verified related web tests:
  - `./gradlew.bat test --tests com.kraft.lotto.web.OpsPageControllerTest` (passed)

Step 2 completed: core template text recovery

- Recovered broken mojibake text in key user-facing templates:
  - `templates/layout/base.html`
  - `templates/home.html`
  - `templates/fragments/latest-card.html`
  - `templates/fragments/rounds-list.html`
- Standardized labels and loading/error copy into clean readable text to remove rendering noise and malformed markup risk.

Verification:

- `./gradlew.bat test --tests com.kraft.lotto.web.HomeControllerTest --tests com.kraft.lotto.web.HomeControllerWebMvcTest --tests com.kraft.lotto.web.OpsPageControllerTest`
- `npm run -s test:e2e -- --list`

Step 3 completed: Korean text restoration and remaining template cleanup

- Reverted prior English text replacement and restored Korean UX copy in UTF-8.
- Completed cleanup for all remaining templates:
  - `templates/layout/base.html`
  - `templates/home.html`
  - `templates/admin-ops.html`
  - `templates/error.html`
  - `templates/fragments/latest-card.html`
  - `templates/fragments/round-search-card.html`
  - `templates/fragments/recommend-card.html`
  - `templates/fragments/frequency-card.html`
  - `templates/fragments/rounds-list.html`
  - `templates/fragments/lotto-ball.html`

Verification:

- `./gradlew.bat test --tests com.kraft.lotto.web.HomeControllerTest --tests com.kraft.lotto.web.HomeControllerWebMvcTest --tests com.kraft.lotto.web.OpsPageControllerTest --tests com.kraft.lotto.web.OpsControllerTest`
- Broken-text residue search on templates (`rg -F`) returned no matches for known mojibake markers.
