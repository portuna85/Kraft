FROM eclipse-temurin:25-jdk@sha256:c2b7ea21649875fb9052237ac4e3cd4ef63968a2a389a0a1b1a72a5e53e5c93f AS build
WORKDIR /workspace
ARG GRADLE_BUILD_ARGS=""

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies -q

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test $GRADLE_BUILD_ARGS

FROM eclipse-temurin:25-jre@sha256:04262e8782d6b034ee5d7c1c5d4e8938fcf2063a76b4bfcd84e5d994d09c27bc
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 kraft \
 && useradd --system --uid 10001 --gid 10001 --home /app kraft

COPY --from=build /workspace/build/libs/app.jar /app/app.jar
COPY docker/healthcheck.sh /app/healthcheck.sh
RUN mkdir -p /app/logs \
 && chmod +x /app/healthcheck.sh \
 && chown -R kraft:kraft /app
USER kraft

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -Duser.timezone=Asia/Seoul" \
    KRAFT_LOG_PATH="/app/logs" \
    KRAFT_HEALTHCHECK_URL="http://localhost:8080/actuator/health/readiness" \
    KRAFT_HEALTHCHECK_TIMEOUT_SECONDS="3"

VOLUME ["/app/logs"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=12 \
  CMD /app/healthcheck.sh

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
