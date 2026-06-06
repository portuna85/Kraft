package com.kraft.lotto.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.TestCacheConfig;
import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchFailureOverviewDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FetchLogRetentionStatusDto;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpsPageController.class)
@Import({TestCacheConfig.class, OpsPageControllerTest.CollectPropertiesConfig.class})
@ImportAutoConfiguration(exclude = {OAuth2ClientWebSecurityAutoConfiguration.class})
@DisplayName("운영 페이지 컨트롤러 테스트")
class OpsPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LottoFetchLogQueryService fetchLogQueryService;

    @TestConfiguration
    static class CollectPropertiesConfig {
        @Bean
        KraftCollectProperties kraftCollectProperties() {
            return new KraftCollectProperties(
                    52,
                    1500,
                    true,
                    new KraftCollectProperties.Auto(true, "Asia/Seoul"),
                    new KraftCollectProperties.LogRetention(true, 90, 1000, "0 30 3 * * *")
            );
        }
    }

    @Test
    @WithMockUser
    @DisplayName("유효한 요청은 모델을 정상 바인딩한다")
    void bindsModelForValidRequest() throws Exception {
        int expectedTo = com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy.maxPossibleRound(java.time.LocalDate.now());

        when(fetchLogQueryService.failureOverview(200, 100, "timeout", 1, expectedTo))
                .thenReturn(new FetchFailureOverviewDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        200,
                        100,
                        List.of(),
                        List.of()
                ));
        when(fetchLogQueryService.listRecentFailuresPage(0, 20, "timeout", 1, expectedTo))
                .thenReturn(new LottoFetchLogQueryService.PagedFailures(List.of(), 0, 20, false));
        when(fetchLogQueryService.retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul"))
                .thenReturn(new FetchLogRetentionStatusDto(
                        LocalDateTime.of(2026, 5, 24, 12, 0),
                        true,
                        90,
                        1000,
                        "0 30 3 * * *",
                        "Asia/Seoul",
                        LocalDateTime.of(2026, 2, 24, 12, 0),
                        100L,
                        10L,
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 5, 24, 11, 59)
                ));

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
                .andExpect(model().attribute("drwNoTo", expectedTo))
                .andExpect(model().attributeDoesNotExist("opsToken"));

        verify(fetchLogQueryService).failureOverview(200, 100, "timeout", 1, expectedTo);
        verify(fetchLogQueryService).listRecentFailuresPage(0, 20, "timeout", 1, expectedTo);
        verify(fetchLogQueryService).retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul");
    }

    @Test
    @WithMockUser
    @DisplayName("범위를 벗어난 파라미터는 400을 반환한다")
    void invalidParamsReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/ops")
                        .param("reasonLimit", "0")
                        .param("logLimit", "2001")
                        .param("page", "-1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("운영 페이지는 빠른 필터와 초기화 버튼을 렌더링한다")
    void rendersQuickReasonFiltersAndReset() throws Exception {
        when(fetchLogQueryService.failureOverview(200, 100, null, null, null))
                .thenReturn(new FetchFailureOverviewDto(
                        LocalDateTime.of(2026, 5, 22, 12, 0),
                        200,
                        100,
                        List.of(),
                        List.of()
                ));
        when(fetchLogQueryService.listRecentFailuresPage(0, 20, null, null, null))
                .thenReturn(new LottoFetchLogQueryService.PagedFailures(List.of(), 0, 20, false));
        when(fetchLogQueryService.retentionStatus(true, 90, 1000, "0 30 3 * * *", "Asia/Seoul"))
                .thenReturn(new FetchLogRetentionStatusDto(
                        LocalDateTime.of(2026, 5, 24, 12, 0),
                        true,
                        90,
                        1000,
                        "0 30 3 * * *",
                        "Asia/Seoul",
                        LocalDateTime.of(2026, 2, 24, 12, 0),
                        100L,
                        10L,
                        LocalDateTime.of(2025, 1, 1, 0, 0),
                        LocalDateTime.of(2026, 5, 24, 11, 59)
                ));

        mockMvc.perform(get("/admin/ops"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("빠른 사유 필터")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-reason=\"timeout\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">초기화<")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("로그 보관 정책")));
    }
}
