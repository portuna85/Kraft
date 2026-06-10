package com.kraft.lotto.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.statistics.application.WinningStatisticsService;
import com.kraft.lotto.feature.winningnumber.web.dto.CombinationPrizeHistoryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.CompanionNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.NumberFrequencyDto;
import com.kraft.lotto.feature.winningnumber.web.dto.OddEvenStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.PatternStatDto;
import com.kraft.lotto.feature.winningnumber.web.dto.SumRangeStatDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
@DisplayName("통계 API 컨트롤러")
class StatisticsApiControllerTest {

    @Mock
    WinningStatisticsService statisticsService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new StatisticsApiController(statisticsService))
                .setControllerAdvice(new PublicApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("전체 기간 빈도 조회는 45개 번호 목록을 반환한다")
    void frequencyReturnsFullList() throws Exception {
        when(statisticsService.frequency()).thenReturn(sampleFrequencies());

        mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].number").value(7))
                .andExpect(jsonPath("$.data[0].count").value(210));
    }

    @Test
    @DisplayName("period 파라미터 지정 시 최근 N회차 빈도 서비스를 호출한다")
    void frequencyWithPeriodCallsFrequencyForPeriod() throws Exception {
        when(statisticsService.frequencyForPeriod(100)).thenReturn(sampleFrequencies());

        mockMvc.perform(get("/api/v1/stats/frequency").param("period", "100"))
                .andExpect(status().isOk());

        verify(statisticsService).frequencyForPeriod(100);
    }

    @Test
    @DisplayName("패턴 조회는 홀짝·합산 통계를 반환한다")
    void patternsReturnsPatternStat() throws Exception {
        PatternStatDto dto = new PatternStatDto(
                List.of(new OddEvenStatDto(3, 3, 1000, 33.3, 1000, 31.25)),
                List.of(new SumRangeStatDto(100, 175, 450, 45.0, 1000, 38.2)),
                1000L
        );
        when(statisticsService.patternStats()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/stats/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalDraws").value(1000))
                .andExpect(jsonPath("$.data.oddEvenStats").isArray())
                .andExpect(jsonPath("$.data.sumRangeStats").isArray());
    }

    @Test
    @DisplayName("동반 출현 조회는 대상 번호와 함께 출현한 번호 목록을 반환한다")
    void companionReturnsListForTarget() throws Exception {
        List<CompanionNumberDto> companions = List.of(
                new CompanionNumberDto(34, 85, 8.5, 1),
                new CompanionNumberDto(12, 78, 7.8, 2)
        );
        when(statisticsService.companionNumbers(7)).thenReturn(companions);

        mockMvc.perform(get("/api/v1/stats/companion").param("target", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].number").value(34))
                .andExpect(jsonPath("$.data[0].rank").value(1));
    }

    @Test
    @DisplayName("target 파라미터 없이 동반 출현 조회 시 400을 반환한다")
    void companionWithoutTargetReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stats/companion"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("유효한 6개 번호로 분석 요청 시 당첨 이력을 반환한다")
    void analysisWithValidNumbersReturnsHistory() throws Exception {
        CombinationPrizeHistoryDto history = new CombinationPrizeHistoryDto(
                List.of(1, 7, 15, 23, 38, 45), 0, 1, List.of(), List.of()
        );
        when(statisticsService.combinationPrizeHistory(anyList())).thenReturn(history);

        String body = objectMapper.writeValueAsString(
                Map.of("numbers", List.of(1, 7, 15, 23, 38, 45)));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secondPrizeCount").value(1));
    }

    @Test
    @DisplayName("분석 요청 시 번호가 정렬되어 서비스에 전달된다")
    void analysisPassesSortedNumbersToService() throws Exception {
        CombinationPrizeHistoryDto history = new CombinationPrizeHistoryDto(
                List.of(1, 7, 15, 23, 38, 45), 0, 0, List.of(), List.of()
        );
        when(statisticsService.combinationPrizeHistory(List.of(1, 7, 15, 23, 38, 45)))
                .thenReturn(history);

        String body = objectMapper.writeValueAsString(
                Map.of("numbers", List.of(45, 38, 23, 15, 7, 1)));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(statisticsService).combinationPrizeHistory(List.of(1, 7, 15, 23, 38, 45));
    }

    @Test
    @DisplayName("중복 번호가 포함된 분석 요청은 400을 반환한다")
    void analysisWithDuplicateNumbersReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("numbers", List.of(1, 1, 15, 23, 38, 45)));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOTTO_INVALID_NUMBER"));
    }

    @Test
    @DisplayName("6개 미만의 번호로 분석 요청 시 400을 반환한다")
    void analysisWithTooFewNumbersReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("numbers", List.of(1, 7, 15, 23, 38)));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("범위를 벗어난 번호(46)가 포함된 분석 요청은 400을 반환한다")
    void analysisWithOutOfRangeNumberReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("numbers", List.of(1, 7, 15, 23, 38, 46)));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("요청 바디 없이 분석 요청 시 400을 반환한다")
    void analysisWithMissingBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── Cache-Control ────────────────────────────────────────────────────

    @Test
    @DisplayName("빈도 조회 응답에 Cache-Control max-age=600, public 이 포함된다")
    void frequencyResponseHasCacheControl() throws Exception {
        when(statisticsService.frequency()).thenReturn(sampleFrequencies());

        mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("패턴 조회 응답에 Cache-Control max-age=600, public 이 포함된다")
    void patternsResponseHasCacheControl() throws Exception {
        when(statisticsService.patternStats()).thenReturn(samplePatternStats());

        mockMvc.perform(get("/api/v1/stats/patterns"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("동반 출현 조회 응답에 Cache-Control max-age=600, public 이 포함된다")
    void companionResponseHasCacheControl() throws Exception {
        when(statisticsService.companionNumbers(7)).thenReturn(List.of(
                new CompanionNumberDto(34, 80L, 43.2, 1)));

        mockMvc.perform(get("/api/v1/stats/companion").param("target", "7"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("분석 POST 응답에는 Cache-Control 헤더가 없다")
    void analysisPostHasNoCacheControl() throws Exception {
        when(statisticsService.combinationPrizeHistory(anyList())).thenReturn(
                new CombinationPrizeHistoryDto(List.of(1,2,3,4,5,6), 0, 0, List.of(), List.of()));

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("numbers", List.of(1,2,3,4,5,6)))))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }

    private static List<NumberFrequencyDto> sampleFrequencies() {
        return List.of(
                new NumberFrequencyDto(7, 210L, 17.1),
                new NumberFrequencyDto(34, 185L, 15.1)
        );
    }

    private static PatternStatDto samplePatternStats() {
        return new PatternStatDto(
                List.of(new OddEvenStatDto(3, 3, 200L, 32.5, 250L, 31.3)),
                List.of(new SumRangeStatDto(100, 149, 180L, 29.2, 220L, 28.6)),
                615L
        );
    }
}
