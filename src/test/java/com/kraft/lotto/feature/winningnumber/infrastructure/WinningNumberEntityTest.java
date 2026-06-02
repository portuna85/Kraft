package com.kraft.lotto.feature.winningnumber.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WinningNumberEntity rawJson 절단 테스트")
class WinningNumberEntityTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    @Test
    @DisplayName("rawJson은 4000자를 초과하면 절단된다")
    void rawJsonIsTruncatedAt4000Chars() {
        String longJson = "x".repeat(5000);

        WinningNumberEntity entity = entity(longJson);

        assertThat(entity.getRawJson()).hasSize(4000);
    }

    @Test
    @DisplayName("4000자 이하 rawJson은 그대로 유지된다")
    void rawJsonUnder4000IsKeptAsIs() {
        String json = "{\"returnValue\":\"success\"}";

        WinningNumberEntity entity = entity(json);

        assertThat(entity.getRawJson()).isEqualTo(json);
    }

    @Test
    @DisplayName("null rawJson은 null로 유지된다")
    void nullRawJsonIsPassedThrough() {
        WinningNumberEntity entity = entity(null);

        assertThat(entity.getRawJson()).isNull();
    }

    @Test
    @DisplayName("updateFrom은 secondPrize와 secondWinners도 갱신한다")
    void updateFromShouldUpdateSecondPrizeAndWinners() {
        WinningNumberEntity target = new WinningNumberEntity(
                1150, DATE,
                1, 2, 3, 4, 5, 6, 7,
                1_000_000_000L, 1,
                100_000_000_000L, 0L,
                100_000L, 2,
                "{\"a\":1}", NOW, NOW, NOW
        );
        WinningNumberEntity source = new WinningNumberEntity(
                1150, DATE,
                1, 2, 3, 4, 5, 6, 7,
                1_000_000_000L, 1,
                100_000_000_000L, 0L,
                250_000L, 5,
                "{\"b\":2}", NOW, NOW, NOW
        );

        target.updateFrom(source, NOW.plusDays(1));

        assertThat(target.getSecondPrize()).isEqualTo(250_000L);
        assertThat(target.getSecondWinners()).isEqualTo(5);
    }

    private static WinningNumberEntity entity(String rawJson) {
        return new WinningNumberEntity(
                1150, DATE,
                1, 2, 3, 4, 5, 6, 7,
                1_000_000_000L, 1,
                100_000_000_000L, 0L,
                rawJson, NOW, NOW, NOW
        );
    }
}
