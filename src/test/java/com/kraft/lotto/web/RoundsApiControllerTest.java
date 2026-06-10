package com.kraft.lotto.web;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumberDto;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberPageDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("회차 API 컨트롤러")
class RoundsApiControllerTest {

    @Mock
    WinningNumberQueryService queryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RoundsApiController(queryService))
                .setControllerAdvice(new PublicApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("최신 회차 조회는 success=true와 회차 데이터를 반환한다")
    void latestReturnsSuccessWithRoundData() throws Exception {
        when(queryService.getLatest()).thenReturn(winningNumberDto(1200));

        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.round").value(1200))
                .andExpect(jsonPath("$.data.numbers").isArray())
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("당첨번호가 없을 때 최신 회차 조회는 404를 반환한다")
    void latestWhenNonePersistedReturns404() throws Exception {
        when(queryService.getLatest())
                .thenThrow(new BusinessException(ErrorCode.WINNING_NUMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WINNING_NUMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("특정 회차 조회는 해당 회차 데이터를 반환한다")
    void byRoundReturnsMatchingRoundData() throws Exception {
        when(queryService.getByRound(1200)).thenReturn(winningNumberDto(1200));

        mockMvc.perform(get("/api/v1/rounds/1200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.round").value(1200));
    }

    @Test
    @DisplayName("존재하지 않는 회차 조회는 404를 반환한다")
    void byRoundWhenNotFoundReturns404() throws Exception {
        when(queryService.getByRound(999))
                .thenThrow(new BusinessException(ErrorCode.WINNING_NUMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rounds/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("유효하지 않은 회차 범위는 서비스 예외를 400으로 변환한다")
    void byRoundWithInvalidRangeReturns400() throws Exception {
        when(queryService.getByRound(0))
                .thenThrow(new BusinessException(ErrorCode.LOTTO_INVALID_TARGET_ROUND));

        mockMvc.perform(get("/api/v1/rounds/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LOTTO_INVALID_TARGET_ROUND"));
    }

    @Test
    @DisplayName("숫자가 아닌 회차 값은 400을 반환한다")
    void byRoundWithNonNumericValueReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("회차 목록 조회는 페이지네이션 응답을 반환한다")
    void listReturnsPaginatedResponse() throws Exception {
        var pageDto = new WinningNumberPageDto(List.of(winningNumberDto(1200)), 0, 20, 1L, 1);
        when(queryService.list(0, 20)).thenReturn(pageDto);

        mockMvc.perform(get("/api/v1/rounds").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].round").value(1200))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("page 파라미터 미전달 시 기본값 0으로 서비스를 호출한다")
    void listUsesDefaultPageParams() throws Exception {
        var pageDto = new WinningNumberPageDto(List.of(), 0, 20, 0L, 0);
        when(queryService.list(0, 20)).thenReturn(pageDto);

        mockMvc.perform(get("/api/v1/rounds"))
                .andExpect(status().isOk());

        verify(queryService).list(0, 20);
    }

    @Test
    @DisplayName("회차 목록 조회에서 size가 숫자가 아니면 400을 반환한다")
    void listWithNonNumericSizeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/rounds").param("size", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ── Cache-Control ────────────────────────────────────────────────────

    @Test
    @DisplayName("최신 회차 조회 응답에 Cache-Control max-age=300, public 이 포함된다")
    void latestRoundResponseHasShortCacheControl() throws Exception {
        when(queryService.getLatest()).thenReturn(winningNumberDto(1200));

        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("회차 목록 조회 응답에 Cache-Control max-age=300, public 이 포함된다")
    void listRoundsResponseHasShortCacheControl() throws Exception {
        var pageDto = new WinningNumberPageDto(List.of(winningNumberDto(1200)), 0, 20, 1L, 1);
        when(queryService.list(0, 20)).thenReturn(pageDto);

        mockMvc.perform(get("/api/v1/rounds"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("특정 회차 조회 응답에 Cache-Control max-age=86400, public 이 포함된다")
    void byRoundResponseHasLongCacheControl() throws Exception {
        when(queryService.getByRound(1200)).thenReturn(winningNumberDto(1200));

        mockMvc.perform(get("/api/v1/rounds/1200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }
}
