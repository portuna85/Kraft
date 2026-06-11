package com.kraft.lotto.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.OddEvenStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.SumRangeStatDto;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicPageController.class)
@Import(TestCacheConfig.class)
@DisplayName("public page controller")
class PublicPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    WinningStatisticsService statisticsService;

    @Test
    @DisplayName("renders stats page with server-side model")
    void rendersStatsPage() throws Exception {
        PatternStatDto stats = new PatternStatDto(
                List.of(new OddEvenStatDto(3, 3, 120, 21.5, 120, 31.2)),
                List.of(new SumRangeStatDto(100, 109, 80, 14.3, 120, 12.8)),
                558
        );
        when(statisticsService.patternStats()).thenReturn(stats);

        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("stats"))
                .andExpect(model().attribute("stats", stats))
                .andExpect(content().string(Matchers.containsString("data-testid=\"stats-page\"")));

        verify(statisticsService).patternStats();
    }
}
