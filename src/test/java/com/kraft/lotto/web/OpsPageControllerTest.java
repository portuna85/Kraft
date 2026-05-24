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
@DisplayName("운영 페이지 컨트롤러 테스트")
class OpsPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LottoFetchLogQueryService fetchLogQueryService;

    @Test
    @DisplayName("유효한 요청은 모델을 정상 바인딩한다")
    void bindsModelForValidRequest() throws Exception {
        when(fetchLogQueryService.failureOverview(200, 100, "timeout", 1, 3000))
                .thenReturn(new FetchFailureOverviewDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        200,
                        100,
                        List.of(),
                        List.of()
                ));
        when(fetchLogQueryService.listRecentFailuresPage(0, 20, "timeout", 1, 3000))
                .thenReturn(new LottoFetchLogQueryService.PagedFailures(List.of(), 0, 20, false));

        mockMvc.perform(get("/admin/ops")
                        .param("reasonLimit", "200")
                        .param("logLimit", "100")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("reason", " timeout ")
                        .param("drwNoFrom", "-2")
                        .param("drwNoTo", "9999"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-ops"))
                .andExpect(model().attribute("reasonLimit", 200))
                .andExpect(model().attribute("logLimit", 100))
                .andExpect(model().attribute("page", 0))
                .andExpect(model().attribute("pageSize", 20))
                .andExpect(model().attribute("reason", "timeout"))
                .andExpect(model().attribute("drwNoFrom", 1))
                .andExpect(model().attribute("drwNoTo", 3000))
                .andExpect(model().attributeDoesNotExist("opsToken"));

        verify(fetchLogQueryService).failureOverview(200, 100, "timeout", 1, 3000);
        verify(fetchLogQueryService).listRecentFailuresPage(0, 20, "timeout", 1, 3000);
    }

    @Test
    @DisplayName("범위를 벗어난 파라미터는 400을 반환한다")
    void invalidParamsReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/ops")
                        .param("reasonLimit", "0")
                        .param("logLimit", "2001")
                        .param("page", "-1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }
}
