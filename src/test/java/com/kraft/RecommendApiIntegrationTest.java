package com.kraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("번호 추천 API 통합 테스트")
class RecommendApiIntegrationTest extends BaseApiIntegrationTest {

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
    @DisplayName("번호 추천 시 최대 10세트까지 허용하는지 확인")
    void recommendAllowsUpToTenSets() throws Exception {
        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations", hasSize(10)));

        mockMvc.perform(post("/api/v1/numbers/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"count":11}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }
}
