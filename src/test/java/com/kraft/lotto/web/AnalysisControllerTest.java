package com.kraft.lotto.web;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.support.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("번호 조합 분석 컨트롤러")
class AnalysisControllerTest {

    @Mock
    WinningStatisticsService statisticsService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AnalysisController(statisticsService))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("분석 페이지를 렌더링한다")
    void analysisPageRendersView() throws Exception {
        mockMvc.perform(get("/analysis"))
                .andExpect(status().isOk())
                .andExpect(view().name("analysis"))
                .andExpect(model().attributeExists("allNumbers"));
    }

    @Test
    @DisplayName("6개 번호 선택 시 분석 결과를 반환한다")
    void analysisFragmentReturnsResult() throws Exception {
        List<NumberFrequencyDto> frequencies = List.of(
                new NumberFrequencyDto(1, 180L, 14.7),
                new NumberFrequencyDto(7, 210L, 17.1),
                new NumberFrequencyDto(15, 195L, 15.9),
                new NumberFrequencyDto(23, 220L, 17.9),
                new NumberFrequencyDto(38, 165L, 13.5),
                new NumberFrequencyDto(45, 155L, 12.6)
        );
        CombinationPrizeHistoryDto history =
                new CombinationPrizeHistoryDto(List.of(1, 7, 15, 23, 38, 45), 0, 0, List.of(), List.of());
        when(statisticsService.frequency()).thenReturn(frequencies);
        when(statisticsService.combinationPrizeHistory(anyList())).thenReturn(history);

        mockMvc.perform(get("/fragments/analysis")
                        .param("n1", "1").param("n2", "7").param("n3", "15")
                        .param("n4", "23").param("n5", "38").param("n6", "45"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/analysis-result :: analysis-result"))
                .andExpect(model().attributeExists("selectedBalls"))
                .andExpect(model().attributeExists("history"));
    }

    @Test
    @DisplayName("중복 번호 요청 시 오류 메시지를 반환한다")
    void analysisFragmentWithDuplicatesReturnsError() throws Exception {
        mockMvc.perform(get("/fragments/analysis")
                        .param("n1", "1").param("n2", "1").param("n3", "15")
                        .param("n4", "23").param("n5", "38").param("n6", "45"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/analysis-result :: analysis-result"))
                .andExpect(model().attributeExists("error"));
    }
}
