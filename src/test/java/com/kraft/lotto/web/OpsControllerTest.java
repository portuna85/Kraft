package com.kraft.lotto.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.winningnumber.application.LottoCollectionCommandService;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureLogsResponseDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureReasonsResponseDto;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpsController.class)
@Import(TestCacheConfig.class)
@DisplayName("운영 도구 API 컨트롤러 테스트")
class OpsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LottoFetchLogQueryService fetchLogQueryService;

    @MockitoBean
    LottoCollectionCommandService collectionCommandService;

    @MockitoBean
    LockingTaskExecutor lockingTaskExecutor;

    @Test
    @DisplayName("failure-reasons 엔드포인트가 래핑된 응답과 정규화된 파라미터를 반환한다")
    void returnsWrappedReasonsResponse() throws Exception {
        when(fetchLogQueryService.failureReasonsResponse(2000, "timeout", 1, 3000))
                .thenReturn(new FetchFailureReasonsResponseDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        2000,
                        "timeout",
                        1,
                        3000,
                        List.of()
                ));

        mockMvc.perform(get("/ops/fetch-logs/failure-reasons")
                        .param("limit", "999999")
                        .param("reason", " timeout ")
                        .param("drwNoFrom", "-1")
                        .param("drwNoTo", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(2000))
                .andExpect(jsonPath("$.reason").value("timeout"))
                .andExpect(jsonPath("$.drwNoFrom").value(1))
                .andExpect(jsonPath("$.drwNoTo").value(3000));

        verify(fetchLogQueryService).failureReasonsResponse(2000, "timeout", 1, 3000);
    }

    @Test
    @DisplayName("failures 엔드포인트가 래핑된 응답과 정규화된 범위를 반환한다")
    void returnsWrappedFailuresResponse() throws Exception {
        when(fetchLogQueryService.failuresResponse(1, null, 10, 20))
                .thenReturn(new FetchFailureLogsResponseDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        1,
                        null,
                        10,
                        20,
                        List.of()
                ));

        mockMvc.perform(get("/ops/fetch-logs/failures")
                        .param("limit", "0")
                        .param("drwNoFrom", "20")
                        .param("drwNoTo", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.drwNoFrom").value(10))
                .andExpect(jsonPath("$.drwNoTo").value(20));

        verify(fetchLogQueryService).failuresResponse(1, null, 10, 20);
    }

    @Test
    @DisplayName("추천 통계 엔드포인트가 200 상태코드와 지표 필드들을 반환한다")
    void recommendStatsReturns200() throws Exception {
        mockMvc.perform(get("/ops/recommend/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generationCount").exists())
                .andExpect(jsonPath("$.timeoutCount").exists())
                .andExpect(jsonPath("$.failuresByReason").exists())
                .andExpect(jsonPath("$.rejectionsByRule").exists());
    }

    @Test
    @DisplayName("failure-overview가 기존 래퍼 계약을 유지한다")
    void returnsOverview() throws Exception {
        when(fetchLogQueryService.failureOverview(2000, 1, null, null, null))
                .thenReturn(new FetchFailureOverviewDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        2000,
                        1,
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(get("/ops/fetch-logs/failure-overview")
                        .param("reasonLimit", "999999")
                        .param("logLimit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasonLimit").value(2000))
                .andExpect(jsonPath("$.logLimit").value(1));

        verify(fetchLogQueryService).failureOverview(2000, 1, null, null, null);
    }
}
