# kLo (kraft-lotto)

Spring Boot 4 based Lotto 6/45 web service.

## Tech Stack

- Java 25
- Spring Boot 4.0.5
- Spring Web, Thymeleaf, Validation, Data JPA, Flyway, Actuator, Cache
- MariaDB
- Gradle (Kotlin DSL)
- Playwright (E2E smoke)

## Project Structure

- `src/main/java/com/kraft/lotto`: application code
- `src/main/resources`: app config, Flyway migrations, templates, static assets
- `src/test/java/com/kraft/lotto`: unit and integration tests
- `tests/e2e`: Playwright smoke tests
- `scripts`: deploy/db/validation scripts

## Local Run

1. Start a local MariaDB instance.
   - Default local connection: `localhost:3306`
   - DB-only local compose: `docker compose -f docker-compose.local.yml up -d`
   - Existing full compose service target: `docker compose up -d mariadb`
   - Use `.env.local.example` as the starting template, or export values directly in your shell or IntelliJ Run Configuration.

2. Run the application from IntelliJ using `KraftLottoApplication`
   or from the terminal:

```bash
./gradlew bootRun
```

3. Open:

- `http://localhost:8080`

Local mode defaults to the `local` profile, uses MariaDB on `localhost`, and keeps the external API client and auto-collection disabled so the app can start cleanly from the IDE.

### IntelliJ Run Configuration

- Main class: `com.kraft.lotto.KraftLottoApplication`
- Active profile: `local` or leave blank and rely on the default
- Required DB values when using the defaults:
  - `KRAFT_DB_USER=<your user>`
  - `KRAFT_DB_PASSWORD=<your password>`
- Optional overrides:
  - `KRAFT_DB_URL=jdbc:mariadb://localhost:3306/kraft_lotto?...`
  - `KRAFT_DB_HOST=localhost`
  - `KRAFT_DB_NAME=kraft_lotto`
  - `KRAFT_DOTENV_PATH=/absolute/path/to/.env`

### Env Templates

- `.env.local.example`: local MariaDB, local API defaults, and IDE-friendly settings
- `.env.prod.example`: production-style values and container-oriented defaults
- `.env.example`: index file that points to the two templates above

## Test

- Local (Ubuntu 26.04): run only Gradle unit/integration tests.

```bash
./gradlew test
```

- Performance smoke (`@Tag("perf")`):

```bash
./gradlew performanceSmokeTest
```

- E2E smoke:

```bash
# Run only in CI or Docker based on the official Playwright Ubuntu 24.04 image.
docker run --rm -it -v "$PWD:/work" -w /work mcr.microsoft.com/playwright:v1.60.0-noble bash -lc "npm ci && npx playwright install chromium && npm run test:e2e"
```

## Docker Compose

Local DB-only:

```bash
docker compose -f docker-compose.local.yml up -d
```

Full stack:

```bash
docker compose up -d --build
```

## Ops & API Notes

- Actuator exposure: `health`, `info`
- Ops endpoints are protected by IP allowlist and token configuration
- Production requires `KRAFT_SECURITY_OPS_*` values

## UTF-8 Validation

All text files should be UTF-8 without BOM.

```bash
python scripts/check_utf8.py
```

The CI pipeline also runs this validation.

## Workspace Policy

- Local test artifacts are not committed:
  - `node_modules/`
  - `test-results/`
- If they are created during local validation, remove them before commit.
- Completion log file standard: `docs/completedd.md` (force-add may be required due ignore rules).
