package com.kraft.lotto.feature.winningnumber.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("당첨번호 수집 이벤트 팩토리 테스트")
class WinningNumbersCollectedEventTest {

    @Test
    @DisplayName("4-인자 팩토리: collected+updated > 0이면 dataChanged=true")
    void fourArgFactoryWithCollectedSetsDataChanged() {
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(2, 0, 1, 0);

        assertThat(event.collected()).isEqualTo(2);
        assertThat(event.updated()).isZero();
        assertThat(event.skipped()).isEqualTo(1);
        assertThat(event.failed()).isZero();
        assertThat(event.dataChanged()).isTrue();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("collected+updated가 0보다 크면 dataChanged=true다")
    void dataChangedTrueWhenCollectedOrUpdatedExists() {
        assertThat(WinningNumbersCollectedEvent.of(1, 0, 0, 0).dataChanged()).isTrue();
        assertThat(WinningNumbersCollectedEvent.of(0, 1, 0, 0).dataChanged()).isTrue();
    }

    @Test
    @DisplayName("collected+updated가 0이면 dataChanged=false다")
    void dataChangedFalseWhenNoChange() {
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(0, 0, 3, 1);

        assertThat(event.dataChanged()).isFalse();
    }
}
