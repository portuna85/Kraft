package com.kraft.lotto.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.support.GlobalExceptionHandler;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("정보 페이지 컨트롤러")
class InfoPageControllerTest {

    @Mock
    LottoFetchLogQueryService fetchLogQueryService;
    @Mock
    WinningNumberQueryService winningNumberQueryService;
    @Mock
    ObjectProvider<BuildProperties> buildPropertiesProvider;
    @Mock
    Environment environment;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InfoPageController(
                        fetchLogQueryService, winningNumberQueryService, buildPropertiesProvider, environment))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("방법론 페이지를 반환한다")
    void methodologyReturnsView() throws Exception {
        mockMvc.perform(get("/methodology"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/methodology"));
    }

    @Test
    @DisplayName("데이터 출처 페이지를 반환하고 변경 로그 모델을 포함한다")
    void dataSourceReturnsViewWithChangeLog() throws Exception {
        when(fetchLogQueryService.recentCollectionLogs(anyInt())).thenReturn(List.of());
        when(winningNumberQueryService.findLatest()).thenReturn(Optional.empty());
        when(winningNumberQueryService.expectedCurrentRound()).thenReturn(1227);
        when(winningNumberQueryService.maxPossibleRound()).thenReturn(1237);
        when(environment.getProperty("KRAFT_BUILD_COMMIT")).thenReturn(null);
        when(environment.getProperty("KRAFT_APP_IMAGE_TAG")).thenReturn("abc1234");
        Properties entries = new Properties();
        entries.setProperty("version", "0.2.0");
        entries.setProperty("time", "2026-06-08T00:00:00Z");
        when(buildPropertiesProvider.getIfAvailable()).thenReturn(new BuildProperties(entries));

        mockMvc.perform(get("/data-source"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/data-source"))
                .andExpect(model().attributeExists("changeLog"))
                .andExpect(model().attribute("expectedRound", 1227))
                .andExpect(model().attribute("maxSearchRound", 1237))
                .andExpect(model().attribute("appVersion", "0.2.0"))
                .andExpect(model().attribute("buildTimeText", "2026-06-08T00:00:00Z"))
                .andExpect(model().attribute("buildCommit", "abc1234"));
    }

    @Test
    @DisplayName("자주 묻는 질문 페이지를 반환한다")
    void faqReturnsView() throws Exception {
        mockMvc.perform(get("/faq"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/faq"));
    }

    @Test
    @DisplayName("책임 이용 안내 페이지를 반환한다")
    void responsiblePlayReturnsView() throws Exception {
        mockMvc.perform(get("/responsible-play"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/responsible-play"));
    }

    @Test
    @DisplayName("개인정보 처리방침 페이지를 반환한다")
    void privacyReturnsView() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/privacy"));
    }

    @Test
    @DisplayName("이용약관 페이지를 반환한다")
    void termsReturnsView() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/terms"));
    }

    @Test
    @DisplayName("문의 페이지를 반환한다")
    void contactReturnsView() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/contact"));
    }
}
