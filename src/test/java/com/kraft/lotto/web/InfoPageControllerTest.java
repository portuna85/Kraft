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
@DisplayName("정보 페이지 컨트롤러")
class InfoPageControllerTest {

    @Mock
    LottoFetchLogQueryService fetchLogQueryService;
    @Mock
    WinningNumberQueryService winningNumberQueryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InfoPageController(
                        fetchLogQueryService, winningNumberQueryService))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("/methodology 페이지를 반환한다")
    void methodologyReturnsView() throws Exception {
        mockMvc.perform(get("/methodology"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/methodology"));
    }

    @Test
    @DisplayName("/data-source 페이지를 반환하고 changeLog 모델을 포함한다")
    void dataSourceReturnsViewWithChangeLog() throws Exception {
        when(fetchLogQueryService.recentCollectionLogs(anyInt())).thenReturn(List.of());
        when(winningNumberQueryService.findLatest()).thenReturn(Optional.empty());
        when(winningNumberQueryService.maxPossibleRound()).thenReturn(1230);

        mockMvc.perform(get("/data-source"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/data-source"))
                .andExpect(model().attributeExists("changeLog"));
    }

    @Test
    @DisplayName("/faq 페이지를 반환한다")
    void faqReturnsView() throws Exception {
        mockMvc.perform(get("/faq"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/faq"));
    }

    @Test
    @DisplayName("/responsible-play 페이지를 반환한다")
    void responsiblePlayReturnsView() throws Exception {
        mockMvc.perform(get("/responsible-play"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/responsible-play"));
    }

    @Test
    @DisplayName("/privacy 페이지를 반환한다")
    void privacyReturnsView() throws Exception {
        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/privacy"));
    }

    @Test
    @DisplayName("/terms 페이지를 반환한다")
    void termsReturnsView() throws Exception {
        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/terms"));
    }

    @Test
    @DisplayName("/contact 페이지를 반환한다")
    void contactReturnsView() throws Exception {
        mockMvc.perform(get("/contact"))
                .andExpect(status().isOk())
                .andExpect(view().name("info/contact"));
    }
}
