package com.kraft.lotto.web;

import static com.kraft.lotto.support.fixtures.LottoTestFixtures.combinationDtos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.lotto.feature.recommend.application.RecommendFilter;
import com.kraft.lotto.feature.recommend.application.RecommendService;
import com.kraft.lotto.feature.recommend.web.dto.RecommendResponse;
import com.kraft.lotto.feature.recommend.web.dto.RuleDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
@DisplayName("번호 추천 API 컨트롤러")
class NumbersApiControllerTest {

    @Mock
    RecommendService recommendService;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new NumbersApiController(recommendService))
                .setControllerAdvice(new PublicApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("count=5 추천 요청은 5개 조합을 반환한다")
    void recommendWithCount5ReturnsFiveCombinations() throws Exception {
        when(recommendService.recommend(eq(5), any(RecommendFilter.class)))
                .thenReturn(new RecommendResponse(combinationDtos(5)));

        String body = objectMapper.writeValueAsString(Map.of("count", 5));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.combinations").isArray())
                .andExpect(jsonPath("$.data.combinations.length()").value(5));
    }

    @Test
    @DisplayName("필터(oddCount·sumMin·sumMax)가 서비스에 그대로 전달된다")
    void recommendWithFilterPassesFilterToService() throws Exception {
        when(recommendService.recommend(eq(3), any(RecommendFilter.class)))
                .thenReturn(new RecommendResponse(combinationDtos(3)));

        String body = objectMapper.writeValueAsString(
                Map.of("count", 3, "oddCount", 3, "sumMin", 100, "sumMax", 180));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<RecommendFilter> captor = ArgumentCaptor.forClass(RecommendFilter.class);
        verify(recommendService).recommend(eq(3), captor.capture());
        RecommendFilter captured = captor.getValue();
        assertThat(captured.oddCount()).isEqualTo(3);
        assertThat(captured.sumMin()).isEqualTo(100);
        assertThat(captured.sumMax()).isEqualTo(180);
    }

    @Test
    @DisplayName("비활성화 규칙 목록이 서비스에 전달된다")
    void recommendWithDisabledRulesPassesThemToService() throws Exception {
        when(recommendService.recommend(eq(1), any(RecommendFilter.class)))
                .thenReturn(new RecommendResponse(combinationDtos(1)));

        String body = objectMapper.writeValueAsString(
                Map.of("count", 1, "disabledRules", List.of("BirthdayBiasRule")));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<RecommendFilter> captor = ArgumentCaptor.forClass(RecommendFilter.class);
        verify(recommendService).recommend(eq(1), captor.capture());
        assertThat(captor.getValue().isRuleDisabled("BirthdayBiasRule")).isTrue();
    }

    @Test
    @DisplayName("count=0 요청은 400을 반환한다")
    void recommendWithCountZeroReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("count", 0));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("count=11 요청은 400을 반환한다")
    void recommendWithCountAboveMaxReturnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("count", 11));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("요청 바디 없이 추천 요청 시 400을 반환한다")
    void recommendWithMissingBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조합 생성 타임아웃 시 503을 반환한다")
    void recommendWhenGenerationTimesOutReturns503() throws Exception {
        when(recommendService.recommend(eq(5), any(RecommendFilter.class)))
                .thenThrow(new BusinessException(ErrorCode.LOTTO_GENERATION_TIMEOUT, "timeout"));

        String body = objectMapper.writeValueAsString(Map.of("count", 5));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("LOTTO_GENERATION_TIMEOUT"))
                .andExpect(jsonPath("$.error.retryable").value(true));
    }

    @Test
    @DisplayName("규칙 목록 조회는 활성 규칙 이름과 설명을 반환한다")
    void rulesReturnsActiveRuleList() throws Exception {
        List<RuleDto> rules = List.of(
                new RuleDto("BirthdayBiasRule", "31 이하 번호 편중 제외"),
                new RuleDto("AllOddRule", "전체 홀수 조합 제외")
        );
        when(recommendService.rules()).thenReturn(rules);

        mockMvc.perform(get("/api/v1/numbers/recommend/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("BirthdayBiasRule"));
    }

    @Test
    @DisplayName("규칙 목록 조회에 POST를 사용하면 405를 반환한다")
    void rulesWithPostMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/v1/numbers/recommend/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ── Cache-Control ────────────────────────────────────────────────────

    @Test
    @DisplayName("규칙 목록 조회 응답에 Cache-Control max-age=3600, public 이 포함된다")
    void rulesResponseHasOneHourCacheControl() throws Exception {
        when(recommendService.rules()).thenReturn(List.of(
                new RuleDto("BirthdayBiasRule", "생일 편향 제외")));

        mockMvc.perform(get("/api/v1/numbers/recommend/rules"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")))
                .andExpect(header().string("Cache-Control", containsString("public")));
    }

    @Test
    @DisplayName("번호 추천 POST 응답에는 Cache-Control 헤더가 없다")
    void recommendPostHasNoCacheControl() throws Exception {
        when(recommendService.recommend(eq(1), any(RecommendFilter.class)))
                .thenReturn(new RecommendResponse(combinationDtos(1)));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("count", 1))))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Cache-Control"));
    }
}
