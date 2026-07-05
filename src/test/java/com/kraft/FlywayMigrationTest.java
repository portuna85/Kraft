package com.kraft;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Flyway 마이그레이션이 실제 MariaDB에서 오류 없이 적용되고,
 * Hibernate ddl-auto=validate 가 엔티티 매핑과 스키마가 일치함을 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("데이터베이스 마이그레이션 및 스키마 검증 테스트")
class FlywayMigrationTest {

    @Container
    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11.7")
            .withDatabaseName("kraft_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        Flyway.configure()
                .dataSource(mariadb.getJdbcUrl(), mariadb.getUsername(), mariadb.getPassword())
                .load()
                .migrate();
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "false");
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Test
    @DisplayName("마이그레이션이 성공적으로 적용되고 엔티티와 스키마가 일치한다")
    void migrationsApplySuccessfully_andSchemaMatchesEntities() {
        // 컨텍스트가 로드되면 테스트 통과:
        // 1. 모든 Flyway 마이그레이션이 MariaDB에서 오류 없이 적용됨
        // 2. Hibernate ddl-auto=validate 가 엔티티 매핑과 스키마 일치를 확인함
    }
}
