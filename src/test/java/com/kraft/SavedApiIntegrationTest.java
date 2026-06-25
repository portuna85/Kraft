package com.kraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("저장 번호 API 통합 테스트")
class SavedApiIntegrationTest extends BaseApiIntegrationTest {

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

        // Token must be 32-128 chars (DeviceTokenSupport.requireHashedToken)
        String deviceToken = "test-device-token-for-integration-test-01";

        String response = mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created", is(true)))
                .andExpect(jsonPath("$.savedNumber.numbers", hasSize(6)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long savedId = extractId(response);

        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(false)))
                .andExpect(jsonPath("$.savedNumber.id", is((int) savedId)));

        mockMvc.perform(get("/api/v1/saved").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label", is("favorites")));

        mockMvc.perform(delete("/api/v1/saved/{id}", savedId).header("X-Device-Token", deviceToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/saved").header("X-Device-Token", deviceToken))
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
    @DisplayName("번호 저장 시 번호 개수 및 범위 검증이 동작하는지 확인")
    void savedNumbersValidatesCountAndRange() throws Exception {
        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", "device-v")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numbers":[1,2,3,4,5],"source":"MANUAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", "device-v")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numbers":[1,2,3,4,5,46],"source":"MANUAL"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("저장 번호와 최신 추첨 결과 비교 엔드포인트가 등수와 일치 번호 수를 반환하는지 확인")
    void savedResultsEndpointReturnsMatchCountAndPrizeTier() throws Exception {
        String deviceToken = "test-device-token-for-results-endpoint-test";

        // 1등: 최신 회차 번호 그대로 저장
        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numbers\":[3,11,19,28,34,42],\"source\":\"MANUAL\"}"))
                .andExpect(status().isCreated());

        // 2등: 5개 일치 + 보너스
        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numbers\":[3,11,19,28,34,7],\"source\":\"MANUAL\"}"))
                .andExpect(status().isCreated());

        // 낙첨: 1개 일치
        mockMvc.perform(post("/api/v1/saved")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numbers\":[1,2,3,4,5,6],\"source\":\"MANUAL\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/saved/results").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // 저장 순서 역순(최신 먼저)으로 반환됨
                .andExpect(jsonPath("$[0].prizeTier", is("낙첨")))
                .andExpect(jsonPath("$[0].matchedCount", is(1)))
                .andExpect(jsonPath("$[0].bonusMatch", is(false)))
                .andExpect(jsonPath("$[1].prizeTier", is("2등")))
                .andExpect(jsonPath("$[1].matchedCount", is(5)))
                .andExpect(jsonPath("$[1].bonusMatch", is(true)))
                .andExpect(jsonPath("$[2].prizeTier", is("1등")))
                .andExpect(jsonPath("$[2].matchedCount", is(6)))
                .andExpect(jsonPath("$[2].round", is(1200)));
    }

    @Test
    @DisplayName("저장 번호가 없으면 결과 비교 엔드포인트가 빈 배열을 반환하는지 확인")
    void savedResultsEndpointReturnsEmptyWhenNoSavedNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/saved/results")
                        .header("X-Device-Token", "test-device-token-for-results-empty-test-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
