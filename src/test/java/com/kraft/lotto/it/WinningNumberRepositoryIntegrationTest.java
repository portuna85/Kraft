package com.kraft.lotto.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryEntity;
import com.kraft.lotto.feature.statistics.infrastructure.WinningNumberFrequencySummaryRepository;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("it")
@Import(MariaDbContainerConfig.class)
@Transactional
@EnabledIf(value = "com.kraft.lotto.it.TestcontainersAvailability#isDockerAvailable",
        disabledReason = "Docker is not available for Testcontainers")
@DisplayName("WinningNumberRepository 통합 테스트")
class WinningNumberRepositoryIntegrationTest {

    @Autowired
    WinningNumberRepository winningNumberRepository;

    @Autowired
    WinningNumberFrequencySummaryRepository summaryRepository;

    private WinningNumberEntity entity(int round) {
        return new WinningNumberEntity(round, LocalDate.of(2020, 1, 1),
                1, 2, 3, 4, 5, 6, 7, 1_000_000_000L, 1, 20_000_000_000L,
                LocalDateTime.now());
    }

    @Test
    @DisplayName("저장한 엔티티를 round로 조회할 수 있다")
    void saveAndFindById() {
        winningNumberRepository.save(entity(100));

        assertThat(winningNumberRepository.findById(100)).isPresent()
                .get()
                .satisfies(e -> {
                    assertThat(e.getRound()).isEqualTo(100);
                    assertThat(e.getN1()).isEqualTo(1);
                    assertThat(e.getBonusNumber()).isEqualTo(7);
                });
    }

    @Test
    @DisplayName("findAllByOrderByRoundDesc는 round 내림차순으로 반환한다")
    void findAllByOrderByRoundDescReturnsDescendingOrder() {
        winningNumberRepository.saveAll(List.of(entity(10), entity(30), entity(20)));

        var page = winningNumberRepository.findAllByOrderByRoundDesc(
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(WinningNumberEntity::getRound)
                .containsExactly(30, 20, 10);
    }

    @Test
    @DisplayName("findPrizeHitsByNumbers는 6개가 모두 일치하는 회차를 1등으로 반환한다")
    void findPrizeHitsByNumbersReturnsFirstPrizeHit() {
        winningNumberRepository.save(entity(500));

        var hits = winningNumberRepository.findPrizeHitsByNumbers(1, 2, 3, 4, 5, 6);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRound()).isEqualTo(500);
        assertThat(hits.get(0).getPrizeRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("findPrizeHitsByNumbers는 5개 + 보너스 일치 회차를 2등으로 반환한다")
    void findPrizeHitsByNumbersReturnsSecondPrizeHit() {
        WinningNumberEntity secondPrize = new WinningNumberEntity(
                600, LocalDate.of(2021, 6, 1),
                1, 2, 3, 4, 5, 45, 6,
                500_000_000L, 3, 10_000_000_000L,
                LocalDateTime.now());
        winningNumberRepository.save(secondPrize);

        var hits = winningNumberRepository.findPrizeHitsByNumbers(1, 2, 3, 4, 5, 6);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getRound()).isEqualTo(600);
        assertThat(hits.get(0).getPrizeRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("WinningNumberFrequencySummaryRepository saveAll은 upsert처럼 동작한다")
    void frequencySummaryUpsert() {
        var initial = List.of(
                new WinningNumberFrequencySummaryEntity(1, 5L, 100),
                new WinningNumberFrequencySummaryEntity(2, 3L, 100));
        summaryRepository.saveAll(initial);

        var updated = List.of(
                new WinningNumberFrequencySummaryEntity(1, 10L, 200),
                new WinningNumberFrequencySummaryEntity(2, 7L, 200));
        summaryRepository.saveAll(updated);

        var rows = summaryRepository.findAllByOrderByBallAsc();
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getHitCount()).isEqualTo(10L);
        assertThat(rows.get(0).getLastCalculatedRound()).isEqualTo(200);
        assertThat(rows.get(1).getHitCount()).isEqualTo(7L);
    }
}
