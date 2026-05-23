plugins {
    java
    id("org.springframework.boot") version "4.0.5"
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
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.8")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.flywaydb:flyway-mysql")
    implementation("net.javacrumbs.shedlock:shedlock-spring:6.9.2")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.9.2")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.8")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("net.jqwik:jqwik:1.9.2")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-mariadb")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
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
                minimum = "0.75".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
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
