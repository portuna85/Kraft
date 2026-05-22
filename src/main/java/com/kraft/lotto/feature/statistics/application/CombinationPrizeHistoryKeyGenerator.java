package com.kraft.lotto.feature.statistics.application;

import java.lang.reflect.Method;
import java.util.List;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

@Component("combinationPrizeHistoryKeyGenerator")
class CombinationPrizeHistoryKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        List<Integer> numbers = params.length == 0 ? List.of() : castNumbers(params[0]);
        return WinningStatisticsCacheService.combinationPrizeHistoryCacheKey(numbers);
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> castNumbers(Object value) {
        return value instanceof List<?> ? (List<Integer>) value : List.of();
    }
}
