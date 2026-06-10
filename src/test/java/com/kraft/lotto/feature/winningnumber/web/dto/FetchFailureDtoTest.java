package com.kraft.lotto.feature.winningnumber.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("수집 실패 디티오 널 방어 분기")
class FetchFailureDtoTest {

    @Test
    @DisplayName("수집 실패 로그 응답 디티오 — 널 항목는 빈 리스트로 대체된다")
    void logsResponseDtoNullItemsFallsBack() {
        var dto = new FetchFailureLogsResponseDto(
                LocalDateTime.now(), 100, null, null, null, null);

        assertThat(dto.items()).isEmpty();
    }

    @Test
    @DisplayName("수집 실패 로그 응답 디티오는 널이 아닌 항목의 불변 복사본을 사용한다")
    void logsResponseDtoCopiesItems() {
        var item = new FetchFailureLogDto(1L, 1000, 500, "timeout", "msg", LocalDateTime.now());
        var dto = new FetchFailureLogsResponseDto(
                LocalDateTime.now(), 100, null, null, null, java.util.List.of(item));

        assertThat(dto.items()).hasSize(1);
    }

    @Test
    @DisplayName("수집 실패 사유 응답 디티오 — 널 항목는 빈 리스트로 대체된다")
    void reasonsResponseDtoNullItemsFallsBack() {
        var dto = new FetchFailureReasonsResponseDto(
                LocalDateTime.now(), 200, null, null, null, null);

        assertThat(dto.items()).isEmpty();
    }

    @Test
    @DisplayName("수집 실패 사유 응답 디티오는 널이 아닌 항목의 불변 복사본을 사용한다")
    void reasonsResponseDtoCopiesItems() {
        var item = new FetchFailureReasonDto("timeout", 5L);
        var dto = new FetchFailureReasonsResponseDto(
                LocalDateTime.now(), 200, null, null, null, java.util.List.of(item));

        assertThat(dto.items()).hasSize(1);
    }
}
