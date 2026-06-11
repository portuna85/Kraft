plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.5.4"
}

val strictStatic = providers.gradleProperty("strictStatic")
    .map { it.toBooleanStrictOrNull() ?: false }
    .getOrElse(false)
val strictCoverage = providers.gradleProperty("strictCoverage")
    .map { it.toBooleanStrictOrNull() ?: false }
    .getOrElse(false)

group = "com.kraft"
version = "0.2.0"

extra["tomcat.version"] = "11.0.22"
// CVE-2026-42583, CVE-2026-42584, CVE-2026-42587, CVE-2026-44249, CVE-2026-45416
// io.netty 4.2.12.Final → 4.2.15.Final (Spring Boot BOM override)
extra["netty.version"] = "4.2.15.Final"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    val lombokVersion = "1.18.46"

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-mysql")
    implementation("net.javacrumbs.shedlock:shedlock-spring:6.9.2")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.9.2")
    implementation("com.google.firebase:firebase-admin:9.4.3") {
        // CVE-2025-55163: grpc-netty-shaded 1.69.0의 Netty HTTP/2 취약점 수정 (fix: 1.75.0)
        exclude(group = "io.grpc", module = "grpc-netty-shaded")
    }
    implementation("io.grpc:grpc-netty-shaded:1.75.0")

    // Spring 7 ClassFileMetadataReader는 named module(tomcat-embed-core) 내 jakarta/servlet/Filter.class를
    // getResourceAsStream으로 읽지 못한다(Java 25 모듈 캡슐화). 독립 JAR을 unnamed module에 올려 우회.
    runtimeOnly("jakarta.servlet:jakarta.servlet-api")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    // H2는 prod에서 사용하지 않으나 E2E(playwright)가 JAR를 H2 모드로 기동하므로 runtimeOnly 유지
    runtimeOnly("com.h2database:h2")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-mariadb")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testRuntimeOnly("com.h2database:h2")
}

checkstyle {
    toolVersion = "10.25.0"
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<Checkstyle>().configureEach {
    ignoreFailures = !strictStatic
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    ignoreFailures = !strictStatic
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(true)
    }
}

tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest", "checkstyleMain", "checkstyleTest")
    if (strictCoverage) {
        dependsOn("jacocoTestCoverageVerification")
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.82".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.65".toBigDecimal()
            }
            limit {
                counter = "METHOD"
                value = "COVEREDRATIO"
                minimum = "0.88".toBigDecimal()
            }
            limit {
                counter = "CLASS"
                value = "COVEREDRATIO"
                minimum = "0.97".toBigDecimal()
            }
        }
    }
}

// ── 프론트엔드(Next.js) 빌드 통합 ──────────────────────────────────────────
// 설정 캐시 비호환 태스크: Exec/Copy 람다가 스크립트 객체를 캡처하므로
// notCompatibleWithConfigurationCache 로 명시, bootJar 빌드 시에만 트리거됨.
val frontendDirPath: String = projectDir.resolve("frontend").absolutePath
val isWindowsBuild: Boolean = System.getProperty("os.name").lowercase().contains("windows")

val npmCi = tasks.register<Exec>("npmCi") {
    notCompatibleWithConfigurationCache("npm Exec 태스크는 설정 캐시 미지원")
    onlyIf {
        val skip = providers.environmentVariable("SKIP_FRONTEND").map { it.isNotBlank() }.getOrElse(false)
        !skip && file(frontendDirPath).exists()
    }
    workingDir(frontendDirPath)
    if (isWindowsBuild) commandLine("cmd", "/c", "npm", "ci", "--prefer-offline")
    else commandLine("npm", "ci", "--prefer-offline")
}

val buildFrontend = tasks.register<Exec>("buildFrontend") {
    notCompatibleWithConfigurationCache("npm Exec 태스크는 설정 캐시 미지원")
    dependsOn(npmCi)
    onlyIf {
        val skip = providers.environmentVariable("SKIP_FRONTEND").map { it.isNotBlank() }.getOrElse(false)
        !skip && file(frontendDirPath).exists()
    }
    workingDir(frontendDirPath)
    if (isWindowsBuild) commandLine("cmd", "/c", "npm", "run", "build")
    else commandLine("npm", "run", "build")
    environment("NODE_ENV", "production")
}

val copyFrontend = tasks.register<Copy>("copyFrontend") {
    notCompatibleWithConfigurationCache("프론트엔드 Copy 태스크는 설정 캐시 미지원")
    dependsOn(buildFrontend)
    onlyIf {
        val skip = providers.environmentVariable("SKIP_FRONTEND").map { it.isNotBlank() }.getOrElse(false)
        !skip && file(frontendDirPath).exists()
    }
    from("$frontendDirPath/out")
    into(layout.buildDirectory.dir("resources/main/static"))
}

// resolveMainClassName 이 build/resources/main 을 읽고 copyFrontend 가 같은 경로에 쓰므로
// CC 암묵적 의존관계 검증 오류를 피하기 위해 명시적 순서 선언
tasks.named("resolveMainClassName") {
    dependsOn(copyFrontend)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn(copyFrontend)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("user.timezone", "Asia/Seoul")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("perf")
    }
}

tasks.register<Test>("performanceSmokeTest") {
    description = "Runs @Tag(\"perf\") performance smoke tests"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("perf")
    }
    shouldRunAfter("test")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
}

springBoot {
    buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.jar {
    enabled = false
}
