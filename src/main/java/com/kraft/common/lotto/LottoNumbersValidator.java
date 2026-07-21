package com.kraft.common.lotto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LottoNumbersValidator implements ConstraintValidator<LottoNumbers, List<Integer>> {

    @Override
    public boolean isValid(List<Integer> numbers, ConstraintValidatorContext context) {
        if (numbers == null || numbers.size() != 6) {
            return false;
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer n : numbers) {
            if (n == null || n < 1 || n > 45 || !seen.add(n)) {
                return false;
            }
        }
        return true;
    }
}
