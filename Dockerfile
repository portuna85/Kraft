FROM eclipse-temurin:25-jdk@sha256:edb3aa0f621796d8f5f9d602c7611ffdf015cd89e6ddda1894d85a3a99d170a8 AS build
WORKDIR /workspace

# 의존성 레이어 분리 — build.gradle.kts 변경 시에만 재다운로드
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.lockfile ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

# 소스 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre@sha256:5cf92df78f6dba978777d5cffa3c856e583f86814fde82a6c3534ccdfd794f2f
WORKDIR /app

# 컨테이너 친화적 JVM 옵션
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app
COPY --from=build /workspace/build/libs/*.jar app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
