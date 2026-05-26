# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workspace Rules (from AGENTS.md)

- Do not commit or push any documentation files (except README.md and complement.md).
- Do not commit or push until explicitly requested.
- Do not modify `docs/improvement.md`. Use `complement.md` only to mark items complete or incomplete.

## Commands

```bash
# Run the application (requires MariaDB)
./gradlew bootRun

# Run unit and integration tests (excludes @Tag("perf"))
./gradlew test

# Run performance smoke tests only
./gradlew performanceSmokeTest

# Full check (Checkstyle + SpotBugs + tests + JaCoCo)
./gradlew check

# Strict static analysis (fail on violations)
./gradlew check -PstrictStatic=true

# Strict coverage enforcement (76% line, 59% branch, 80% method, 90% class)
./gradlew check -PstrictCoverage=true

# E2E smoke tests (run only inside Docker — Playwright requires Ubuntu 24.04 image)
docker run --rm -it -v "$PWD:/work" -w /work mcr.microsoft.com/playwright:v1.60.0-noble bash -lc "npm ci && npx playwright install chromium && npm run test:e2e"

# Validate UTF-8 encoding
python scripts/check_utf8.py

# Start DB only (local development)
docker compose -f docker-compose.local.yml up -d

# Full stack
docker compose up -d --build
```

## Architecture

### Package Layout

```
com.kraft.lotto
├── KraftLottoApplication.java
├── feature/
│   ├── winningnumber/        # Lotto draw data collection & query
│   │   ├── application/      # Services, API client, scheduler, circuit breaker
│   │   ├── domain/           # LottoCombination, WinningNumber, LottoRoundPolicy
│   │   ├── event/            # WinningNumbersCollectedEvent
│   │   ├── infrastructure/   # JPA entities, repositories (MariaDB + H2)
│   │   └── web/dto/          # Request/response DTOs, validators
│   ├── recommend/            # Number recommendation engine
│   │   ├── application/      # RecommendService, LottoRecommender, cache loader
│   │   ├── domain/           # ExclusionRule implementations
│   │   └── web/dto/
│   └── statistics/           # Winning number frequency summaries
│       ├── application/
│       └── infrastructure/
├── infra/config/             # Property bindings (KraftXxxProperties), startup validation
├── support/                  # Cross-cutting: filters, exception handler, error codes
└── web/                      # Top-level controllers (HomeController, OpsController)
```

### Key Design Patterns

**Feature-slice packaging**: Each feature owns its `application`, `domain`, `infrastructure`, and `web` sub-packages. Cross-feature dependencies flow through domain objects, not through internal service classes.

**ExclusionRule interface** (`feature/recommend/domain/ExclusionRule.java`): Strategy pattern for filtering biased lotto combinations. All `ExclusionRule` implementations are collected by Spring and injected as `List<ExclusionRule>` into `RecommendService`. The rule order is fixed at configuration time in `RecommendConfiguration`.

**LottoCollectionCommandService**: Mutex-guarded via `CollectionRunState` to prevent concurrent collection runs. Also coordinated at the cluster level via ShedLock (`COLLECT_ALL_LOCK_NAME`). Ops endpoints trigger this service through `OpsController`.

**ApiCircuitBreaker**: A hand-rolled circuit breaker (closed → open → half-open) wrapping `LottoApiClient` calls. Registered in `ApiCircuitBreakerRegistry`; state is observable via `GET /ops/circuit-breakers`.

**LottoApiClient implementations**: Three profiles — `mock` (local/test), `smok` (smoke/CI), and the real `DhLotteryApiClient` (prod). Selected via `kraft.api.client` config property (defaults to `mock` in local profile).

**Caching**: Caffeine caches for winning number frequency and combination prize history. Cache names and TTLs are configured in `KraftCacheProperties`. The `PastWinningCache` in the recommend feature is a separate in-memory structure loaded at startup.

**Security filters** (applied in `support/`):
- `PublicRateLimitFilter` — per-IP token bucket rate limiter
- `OpsAccessFilter` — IP allowlist + bearer token for `/ops/**`
- `ActuatorAccessFilter` — IP allowlist for `/actuator/**`
- `SecurityHeadersFilter` — CSP, HSTS, X-Frame-Options, etc.

**Ops endpoints** (`GET/POST /ops/**`): JSON REST API (no Thymeleaf). Protected by `OpsAccessFilter`. Documented via SpringDoc/OpenAPI (`/swagger-ui.html` — enabled in local profile only).

**HomeController**: Thymeleaf MVC controller serving the main SPA-like page. HTMX fragments are served at `/fragments/recommend`, `/fragments/frequency`, and `/fragments/rounds`.

### Profiles

| Profile | DB host | API client | Auto-collect | History init |
|---------|---------|------------|--------------|--------------|
| `local` (default) | `localhost` | `mock` | disabled | disabled |
| `prod` | `mariadb` (container) | real | enabled | enabled |

### Database

- MariaDB in production; H2 in-memory for unit tests.
- Schema managed by Flyway (`classpath:db/migration/V*.sql`). `ddl-auto=validate`.
- Integration tests use Testcontainers (`MariaDbContainerConfig`).
- ShedLock table (`shedlock`) coordinates distributed schedulers.

### Schedulers

- **`WinningNumberAutoCollectScheduler`**: Runs Saturday 22:30 and Sunday 07:00 (Asia/Seoul) to pull the latest draw from dhlottery.co.kr.
- **`LottoFetchLogRetentionScheduler`**: Purges old fetch logs daily at 03:30; controlled by `kraft.collect.log-retention.*`.

### Required Environment Variables

`KRAFT_DB_USER` and `KRAFT_DB_PASSWORD` are always required. In production, `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` must be set. All other properties have defaults defined in `application.yml`.
