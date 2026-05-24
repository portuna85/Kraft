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
