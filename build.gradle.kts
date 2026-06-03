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
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val lombokVersion = "1.18.46"

    implementation("org.springframework.boot:spring-boot-starter-web")
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
    // bootstrap CSS는 src/main/resources/static/vendor/bootstrap/ 에서 직접 서빙

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    // H2는 prod에서 사용하지 않으나 E2E(playwright)가 JAR를 H2 모드로 기동하므로 runtimeOnly 유지
    runtimeOnly("com.h2database:h2")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
