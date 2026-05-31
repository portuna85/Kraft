package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("UpsertOutcome.dataChanged()")
class UpsertOutcomeTest {

    @ParameterizedTest
    @EnumSource(value = UpsertOutcome.class, names = {"INSERTED", "UPDATED"})
    @DisplayName("INSERTED/UPDATED는 dataChanged=true를 반환한다")
    void insertedAndUpdatedReturnTrue(UpsertOutcome outcome) {
        assertThat(outcome.dataChanged()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = UpsertOutcome.class, names = {"UNCHANGED", "FAILED"})
    @DisplayName("UNCHANGED/FAILED는 dataChanged=false를 반환한다")
    void unchangedAndFailedReturnFalse(UpsertOutcome outcome) {
        assertThat(outcome.dataChanged()).isFalse();
    }
}
