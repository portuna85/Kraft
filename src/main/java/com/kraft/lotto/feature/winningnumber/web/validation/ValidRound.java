package com.kraft.lotto.feature.winningnumber.web.validation;

import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RoundValidator.class)
public @interface ValidRound {

    String message() default "round는 숫자 형식이며 유효한 회차 범위여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default LottoRoundPolicy.MIN_ROUND;

    int max() default LottoRoundPolicy.MAX_ROUND;

    boolean allowNull() default false;
}
