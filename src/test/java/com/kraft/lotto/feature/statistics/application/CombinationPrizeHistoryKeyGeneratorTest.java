package com.kraft.lotto.feature.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.lang.reflect.Method;
import java.util.List;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("조합 이력 캐시 키 생성기 테스트")
class CombinationPrizeHistoryKeyGeneratorTest {

    private final CombinationPrizeHistoryKeyGenerator generator = new CombinationPrizeHistoryKeyGenerator();

    @Test
    @DisplayName("파라미터가 없으면 조합 검증 예외가 발생한다")
    void generateWithNoParams() throws Exception {
        Method method = SampleTarget.class.getDeclaredMethod("target", List.class);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> generator.generate(new SampleTarget(), method))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_INVALID_NUMBER);
    }

    @Test
    @DisplayName("첫 파라미터가 List가 아니면 조합 검증 예외가 발생한다")
    void generateWithNonListParam() throws Exception {
        Method method = SampleTarget.class.getDeclaredMethod("target", List.class);

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> generator.generate(new SampleTarget(), method, "not-a-list"))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_INVALID_NUMBER);
    }

    @Test
    @DisplayName("숫자 리스트는 정렬된 하이픈 키로 생성한다")
    void generateWithNumberList() throws Exception {
        Method method = SampleTarget.class.getDeclaredMethod("target", List.class);

        Object key = generator.generate(new SampleTarget(), method, List.of(6, 1, 3, 2, 5, 4));

        assertThat(key).isEqualTo("1-2-3-4-5-6");
    }

    private static class SampleTarget {
        @SuppressWarnings("unused")
        void target(List<Integer> numbers) {
        }
    }
}
