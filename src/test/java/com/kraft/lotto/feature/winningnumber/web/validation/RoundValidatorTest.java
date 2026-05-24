package com.kraft.lotto.feature.winningnumber.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("회차 검증기 테스트")
class RoundValidatorTest {

    @Test
    @DisplayName("allowNull=true면 null 값을 허용한다")
    void allowsNullWhenConfigured() {
        RoundValidator validator = new RoundValidator();
        validator.initialize(annotation(1, 3000, true));

        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    @DisplayName("allowNull=false면 null 값을 거부한다")
    void rejectsNullByDefault() {
        RoundValidator validator = new RoundValidator();
        validator.initialize(annotation(1, 3000, false));

        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    @DisplayName("숫자 형식이 아니면 거부한다")
    void rejectsNonDigitText() {
        RoundValidator validator = new RoundValidator();
        validator.initialize(annotation(1, 3000, false));

        assertThat(validator.isValid("abc", null)).isFalse();
        assertThat(validator.isValid("12a", null)).isFalse();
        assertThat(validator.isValid("1234567", null)).isFalse();
    }

    @Test
    @DisplayName("범위 안의 숫자는 허용하고 범위 밖은 거부한다")
    void validatesNumericRange() {
        RoundValidator validator = new RoundValidator();
        validator.initialize(annotation(10, 20, false));

        assertThat(validator.isValid("10", null)).isTrue();
        assertThat(validator.isValid("20", null)).isTrue();
        assertThat(validator.isValid("9", null)).isFalse();
        assertThat(validator.isValid("21", null)).isFalse();
    }

    private static ValidRound annotation(int min, int max, boolean allowNull) {
        return new ValidRound() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidRound.class;
            }

            @Override
            public String message() {
                return "test";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return (Class<? extends jakarta.validation.Payload>[]) new Class<?>[0];
            }

            @Override
            public int min() {
                return min;
            }

            @Override
            public int max() {
                return max;
            }

            @Override
            public boolean allowNull() {
                return allowNull;
            }
        };
    }
}
