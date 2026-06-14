plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    jacoco
    checkstyle
    id("com.github.spotbugs") version "6.1.7"
}

group = "com.kraft"
version = "1.0-SNAPSHOT"

// Lock runtime and compile classpaths for reproducible Dockerfile builds (blueprint §10.3)
dependencyLocking {
    lockAllConfigurations()
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("net.javacrumbs.shedlock:shedlock-spring:7.7.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.7.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.4.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:4.1.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.0")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test:4.1.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mariadb")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// ── JaCoCo ─────────────────────────────────────────────────────────────────

jacoco {
    toolVersion = "0.8.13"
}

// Patterns excluded from coverage measurement (shared by report + verification).
// Excludes: Spring config/properties, DTO/record boilerplate, JPA entities
// (no logic beyond getters), enums, event records, and admin Thymeleaf layer
// (controller/handlers/filter — covered by smoke tests, not unit tests).
val jacocoExcludes = listOf(
    "**/Application.class",
    "**/config/**",
    "**/*Properties.class",
    "**/*Response.class",
    "**/*Request.class",
    "**/*Dto.class",
    "**/db/migration/**",
    // JPA entities — getters/constructors only, no business logic
    "**/winningnumber/WinningNumber.class",
    "**/saved/SavedNumber.class",
    "**/admin/AdminUser.class",
    "**/admin/AdminAuditLog.class",
    "**/statistics/FrequencySummary.class",
    "**/statistics/PatternStatsSummary.class",
    "**/statistics/CompanionPairSummary.class",
    "**/operationlog/WinningNumberOperationLog.class",
    // Enums
    "**/WinningNumberOperationType.class",
    "**/WinningNumberOperationStatus.class",
    // Event records
    "**/*Event.class",
    // Admin Thymeleaf layer — tested by smoke tests, not unit tests
    "**/admin/AdminController.class",
    "**/admin/AdminLoginHandler.class",
    "**/admin/AdminLockoutFilter.class"
)

fun jacocoClassDirs() = fileTree(layout.buildDirectory.dir("classes/java/main")) {
    exclude(jacocoExcludes)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(jacocoClassDirs())
}

val strictCoverage: String? by project
if (strictCoverage == "true") {
    tasks.jacocoTestCoverageVerification {
        dependsOn(tasks.jacocoTestReport)
        classDirectories.setFrom(jacocoClassDirs())
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
    tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
}

// ── Checkstyle ──────────────────────────────────────────────────────────────

checkstyle {
    toolVersion = "10.23.0"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = (project.findProperty("strictStatic") != "true")
}

tasks.withType<Checkstyle> {
    reports {
        xml.required = false
        html.required = true
    }
}

// ── SpotBugs ────────────────────────────────────────────────────────────────

spotbugs {
    toolVersion = "4.9.3"
    // SpotBugs 4.9.3 does not support Java 25 class file format (major version 69).
    // Always ignore failures until SpotBugs adds Java 25 support.
    ignoreFailures = true
    excludeFilter = file("config/spotbugs/exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports.create("html") { required = true }
    reports.create("xml") { required = false }
}
