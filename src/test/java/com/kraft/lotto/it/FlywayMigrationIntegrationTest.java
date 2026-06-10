package com.kraft.lotto.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("it")
@Import(MariaDbContainerConfig.class)
@EnabledIf(value = "com.kraft.lotto.it.TestcontainersAvailability#isDockerAvailable",
        disabledReason = "Docker is not available for Testcontainers")
@DisplayName("Flyway 마이그레이션 MariaDB 통합 테스트")
class FlywayMigrationIntegrationTest {

    @Autowired
    WinningNumberRepository winningNumberRepository;

    @Test
    @DisplayName("실제 MariaDB에서 모든 Flyway 마이그레이션이 성공한다")
    void flywayMigrationsSucceed() {
        // 컨텍스트 로딩 = Flyway 마이그레이션 성공
    }

    @Test
    @DisplayName("빈 DB에서 최대 회차 조회는 empty를 반환한다")
    void emptyDatabaseReturnsNoMaxRound() {
        assertThat(winningNumberRepository.findMaxRound()).isEmpty();
    }
}
