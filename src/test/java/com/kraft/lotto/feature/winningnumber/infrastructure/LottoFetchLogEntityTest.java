package com.kraft.lotto.feature.winningnumber.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("로또 수집 로그 엔티티 절단 테스트")
class LottoFetchLogEntityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    @Test
    @DisplayName("메시지는 500자를 초과하면 절단된다")
    void messageIsTruncatedAt500Chars() {
        String longMessage = "a".repeat(600);

        LottoFetchLogEntity entity = entity(longMessage, null);

        assertThat(entity.getMessage()).hasSize(500);
    }

    @Test
    @DisplayName("원본 응답는 4000자를 초과하면 절단된다")
    void rawResponseIsTruncatedAt4000Chars() {
        String longRaw = "x".repeat(5000);

        LottoFetchLogEntity entity = entity(null, longRaw);

        assertThat(entity.getRawResponse()).hasSize(4000);
    }

    @Test
    @DisplayName("500자 이하 메시지는 그대로 유지된다")
    void messageUnder500IsKeptAsIs() {
        String msg = "short message";

        LottoFetchLogEntity entity = entity(msg, null);

        assertThat(entity.getMessage()).isEqualTo(msg);
    }

    @Test
    @DisplayName("널 값은 널로 유지된다")
    void nullValuesArePassedThrough() {
        LottoFetchLogEntity entity = entity(null, null);

        assertThat(entity.getMessage()).isNull();
        assertThat(entity.getRawResponse()).isNull();
    }

    private static LottoFetchLogEntity entity(String message, String rawResponse) {
        return new LottoFetchLogEntity(1, null, LottoFetchStatus.FAILED, message, null, rawResponse, NOW);
    }
}
