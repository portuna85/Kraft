package com.kraft.lotto.feature.winningnumber.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("당첨번호 수집 이벤트 팩토리 테스트")
class WinningNumbersCollectedEventTest {

    @Test
    @DisplayName("4-인자 팩토리: 수집 수+수정 수 > 0이면 데이터 변경 여부=참")
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
    @DisplayName("수집 수+수정 수가 0보다 크면 데이터 변경 여부=참다")
    void dataChangedTrueWhenCollectedOrUpdatedExists() {
        assertThat(WinningNumbersCollectedEvent.of(1, 0, 0, 0).dataChanged()).isTrue();
        assertThat(WinningNumbersCollectedEvent.of(0, 1, 0, 0).dataChanged()).isTrue();
    }

    @Test
    @DisplayName("수집 수+수정 수가 0이면 데이터 변경 여부=거짓다")
    void dataChangedFalseWhenNoChange() {
        WinningNumbersCollectedEvent event = WinningNumbersCollectedEvent.of(0, 0, 3, 1);

        assertThat(event.dataChanged()).isFalse();
    }
}
