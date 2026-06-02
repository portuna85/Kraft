package com.kraft.lotto.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningStoreQueryService;
import com.kraft.lotto.support.GlobalExceptionHandler;
import com.kraft.lotto.support.fixtures.LottoTestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("최신 회차 컨트롤러")
class LatestRoundControllerTest {

    @Mock
    WinningNumberQueryService queryService;

    @Mock
    WinningStoreQueryService storeQueryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LatestRoundController(queryService, storeQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    @DisplayName("최신 회차가 있으면 latest 뷰와 모델을 반환한다")
    void latestPageWithData() throws Exception {
        when(queryService.findLatest()).thenReturn(Optional.of(LottoTestFixtures.winningNumberDto(1200)));
        when(storeQueryService.findByRoundAndGrade(1200, 1)).thenReturn(List.of());
        when(storeQueryService.findByRoundAndGrade(1200, 2)).thenReturn(List.of());
        when(storeQueryService.hasStores(1200)).thenReturn(false);

        mockMvc.perform(get("/latest"))
                .andExpect(status().isOk())
                .andExpect(view().name("latest"))
                .andExpect(model().attributeExists("latest", "firstAfterTax", "secondAfterTax", "stores1", "stores2"));
    }

    @Test
    @DisplayName("최신 회차가 없으면 latest 뷰를 빈 모델로 반환한다")
    void latestPageWithNoData() throws Exception {
        when(queryService.findLatest()).thenReturn(Optional.empty());

        mockMvc.perform(get("/latest"))
                .andExpect(status().isOk())
                .andExpect(view().name("latest"))
                .andExpect(model().attributeDoesNotExist("latest"));
    }

    @ParameterizedTest(name = "prize={0} → afterTax={1}")
    @CsvSource({
        "0, 0",
        "-1, 0",
        "1000000, 1000000",
        "2000000, 2000000",
        "50000000, 39000000",
        "300000000, 234000000",
        "300000001, 201000000",
        "2052166154, 1374951323"
    })
    @DisplayName("세후 금액을 세율에 따라 계산한다")
    void afterTaxCalculation(long prize, long expectedAfterTax) {
        assertThat(LottoPrizeTaxCalculator.afterTax(prize)).isEqualTo(expectedAfterTax);
    }
}
