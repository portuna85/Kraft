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

}
