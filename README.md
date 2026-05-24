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

1. Copy environment file:

```bash
cp .env.example .env
```

2. Fill required values in `.env`.

3. Run app:

```bash
./gradlew.bat bootRun
```

4. Open:

- `http://localhost:8080`

## Test

- Unit/Integration:

```bash
./gradlew.bat test
```

- Performance smoke (`@Tag("perf")`):

```bash
./gradlew.bat performanceSmokeTest
```

- E2E smoke:

```bash
npm ci
npm run test:e2e
```

## Docker Compose

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
