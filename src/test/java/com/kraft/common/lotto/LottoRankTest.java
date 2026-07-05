package com.kraft.common.lotto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("로또 등수 테스트")
class LottoRankTest {

    @Test
    @DisplayName("6개 일치는 1등")
    void sixMatches_isFirstPrize() {
        assertThat(LottoRank.of(6, false)).isEqualTo("1등");
        assertThat(LottoRank.of(6, true)).isEqualTo("1등");
    }

    @Test
    @DisplayName("5개 일치 + 보너스 일치는 2등")
    void fiveMatchesWithBonus_isSecondPrize() {
        assertThat(LottoRank.of(5, true)).isEqualTo("2등");
    }

    @Test
    @DisplayName("5개 일치 + 보너스 불일치는 3등")
    void fiveMatchesWithoutBonus_isThirdPrize() {
        assertThat(LottoRank.of(5, false)).isEqualTo("3등");
    }

    @Test
    @DisplayName("4개 일치는 4등")
    void fourMatches_isFourthPrize() {
        assertThat(LottoRank.of(4, true)).isEqualTo("4등");
        assertThat(LottoRank.of(4, false)).isEqualTo("4등");
    }

    @Test
    @DisplayName("3개 일치는 5등")
    void threeMatches_isFifthPrize() {
        assertThat(LottoRank.of(3, true)).isEqualTo("5등");
        assertThat(LottoRank.of(3, false)).isEqualTo("5등");
    }

    @Test
    @DisplayName("2개 이하 일치는 낙첨")
    void twoOrFewerMatches_isNoPrize() {
        assertThat(LottoRank.of(2, true)).isEqualTo("낙첨");
        assertThat(LottoRank.of(1, false)).isEqualTo("낙첨");
        assertThat(LottoRank.of(0, false)).isEqualTo("낙첨");
    }
}
