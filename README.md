# kLo (kraft-lotto)

Spring Boot 4 based Lotto service.

## Tech Stack
- Java 25
- Spring Boot 4.0.5
- Spring Web, Thymeleaf, Validation, Data JPA, Flyway, Actuator, Cache
- MariaDB
- Gradle (Kotlin DSL)

## Project Structure
- `src/main/java/com/kraft/lotto`: application, domain, web, support
- `src/main/resources`: application config, flyway migrations, templates, static assets
- `src/test/java/com/kraft/lotto`: unit and integration tests
- `scripts/`: deployment and utility scripts
- `docker-compose.yml`: local app + MariaDB runtime

## Main Features
- winning number collection/scheduler
- winning number search/list APIs and pages
- statistics/frequency summary
- recommendation generation with rule-based constraints
- ops endpoints and ops page

## Prerequisites
- JDK 25
- Docker (optional, for compose runtime and container-based integration paths)

## Local Run
1. Copy env template.
2. Fill required values.
3. Run app.

```bash
cp .env.example .env
./gradlew.bat bootRun
```

Open `http://localhost:8080`.

## Test
```bash
./gradlew.bat test
```

Performance smoke tests:
```bash
./gradlew.bat performanceSmokeTest
```

## Docker Compose
```bash
docker compose up -d --build
```

## API and Ops Notes
- Actuator exposed endpoints: `health`, `info`
- OpenAPI UI dependency is included (`springdoc-openapi-starter-webmvc-ui`)
- Ops access is controlled by `KRAFT_SECURITY_OPS_*` settings

## Encoding Policy
- Repository text files should be UTF-8.
- If encoding checks are needed, run:

```bash
python scripts/check_utf8.py
```
