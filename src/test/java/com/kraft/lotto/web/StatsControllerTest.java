package com.kraft.lotto.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.OddEvenStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.SumRangeStatDto;
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
@DisplayName("패턴 통계 컨트롤러")
class StatsControllerTest {

    @Mock
    WinningStatisticsService statisticsService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StatsController(statisticsService))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("통계 페이지를 렌더링한다")
    void statsPageRendersView() throws Exception {
        when(statisticsService.patternStats()).thenReturn(patternStatDto());

        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("stats"))
                .andExpect(model().attributeExists("stats"));
    }

    @Test
    @DisplayName("통계 프래그먼트를 반환한다")
    void statsFragmentReturnsFragment() throws Exception {
        when(statisticsService.patternStats()).thenReturn(patternStatDto());

        mockMvc.perform(get("/fragments/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/stats-card :: stats-card"));
    }

    private static PatternStatDto patternStatDto() {
        OddEvenStatDto oddEven = new OddEvenStatDto(3, 3, 410L, 33.4, 410L, 33.5);
        SumRangeStatDto sumRange = new SumRangeStatDto(130, 139, 160L, 13.0, 160L, 12.8);
        return new PatternStatDto(List.of(oddEven), List.of(sumRange), 1226L);
    }
}
