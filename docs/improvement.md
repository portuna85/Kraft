# Project Improvement Report

## Status

This report is based on a repository-wide static review of application code, tests, build configuration, CI/CD workflows, deployment scripts, static assets, templates, and local project hygiene.

Current project state: incomplete.

The project has a solid baseline: feature areas are separated, tests are broad, CI builds include static analysis and coverage, and production deployment has readiness and rollback steps. The main improvement opportunities are CI reliability, operational hardening, duplicated external API handling, and frontend CSP/a11y resilience.

## Scope Reviewed

- Java application code under `src/main/java`
- Unit, slice, integration, property, and smoke tests under `src/test/java`
- Thymeleaf templates and static assets under `src/main/resources`
- Flyway migrations under `src/main/resources/db/migration`
- Gradle build and verification configuration
- GitHub Actions CI/CD workflows
- Docker, Docker Compose, and deployment scripts
- Playwright E2E smoke tests
- Git hygiene and ignored local artifacts

## Highest Priority Improvements

### 1. Make CI Security Scans Reliable Again

File: `.github/workflows/ci.yml`

The current Trivy and Syft steps are non-blocking because recent CI failures were caused by external action/download instability. This keeps CI moving, but it also means supply-chain and vulnerability scan failures no longer block `main`.

Recommended work:

- Pin Trivy and Syft to explicit release versions.
- Prefer a stable install source or cached binary path.
- Keep PR scans optional or skipped for speed.
- Restore blocking security scans on `push` to `main`.
- Add a clear step summary when scans are skipped or fail non-blocking.

Expected impact:

- Higher confidence in `main`.
- Less CI instability from transient download failures.
- Better audit trail for security checks.

### 2. Add Explicit htmx Indicator Styles

File: `src/main/resources/templates/layout/base.html`

The htmx runtime inline indicator style injection is disabled with:

```html
<meta name="htmx-config" content='{"includeIndicatorStyles":false}'>
```

This fixes strict CSP violations, but no replacement `.htmx-indicator` styles were found in app CSS.

Recommended work:

- Add `.htmx-indicator`, `.htmx-request .htmx-indicator`, and `.htmx-request.htmx-indicator` rules to `src/main/resources/static/css/app.css`.
- Keep these styles static and CSP-compatible.
- Add an E2E assertion that lazy sections still expose loading state correctly.

Expected impact:

- CSP remains strict.
- Loading indicators remain visually functional.
- Future htmx upgrades are less likely to reintroduce inline-style violations.

### 3. Deduplicate External API Client Retry Logic

Files:

- `src/main/java/com/kraft/lotto/feature/winningnumber/application/DhLotteryApiClient.java`
- `src/main/java/com/kraft/lotto/feature/winningnumber/application/SmokLottoApiClient.java`

Both API clients repeat the same operational patterns:

- retry loop
- deadline checks
- circuit breaker permission checks
- latency metrics
- retry metrics
- retriable status classification
- backoff sleep

Recommended work:

- Extract a small `ExternalApiCallExecutor` or similar package-private helper.
- Keep each client responsible only for URI construction, raw fetch, response parsing, and provider-specific failure classification.
- Normalize metric reason names between providers.

Expected impact:

- Fewer inconsistent failure paths.
- Easier future provider additions.
- Smaller and more focused API client tests.

### 4. Narrow Ops Lock Exception Handling

File: `src/main/java/com/kraft/lotto/web/OpsController.java`

`withLock` currently catches `Throwable`, wraps it, and rethrows as `RuntimeException`.

Recommended work:

- Catch `Exception` or the specific ShedLock execution exceptions instead of `Throwable`.
- Let JVM-level errors propagate naturally.
- Add a focused test for lock acquisition failure and action failure.

Expected impact:

- Cleaner operational failure behavior.
- Lower risk of masking serious runtime errors.

### 5. Prepare Rate Limiting for Multi-Instance Deployment

File: `src/main/java/com/kraft/lotto/support/PublicRateLimitFilter.java`

The current rate limiter uses in-memory Caffeine counters. This is simple and effective for one app instance, but limits become per-instance when the service is horizontally scaled.

Recommended work:

- Keep Caffeine as the default local implementation.
- Introduce an interface for rate limit storage.
- Add a Redis-backed implementation for production multi-instance mode.
- Add `X-Forwarded-For` trust tests that verify proxy and trusted-proxy behavior together.

Expected impact:

- More predictable public endpoint protection under scale.
- Cleaner path to distributed deployment.

## Medium Priority Improvements

### 6. Strengthen Production Security Configuration

File: `src/main/resources/application.yml`

HSTS defaults to disabled. This is reasonable for local development, but production should fail fast if HTTPS hardening is unintentionally disabled.

Recommended work:

- Add a prod-profile test asserting HSTS is enabled when production configuration is loaded.
- Consider validating production `KRAFT_PUBLIC_BASE_URL` is HTTPS.
- Keep CSP strict and covered by E2E tests.

Expected impact:

- Fewer silent production security regressions.

### 7. Split CI Build Concerns

File: `.github/workflows/ci.yml`

The `Build + Test + Static analysis + Coverage` step runs several expensive checks as one Gradle invocation. This is simple, but when it fails, failure localization depends on Gradle output.

Recommended work:

- Keep one full strict check on `main`.
- For PRs, consider separating `test`, `checkstyle`, `spotbugs`, and `coverage` into named steps.
- Upload reports after each category with `if: always()`.

Expected impact:

- Faster failure diagnosis.
- Better CI observability.

### 8. Make Deployment Rollback More Explicit

Files:

- `.github/workflows/cd.yml`
- `scripts/deploy/build-and-up.sh`
- `scripts/deploy/rollback.sh`
- `scripts/deploy/wait-readiness.sh`

Deployment already validates prerequisites, renders `.env`, restarts services, checks readiness, and rolls back on failure. The next improvement is more explicit state tracking.

Recommended work:

- Record the previous image/container state before restart.
- Add deployment marker output to the GitHub summary.
- Make rollback report exactly which image/tag was restored.
- Add a dry-run validation mode for compose/env rendering.

Expected impact:

- Easier production incident debugging.
- Safer self-hosted runner deployment behavior.

### 9. Improve External API Failure Reason Consistency

Files:

- `DhLotteryApiClient.java`
- `SmokLottoApiClient.java`
- `FetchFailureReasonSupport.java`

Failure reason names are close but not fully aligned between providers. For example, one path uses provider-specific parse or invalid-number reasons.

Recommended work:

- Define a single enum or sealed-like central mapping for API failure reasons.
- Keep raw provider details in logs or metadata.
- Use normalized reasons in metrics and fetch logs.

Expected impact:

- Cleaner ops dashboards.
- Easier failure trend analysis.

### 10. Review Static Vendor Asset Strategy

Files:

- `src/main/resources/static/vendor/htmx/htmx.min.js`
- `src/main/resources/static/vendor/bootstrap/bootstrap.min.css`
- `build.gradle.kts`

The project both vendors Bootstrap CSS statically and also declares `org.webjars:bootstrap`. This may be intentional, but it should be made consistent.

Recommended work:

- Choose one source for Bootstrap.
- If static vendoring is preferred, remove the unused WebJar dependency.
- If WebJar is preferred, serve from WebJar paths and remove static duplicate assets.

Expected impact:

- Smaller dependency surface.
- Less confusion during upgrades.

## Lower Priority Improvements

### 11. Add Dependency Update Hygiene

Files:

- `build.gradle.kts`
- `package.json`
- `package-lock.json`

Recommended work:

- Add Dependabot or Renovate grouping for Gradle and npm dependencies.
- Group Playwright updates with container image updates.
- Add a CI check that fails when Playwright package and container image versions drift.

Expected impact:

- Fewer surprise E2E failures after package updates.

### 12. Improve E2E Local Developer Experience

Files:

- `playwright.config.js`
- `package.json`
- `.github/workflows/ci.yml`

Local E2E execution can fail if Playwright browser binaries are missing, while CI uses a Playwright container image.

Recommended work:

- Add `test:e2e:install` script for local browser installation.
- Document the local command in README only if documentation policy allows it.
- Keep CI container flow unchanged.

Expected impact:

- Less local setup friction.

### 13. Revisit Java Version Operational Risk

Files:

- `build.gradle.kts`
- `.github/workflows/ci.yml`
- `Dockerfile`

The project targets Java 25. This may be intentional, but it increases dependency on runner/toolchain/container support.

Recommended work:

- Confirm the production image and self-hosted runner consistently support Java 25.
- Add a CI check that prints `java -version` and `./gradlew --version`.
- Consider whether Java 21 LTS would reduce operational risk.

Expected impact:

- Fewer toolchain surprises.

## Test Coverage Observations

The test suite is broad and covers:

- configuration binding and validation
- security filters
- controllers and web slices
- API clients
- collection services
- repository integration
- Flyway migrations
- recommendation rules
- statistics and cache behavior
- E2E smoke, accessibility, CSP, and responsive behavior

Recommended additions:

- E2E check for htmx loading indicator behavior after disabling inline styles.
- Tests for distributed or trusted-proxy rate limit behavior.
- Tests for CD script dry-run behavior if scripts are made testable.
- Provider-neutral failure reason mapping tests if API failure normalization is introduced.

## Suggested Implementation Order

1. Restore blocking security scans on `main` with pinned CLI versions.
2. Add static htmx indicator CSS and E2E assertion.
3. Extract shared external API retry/circuit/metric execution helper.
4. Narrow `OpsController` lock exception handling.
5. Normalize API failure reasons.
6. Add production security configuration assertions.
7. Improve deployment summary and rollback traceability.
8. Decide static-vendor versus WebJar Bootstrap strategy.

## Completion Criteria

The project should be considered complete for this improvement cycle when:

- CI passes consistently for build, static analysis, coverage, and E2E.
- Security scans are blocking again on `main`.
- Strict CSP remains green in E2E.
- Production deployment includes clear rollback state reporting.
- External API client retry and failure behavior is shared or normalized.
- `node_modules`, test reports, local docs, local tool folders, and secrets remain untracked.
