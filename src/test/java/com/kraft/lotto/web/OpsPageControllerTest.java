package com.kraft.lotto.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpsPageController.class)
@Import(TestCacheConfig.class)
@DisplayName("OpsPageController test")
class OpsPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LottoFetchLogQueryService fetchLogQueryService;

    @Test
    @DisplayName("입력 파라미터를 정규화해 모델에 반영한다")
    void normalizesRequestParamsAndBindsModel() throws Exception {
        when(fetchLogQueryService.failureOverview(2000, 1, "timeout", 1, 3000))
                .thenReturn(new FetchFailureOverviewDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        2000,
                        1,
                        List.of(),
                        List.of()
                ));
        when(fetchLogQueryService.listRecentFailuresPage(0, 100, "timeout", 1, 3000))
                .thenReturn(new LottoFetchLogQueryService.PagedFailures(List.of(), 0, 100, false));

        mockMvc.perform(get("/admin/ops")
                        .param("reasonLimit", "99999")
                        .param("logLimit", "0")
                        .param("page", "-1")
                        .param("pageSize", "1000")
                        .param("reason", " timeout ")
                        .param("drwNoFrom", "-2")
                        .param("drwNoTo", "9999"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-ops"))
                .andExpect(model().attribute("reasonLimit", 2000))
                .andExpect(model().attribute("logLimit", 1))
                .andExpect(model().attribute("page", 0))
                .andExpect(model().attribute("pageSize", 100))
                .andExpect(model().attribute("reason", "timeout"))
                .andExpect(model().attribute("drwNoFrom", 1))
                .andExpect(model().attribute("drwNoTo", 3000))
                .andExpect(model().attributeDoesNotExist("opsToken"));

        verify(fetchLogQueryService).failureOverview(2000, 1, "timeout", 1, 3000);
        verify(fetchLogQueryService).listRecentFailuresPage(0, 100, "timeout", 1, 3000);
    }
}
