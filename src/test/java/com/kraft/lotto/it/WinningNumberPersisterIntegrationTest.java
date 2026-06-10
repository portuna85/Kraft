package com.kraft.lotto.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.winningnumber.application.UpsertOutcome;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberPersister;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
@DisplayName("당첨 번호 저장기 통합 테스트 (테스트컨테이너)")
class WinningNumberPersisterIntegrationTest {

    private static final List<Integer> TEST_ROUNDS = List.of(9901, 9902, 9903);

    @Autowired
    WinningNumberPersister persister;

    @Autowired
    WinningNumberRepository repository;

    @AfterEach
    void cleanup() {
        TEST_ROUNDS.forEach(repository::deleteById);
    }

    @Test
    @DisplayName("최초 저장은 삽입됨를 반환한다")
    void firstSaveReturnsInserted() {
        UpsertOutcome outcome = persister.upsert(sample(9901));

        assertThat(outcome).isEqualTo(UpsertOutcome.INSERTED);
    }

    @Test
    @DisplayName("동일 데이터 재저장은 변경 없음를 반환한다")
    void sameSaveReturnsUnchanged() {
        WinningNumber wn = sample(9902);
        persister.upsert(wn);

        UpsertOutcome outcome = persister.upsert(wn);

        assertThat(outcome)
                .as("동일 데이터 두 번째 upsert — actual: %s", outcome)
                .isEqualTo(UpsertOutcome.UNCHANGED);
    }

    @Test
    @DisplayName("데이터 변경 후 저장은 수정됨를 반환한다")
    void changedSaveReturnsUpdated() {
        persister.upsert(sample(9903));
        WinningNumber changed = new WinningNumber(
                9903,
                LocalDate.of(2020, 3, 7),
                new LottoCombination(List.of(2, 4, 6, 8, 10, 12)),
                14,
                5_000_000_000L,
                2,
                100_000_000_000L,
                50_000_000_000L,
                "{\"updated\":true}",
                null
        );

        UpsertOutcome outcome = persister.upsert(changed);

        assertThat(outcome).isEqualTo(UpsertOutcome.UPDATED);
    }

    private WinningNumber sample(int round) {
        return new WinningNumber(
                round,
                LocalDate.of(2020, 1, 4),
                new LottoCombination(List.of(7, 15, 25, 27, 31, 35)),
                39,
                2_000_000_000L,
                3,
                80_000_000_000L,
                20_000_000_000L,
                "{\"returnValue\":\"success\"}",
                null
        );
    }
}
