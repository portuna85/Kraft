package com.kraft.common.lotto;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("로또 번호 검증기 테스트")
class LottoNumbersValidatorTest {

    private final LottoNumbersValidator validator = new LottoNumbersValidator();

    @Test
    @DisplayName("1~45 사이 중복 없는 6개는 유효하다")
    void valid_sixDistinctNumbersInRange() {
        assertThat(validator.isValid(List.of(1, 2, 3, 4, 5, 6), null)).isTrue();
        assertThat(validator.isValid(List.of(1, 15, 22, 33, 40, 45), null)).isTrue();
    }

    @Test
    @DisplayName("null은 무효하다")
    void invalid_null() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("6개가 아니면 무효하다")
    void invalid_wrongCount() {
        assertThat(validator.isValid(List.of(1, 2, 3, 4, 5), null)).isFalse();
        assertThat(validator.isValid(List.of(1, 2, 3, 4, 5, 6, 7), null)).isFalse();
        assertThat(validator.isValid(List.of(), null)).isFalse();
    }

    @Test
    @DisplayName("범위를 벗어난 번호가 있으면 무효하다")
    void invalid_outOfRange() {
        assertThat(validator.isValid(List.of(0, 2, 3, 4, 5, 6), null)).isFalse();
        assertThat(validator.isValid(List.of(1, 2, 3, 4, 5, 46), null)).isFalse();
        assertThat(validator.isValid(Arrays.asList(1, 2, 3, 4, 5, -1), null)).isFalse();
    }

    @Test
    @DisplayName("중복 번호가 있으면 무효하다")
    void invalid_duplicate() {
        assertThat(validator.isValid(List.of(1, 2, 3, 4, 5, 5), null)).isFalse();
    }

    @Test
    @DisplayName("null 원소가 포함되면 무효하다")
    void invalid_nullElement() {
        assertThat(validator.isValid(Arrays.asList(1, 2, 3, 4, 5, null), null)).isFalse();
    }
}
