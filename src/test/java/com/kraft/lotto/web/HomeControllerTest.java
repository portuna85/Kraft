package com.kraft.lotto.web;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.combinationDtos;
import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumberDto;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.kraft.lotto.feature.recommend.application.RecommendFilter;
import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.FrequencySummaryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberPageDto;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("홈 컨트롤러")
class HomeControllerTest {

    @Mock
    WinningNumberQueryService queryService;

    @Mock
    RecommendService recommendService;

    @Mock
    WinningStatisticsService statisticsService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecommendModelSupport recommendModelSupport = new RecommendModelSupport(recommendService);
        FrequencyModelSupport frequencyModelSupport = new FrequencyModelSupport(statisticsService);
        RoundsModelSupport roundsModelSupport = new RoundsModelSupport(queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new HomeController(queryService),
                        new RecommendController(recommendModelSupport),
                        new FrequencyController(frequencyModelSupport),
                        new RoundsController(roundsModelSupport)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("기본 모델 값을 사용하여 홈 페이지를 렌더링한다")
    void homeWithDefaultParamsRendersView() throws Exception {
        stubHomeFrame();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("expectedRound", 1200));
    }

    @Test
    @DisplayName("빈도 프래그먼트는 빈도 뷰 모델 리스트를 모델에 담아 렌더링한다")
    void frequencyFragmentRendersView() throws Exception {
        List<NumberFrequencyDto> freqs = List.of(
                new NumberFrequencyDto(1, 3, 0.1),
                new NumberFrequencyDto(2, 7, 0.2)
        );
        CombinationPrizeHistoryDto emptyHistory =
                new CombinationPrizeHistoryDto(List.of(), 0, 0, List.of(), List.of());
        when(statisticsService.frequencySummary())
                .thenReturn(new FrequencySummaryDto(freqs, emptyHistory));
        when(statisticsService.combinationPrizeHistory(anyList())).thenReturn(emptyHistory);

        mockMvc.perform(get("/fragments/frequency"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/frequency-card :: frequency-card"))
                .andExpect(model().attribute("frequency", hasSize(2)));
    }

    @Test
    @DisplayName("기간 빈도 프래그먼트는 전체 빈도 요약을 계산하지 않는다")
    void frequencyFragmentForPeriodDoesNotLoadOverallSummary() throws Exception {
        List<NumberFrequencyDto> freqs = List.of(
                new NumberFrequencyDto(1, 3, 0.1),
                new NumberFrequencyDto(2, 7, 0.2)
        );
        CombinationPrizeHistoryDto emptyHistory =
                new CombinationPrizeHistoryDto(List.of(), 0, 0, List.of(), List.of());
        when(statisticsService.frequencyForPeriod(100)).thenReturn(freqs);
        when(statisticsService.combinationPrizeHistory(anyList())).thenReturn(emptyHistory);

        mockMvc.perform(get("/fragments/frequency").param("period", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/frequency-card :: frequency-card"))
                .andExpect(model().attribute("period", 100));

        verify(statisticsService, never()).frequencySummary();
    }

    @Test
    @DisplayName("추천 프래그먼트를 렌더링한다")
    void recommendFragmentRendersView() throws Exception {
        stubRecommend(3, 3);

        mockMvc.perform(get("/fragments/recommend").param("count", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/recommend-card :: recommend-card"))
                .andExpect(model().attribute("count", 3))
                .andExpect(model().attribute("combinations", is(combinationDtos(3))));
    }

    @Test
    @DisplayName("회차 목록 프래그먼트를 렌더링한다")
    void roundsFragmentRendersView() throws Exception {
        WinningNumberPageDto page = new WinningNumberPageDto(List.of(winningNumber(1200)), 0, 20, 1, 1);
        when(queryService.list(0, 20)).thenReturn(page);

        mockMvc.perform(get("/fragments/rounds"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/rounds-list :: rounds-list"))
                .andExpect(model().attribute("rounds", page))
                .andExpect(model().attribute("page", 0))
                .andExpect(model().attribute("size", 20));
    }

    @Test
    @DisplayName("추천 프래그먼트에서 낮은 개수 값은 그대로 1로 전달한다")
    void recommendFragmentWithLowCountCallsService() throws Exception {
        stubRecommend(1, 1);

        mockMvc.perform(get("/fragments/recommend").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("count", 1));

        verify(recommendService).recommend(anyInt(), any(RecommendFilter.class));
    }

    @Test
    @DisplayName("추천 프래그먼트에서 개수=10은 그대로 서비스에 전달한다")
    void recommendFragmentWithMaxCountCallsService() throws Exception {
        stubRecommend(10, 10);

        mockMvc.perform(get("/fragments/recommend").param("count", "10"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("count", 10));

        verify(recommendService).recommend(anyInt(), any(RecommendFilter.class));
    }

    @Test
    @DisplayName("회차가 존재할 경우 회차 검색 결과를 모델에 추가한다")
    void homeWithValidRoundShowsResult() throws Exception {
        WinningNumberDto dto = winningNumber(1100);
        stubHomeFrame();
        when(queryService.findByRound(1100)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/").param("round", "1100"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("result", dto));
    }

    @Test
    @DisplayName("존재하지 않는 회차 조회 시 에러 뷰를 렌더링한다")
    void homeWithInvalidRoundReturnsErrorView() throws Exception {
        stubHomeFrame();
        when(queryService.findByRound(200)).thenReturn(Optional.empty());

        mockMvc.perform(get("/").param("round", "200"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("result", (Object) null));
    }

    @Test
    @DisplayName("숫자가 아닌 회차 값에 대해 400 잘못된 요청를 반환한다")
    void nonNumericRoundReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/").param("round", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    @DisplayName("회차 검색 결과와 최신 회차 정보를 모델에 포함한다")
    void homeWithRoundShowsResultAndExpectedRound() throws Exception {
        WinningNumberDto dto = winningNumber(500);
        stubHomeFrame();
        when(queryService.findByRound(500)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/").param("round", "500"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("expectedRound", 1200))
                .andExpect(model().attribute("result", dto));
    }

    private void stubRecommend(int requestedCount, int returnedCount) {
        when(recommendService.recommend(anyInt(), any(RecommendFilter.class)))
                .thenReturn(new RecommendResponse(combinationDtos(returnedCount)));
        when(recommendService.rules()).thenReturn(List.of(new RuleDto("BirthdayBiasRule", "must include a number above 31")));
    }

    private void stubHomeFrame() {
        when(queryService.expectedCurrentRound()).thenReturn(1200);
        when(queryService.findLatest()).thenReturn(Optional.empty());
    }

    private static WinningNumberDto winningNumber(int round) {
        return winningNumberDto(round);
    }
}
