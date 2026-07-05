package com.kraft.common.lotto;

import com.kraft.common.error.ApiException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("로또 번호 코덱 단위 테스트")
class LottoNumberCodecTest {

    private final LottoNumberCodec codec = new LottoNumberCodec();

    @Test
    @DisplayName("값이 없는 원소가 포함된 목록은 400을 반환한다")
    void normalize_nullElement_returns400() {
        List<Integer> withNull = Arrays.asList(1, 2, 3, null, 5, 6);

        assertThatThrownBy(() -> codec.normalize(withNull))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("6개가 아니면 400 반환")
    void normalize_wrongCount_returns400() {
        assertThatThrownBy(() -> codec.normalize(List.of(1, 2, 3)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("범위(1-45) 벗어난 번호는 400 반환")
    void normalize_outOfRange_returns400() {
        assertThatThrownBy(() -> codec.normalize(List.of(0, 1, 2, 3, 4, 5)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> codec.normalize(List.of(1, 2, 3, 4, 5, 46)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("중복 번호는 400 반환")
    void normalize_duplicates_returns400() {
        assertThatThrownBy(() -> codec.normalize(List.of(1, 1, 2, 3, 4, 5)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("정상 입력은 오름차순 정렬된 목록 반환")
    void normalize_validInput_returnsSorted() {
        List<Integer> result = codec.normalize(List.of(42, 3, 11, 19, 28, 34));
        assertThat(result).containsExactly(3, 11, 19, 28, 34, 42);
    }
}
