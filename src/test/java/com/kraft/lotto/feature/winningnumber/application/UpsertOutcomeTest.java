package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("업서트 결과 변경 여부")
class UpsertOutcomeTest {

    @ParameterizedTest
    @EnumSource(value = UpsertOutcome.class, names = {"INSERTED", "UPDATED"})
    @DisplayName("삽입 또는 수정 결과는 변경됨을 반환한다")
    void insertedAndUpdatedReturnTrue(UpsertOutcome outcome) {
        assertThat(outcome.dataChanged()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = UpsertOutcome.class, names = {"UNCHANGED", "FAILED"})
    @DisplayName("변경 없음 또는 실패 결과는 변경되지 않음을 반환한다")
    void unchangedAndFailedReturnFalse(UpsertOutcome outcome) {
        assertThat(outcome.dataChanged()).isFalse();
    }
}
