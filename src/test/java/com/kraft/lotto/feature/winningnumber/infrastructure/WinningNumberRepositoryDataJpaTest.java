package com.kraft.lotto.feature.winningnumber.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.kraft.lotto.TestCacheConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(TestCacheConfig.class)
@DisplayName("WinningNumberRepository DataJpa 슬라이스 테스트")
class WinningNumberRepositoryDataJpaTest {

    @Autowired
    WinningNumberRepository repository;

    @Test
    @DisplayName("findAllNumbersForFrequency는 저장된 회차당 6개의 번호를 반환한다")
    void findAllNumbersForFrequencyReturnsRows() {
        repository.save(entity(
                1001, LocalDate.of(2026, 5, 10),
                1, 2, 3, 4, 5, 6,
                7
        ));
        repository.save(entity(
                1002, LocalDate.of(2026, 5, 17),
                8, 9, 10, 11, 12, 13,
                14
        ));

        List<Object[]> rows = repository.findAllNumbersForFrequency();

        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(row -> assertThat(row).hasSize(6));
    }

    @Test
    @DisplayName("findPrizeHitsByNumbers는 1등과 2등 당첨 회차를 반환한다")
    void findPrizeHitsByNumbersReturnsExpectedRanks() {
        repository.save(entity(
                1100, LocalDate.of(2026, 5, 10),
                1, 2, 3, 4, 5, 6,
                7
        ));
        repository.save(entity(
                1101, LocalDate.of(2026, 5, 17),
                1, 2, 3, 4, 5, 45,
                6
        ));

        List<WinningNumberRepository.PrizeHitWithRankRow> hits = repository.findPrizeHitsByNumbers(1, 2, 3, 4, 5, 6);

        assertThat(hits).hasSize(2);
        assertThat(hits)
                .extracting(WinningNumberRepository.PrizeHitWithRankRow::getRound)
                .containsExactly(1101, 1100);
        assertThat(hits)
                .extracting(WinningNumberRepository.PrizeHitWithRankRow::getPrizeRank)
                .containsExactly(2, 1);
    }

    private static WinningNumberEntity entity(int round,
                                              LocalDate drawDate,
                                              int n1, int n2, int n3,
                                              int n4, int n5, int n6,
                                              int bonus) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 12, 0);
        return new WinningNumberEntity(
                round,
                drawDate,
                n1, n2, n3, n4, n5, n6,
                bonus,
                2_000_000_000L,
                10,
                90_000_000_000L,
                20_000_000_000L,
                null,
                now,
                now,
                now
        );
    }
}
