package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("통계 API 컨트롤러 테스트")
class StatisticsApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @Autowired
    private FrequencySummaryRepository frequencySummaryRepository;

    @Autowired
    private PatternStatsSummaryRepository patternStatsSummaryRepository;

    @Autowired
    private CompanionPairSummaryRepository companionPairSummaryRepository;

    @Autowired
    private StatisticsSummaryRebuilder summaryRebuilder;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        frequencySummaryRepository.deleteAll();
        patternStatsSummaryRepository.deleteAll();
        companionPairSummaryRepository.deleteAll();
        winningNumberRepository.deleteAll();

        winningNumberRepository.save(new WinningNumber(1, LocalDate.of(2026, 1, 4),
                1, 2, 3, 4, 5, 6, 7,
                1_000_000_000L, 0L, 0, 0L, 0L, null,
                OffsetDateTime.now(Clock.system(KST))));
        winningNumberRepository.save(new WinningNumber(2, LocalDate.of(2026, 1, 11),
                10, 20, 30, 40, 41, 42, 43,
                2_000_000_000L, 0L, 0, 0L, 0L, null,
                OffsetDateTime.now(Clock.system(KST))));
    }

    @Test
    @DisplayName("번호별 출현 빈도 조회 시 비어있으면 재생성하고 200을 반환하는지 확인")
    void getFrequency_returns200_andTriggersRebuildWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRounds").value(2))
                .andExpect(jsonPath("$.frequencies", hasSize(45)));
    }

    @Test
    @DisplayName("패턴 통계 조회 시 200을 반환하는지 확인")
    void getPatterns_returns200() throws Exception {
        summaryRebuilder.rebuildAllSummaries();
        mockMvc.perform(get("/api/v1/stats/patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRounds").value(2));
    }

    @Test
    @DisplayName("동반 출연 번호 조회 시 200을 반환하는지 확인")
    void getCompanion_returns200() throws Exception {
        summaryRebuilder.rebuildAllSummaries();
        mockMvc.perform(get("/api/v1/stats/companion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRounds").value(2));
    }

    @Test
    @DisplayName("유효한 입력에 대해 번호 분석 결과가 올바른지 확인")
    void analysis_returnsCorrectMetrics_forValidInput() throws Exception {
        String body = "{\"numbers\":[1,2,3,4,5,6]}";

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oddCount").value(3))
                .andExpect(jsonPath("$.evenCount").value(3))
                .andExpect(jsonPath("$.sumOfNumbers").value(21))
                .andExpect(jsonPath("$.sumBucket").value("21-65"))
                .andExpect(jsonPath("$.consecutivePairCount").value(5))
                .andExpect(jsonPath("$.lowCount").value(6))
                .andExpect(jsonPath("$.highCount").value(0));
    }

    @Test
    @DisplayName("범위를 벗어난 번호 입력 시 400 에러를 반환하는지 확인")
    void analysis_returns400_forNumberOutOfRange() throws Exception {
        String body = "{\"numbers\":[1,2,3,4,5,46]}";

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_NUMBER"));
    }

    @Test
    @DisplayName("중복된 번호 입력 시 400 에러를 반환하는지 확인")
    void analysis_returns400_forDuplicateNumbers() throws Exception {
        String body = "{\"numbers\":[1,2,3,4,5,5]}";

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_NUMBER"));
    }

    @Test
    @DisplayName("잘못된 번호 개수 입력 시 400 에러를 반환하는지 확인")
    void analysis_returns400_forWrongCount() throws Exception {
        String body = "{\"numbers\":[1,2,3,4,5]}";

        mockMvc.perform(post("/api/v1/stats/analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
