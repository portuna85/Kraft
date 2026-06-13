package com.kraft;

import com.kraft.operationlog.WinningNumberOperationLogRepository;
import com.kraft.saved.SavedNumberRepository;
import com.kraft.winningnumber.ExternalWinningNumberFetchClient;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API 통합 테스트")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @Autowired
    private SavedNumberRepository savedNumberRepository;

    @Autowired
    private WinningNumberOperationLogRepository winningNumberOperationLogRepository;

    @BeforeEach
    void setUp() {
        winningNumberOperationLogRepository.deleteAll();
        savedNumberRepository.deleteAll();
        winningNumberRepository.deleteAll();
        winningNumberRepository.save(new WinningNumber(
                1200,
                LocalDate.of(2026, 6, 13),
                3, 11, 19, 28, 34, 42,
                7,
                2_000_000_000L,
                0L, 0, 0L, 0L, null,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        ));
        winningNumberRepository.save(new WinningNumber(
                1199,
                LocalDate.of(2026, 6, 6),
                1, 9, 17, 23, 31, 45,
                8,
                1_800_000_000L,
                0L, 0, 0L, 0L, null,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        ));
    }

    @Test
    @DisplayName("최신 회차 조회 엔드포인트가 최신 회차 정보와 요청 ID를 반환하는지 확인")
    void latestRoundEndpointReturnsLatestRoundAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.round", is(1200)))
                .andExpect(jsonPath("$.numbers", hasSize(6)))
                .andExpect(jsonPath("$.bonusNumber", is(7)));
    }

    @Test
    @DisplayName("번호 추천 엔드포인트가 요청된 개수만큼 번호를 반환하는지 확인")
    void recommendEndpointReturnsRequestedCount() throws Exception {
        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 2,
                                  "excludedNumbers": [1, 2, 3]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations", hasSize(2)))
                .andExpect(jsonPath("$.recommendations[0]", hasSize(6)));
    }

    @Test
    @DisplayName("번호 저장 엔드포인트가 디바이스 토큰별로 해싱되며 멱등성을 유지하는지 확인")
    void savedNumbersEndpointsHashByDeviceTokenAndAreIdempotent() throws Exception {
        String payload = """
                {
                  "numbers": [4, 8, 15, 16, 23, 42],
                  "label": "favorites",
                  "source": "MANUAL"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numbers", hasSize(6)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long savedId = extractId(response);

        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) savedId)));

        mockMvc.perform(get("/api/v1/saved").header("X-Device-Token", "device-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("favorites")));

        mockMvc.perform(delete("/api/v1/saved/{id}", savedId).header("X-Device-Token", "device-1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/saved").header("X-Device-Token", "device-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("번호 저장 시 디바이스 토큰 헤더가 필수인지 확인")
    void savedNumbersRequireDeviceTokenHeader() throws Exception {
        mockMvc.perform(get("/api/v1/saved"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("DEVICE_TOKEN_REQUIRED")))
                .andExpect(jsonPath("$.message", is("X-Device-Token 헤더가 필요합니다.")));
    }

    @Test
    @DisplayName("운영 요약 정보 조회 시 토큰이 필요하며 최신 회차 정보를 반환하는지 확인")
    void opsSummaryRequiresTokenAndReturnsLatestRound() throws Exception {
        mockMvc.perform(get("/ops/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("OPS_UNAUTHORIZED")));

        mockMvc.perform(get("/ops/summary").header("X-Ops-Token", "test-ops-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("정상")))
                .andExpect(jsonPath("$.timezone", is("Asia/Seoul")))
                .andExpect(jsonPath("$.latestRound", is(1200)));
    }

    @Test
    @DisplayName("운영 로그 조회 시 토큰이 필요하며 최근 항목을 반환하는지 확인")
    void opsLogsRequiresTokenAndReturnsRecentEntries() throws Exception {
        MvcResult result = mockMvc.perform(post("/ops/rounds")
                        .header("X-Ops-Token", "test-ops-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "round": 1201,
                                  "drawDate": "2026-06-20",
                                  "numbers": [5, 12, 18, 27, 36, 44],
                                  "bonusNumber": 9,
                                  "firstPrizeAmount": 2100000000
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");

        mockMvc.perform(get("/ops/logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("OPS_UNAUTHORIZED")));

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].operationType", is("MANUAL_UPSERT")))
                .andExpect(jsonPath("$.items[0].executionStatus", is("SUCCESS")))
                .andExpect(jsonPath("$.items[0].round", is(1201)))
                .andExpect(jsonPath("$.items[0].requestId", is(requestId)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(10)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("운영 로그 조회 시 유형, 상태, 회차별 필터링을 지원하는지 확인")
    void opsLogsSupportsFilteringByTypeStatusAndRound() throws Exception {
        mockMvc.perform(post("/ops/rounds")
                        .header("X-Ops-Token", "test-ops-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "round": 1201,
                                  "drawDate": "2026-06-20",
                                  "numbers": [5, 12, 18, 27, 36, 44],
                                  "bonusNumber": 9,
                                  "firstPrizeAmount": 2100000000
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/ops/collect/latest")
                        .header("X-Ops-Token", "test-ops-token"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("operationType", "external_collect")
                        .param("executionStatus", "success")
                        .param("round", "1202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].operationType", is("EXTERNAL_COLLECT")))
                .andExpect(jsonPath("$.items[0].executionStatus", is("SUCCESS")))
                .andExpect(jsonPath("$.items[0].round", is(1202)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("운영 로그 조회 시 알 수 없는 필터 값에 대해 에러를 반환하는지 확인")
    void opsLogsRejectsUnknownFilterValues() throws Exception {
        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("operationType", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_OPERATION_TYPE")));

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("executionStatus", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_EXECUTION_STATUS")));
    }

    @Test
    @DisplayName("운영 로그 조회 시 한국 표준시(KST) 기준 날짜 범위 필터링을 지원하는지 확인")
    void opsLogsSupportsDateRangeFilteringInKst() throws Exception {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        mockMvc.perform(post("/ops/rounds")
                        .header("X-Ops-Token", "test-ops-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "round": 1201,
                                  "drawDate": "2026-06-20",
                                  "numbers": [5, 12, 18, 27, 36, 44],
                                  "bonusNumber": 9,
                                  "firstPrizeAmount": 2100000000
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("from", "2099-01-01")
                        .param("to", "2099-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("운영 로그 조회 시 잘못된 날짜 형식의 필터에 대해 에러를 반환하는지 확인")
    void opsLogsRejectsInvalidDateFilters() throws Exception {
        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("from", "2026/06/13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_FROM_DATE")));

        mockMvc.perform(get("/ops/logs")
                        .header("X-Ops-Token", "test-ops-token")
                        .param("to", "2026/06/13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_TO_DATE")));
    }

    @Test
    @DisplayName("운영 회차 수동 등록 시 회차가 생성되거나 업데이트되는지 확인")
    void opsRoundUpsertCreatesAndUpdatesRound() throws Exception {
        mockMvc.perform(post("/ops/rounds")
                        .header("X-Ops-Token", "test-ops-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "round": 1201,
                                  "drawDate": "2026-06-20",
                                  "numbers": [5, 12, 18, 27, 36, 44],
                                  "bonusNumber": 9,
                                  "firstPrizeAmount": 2100000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1201)))
                .andExpect(jsonPath("$.bonusNumber", is(9)));

        org.assertj.core.api.Assertions.assertThat(winningNumberOperationLogRepository.findAll())
                .hasSize(1);

        mockMvc.perform(post("/ops/rounds")
                        .header("X-Ops-Token", "test-ops-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "round": 1201,
                                  "drawDate": "2026-06-20",
                                  "numbers": [6, 13, 19, 28, 37, 45],
                                  "bonusNumber": 10,
                                  "firstPrizeAmount": 2200000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1201)))
                .andExpect(jsonPath("$.numbers[0]", is(6)))
                .andExpect(jsonPath("$.bonusNumber", is(10)));

        mockMvc.perform(get("/api/v1/rounds/1201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1201)))
                .andExpect(jsonPath("$.bonusNumber", is(10)));
    }

    @Test
    @DisplayName("최신 회차 수집 시 외부 소스를 사용하고 회차 정보를 저장하는지 확인")
    void opsCollectLatestUsesExternalSourceAndPersistsRound() throws Exception {
        mockMvc.perform(post("/ops/collect/latest")
                        .header("X-Ops-Token", "test-ops-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1201)))
                .andExpect(jsonPath("$.bonusNumber", is(9)));

        org.assertj.core.api.Assertions.assertThat(winningNumberOperationLogRepository.findAll())
                .hasSize(1);

        mockMvc.perform(get("/api/v1/rounds/1201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1201)))
                .andExpect(jsonPath("$.numbers[0]", is(5)))
                .andExpect(jsonPath("$.bonusNumber", is(9)));
    }

    @Test
    @DisplayName("특정 회차 수집 시 요청된 회차를 가져와서 저장하는지 확인")
    void opsCollectSpecificRoundFetchesAndUpsertsRequestedRound() throws Exception {
        mockMvc.perform(post("/ops/collect/1305")
                        .header("X-Ops-Token", "test-ops-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1305)))
                .andExpect(jsonPath("$.bonusNumber", is(9)));

        mockMvc.perform(get("/api/v1/rounds/1305"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.round", is(1305)))
                .andExpect(jsonPath("$.numbers[5]", is(44)));
    }

    @Test
    @DisplayName("특정 회차 수집 시 잘못된 회차 번호에 대해 에러를 반환하는지 확인")
    void opsCollectSpecificRoundRejectsInvalidRound() throws Exception {
        mockMvc.perform(post("/ops/collect/0")
                        .header("X-Ops-Token", "test-ops-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ROUND")));

        org.assertj.core.api.Assertions.assertThat(winningNumberOperationLogRepository.findAll())
                .isEmpty();
    }

    private long extractId(String response) {
        int marker = response.indexOf("\"id\":");
        int start = marker + 5;
        int end = response.indexOf(",", start);
        if (end < 0) {
            end = response.indexOf("}", start);
        }
        return Long.parseLong(response.substring(start, end).trim());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ExternalWinningNumberFetchClient externalWinningNumberFetchClient() {
            return round -> new WinningNumberUpsertRequest(
                    round,
                    LocalDate.of(2026, 6, 20),
                    java.util.List.of(5, 12, 18, 27, 36, 44),
                    9,
                    2_100_000_000L,
                    null, null, null, null, null
            );
        }
    }
}
