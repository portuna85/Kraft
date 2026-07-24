package com.kraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("회차 조회 통합 테스트")
class RoundsApiIntegrationTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("최신 회차 조회 엔드포인트가 최신 회차 정보와 요청 식별자를 반환하는지 확인")
    void latestRoundEndpointReturnsLatestRoundAndRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/latest"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.round", is(2)))
                .andExpect(jsonPath("$.numbers", hasSize(6)))
                .andExpect(jsonPath("$.bonusNumber", is(7)));
    }

    @Test
    @DisplayName("공개 회차 목록 엔드포인트는 제거되어 404를 반환한다")
    void roundsListEndpointIsRemoved() throws Exception {
        mockMvc.perform(get("/api/v1/rounds"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("공개 회차 상세 엔드포인트는 제거되어 404를 반환한다")
    void roundsByRoundEndpointIsRemoved() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/1"))
                .andExpect(status().isNotFound());
    }
}
