package com.kraft.lotto.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
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
@DisplayName("번호 궁합 컨트롤러")
class CompanionControllerTest {

    @Mock
    WinningStatisticsService statisticsService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionController(statisticsService))
                .setViewResolvers(viewResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("/companion 페이지를 렌더링한다")
    void companionPageRendersView() throws Exception {
        mockMvc.perform(get("/companion"))
                .andExpect(status().isOk())
                .andExpect(view().name("companion"))
                .andExpect(model().attributeExists("allNumbers"));
    }

    @Test
    @DisplayName("/fragments/companion은 동반 번호 목록을 모델에 담아 반환한다")
    void companionFragmentReturnsCompanions() throws Exception {
        List<CompanionNumberDto> companions = List.of(
                new CompanionNumberDto(34, 820L, 100.0, 1),
                new CompanionNumberDto(12, 750L, 91.5, 2)
        );
        when(statisticsService.companionNumbers(7)).thenReturn(companions);

        mockMvc.perform(get("/fragments/companion").param("target", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/companion-result :: companion-result"))
                .andExpect(model().attribute("target", 7))
                .andExpect(model().attribute("companions", companions));
    }
}
