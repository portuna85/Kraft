package com.kraft.lotto.infra.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    // V1–V18 squash 이전에 적용된 V1__init_winning_numbers.sql의 체크섬
    private static final int LEGACY_V1_CHECKSUM = -2024252480;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            repairV1SquashIfNeeded(dataSource);
            flyway.migrate();
        };
    }

    private static void repairV1SquashIfNeeded(DataSource dataSource) {
        String stored = queryV1Checksum(dataSource);
        if (!String.valueOf(LEGACY_V1_CHECKSUM).equals(stored)) {
            return;
        }
        log.warn("Flyway V1 checksum mismatch detected (squash repair). "
            + "Resetting schema history to baseline...");
        applyBaselineRepair(dataSource);
        log.info("Flyway schema history reset to V1 baseline.");
    }

    private static String queryV1Checksum(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT checksum FROM flyway_schema_history WHERE version = '1'")) {
            return rs.next() ? String.valueOf(rs.getObject("checksum")) : null;
        } catch (Exception e) {
            // 테이블 미존재(최초 설치) 또는 연결 오류 — Flyway가 정상 처리
            return null;
        }
    }

    private static void applyBaselineRepair(DataSource dataSource) {
        String insert = "INSERT INTO flyway_schema_history"
            + " (installed_rank, version, description, type, script,"
            + "  checksum, installed_by, installed_on, execution_time, success)"
            + " VALUES (1, '1', 'baseline', 'BASELINE', 'V1__baseline.sql',"
            + "         NULL, CURRENT_USER(), NOW(), 0, 1)";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM flyway_schema_history");
            stmt.execute(insert);
        } catch (Exception e) {
            throw new IllegalStateException("Flyway V1 squash repair failed", e);
        }
    }
}
