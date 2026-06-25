package com.kraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("회차 수집 API 통합 테스트")
class CollectionApiIntegrationTest extends BaseApiIntegrationTest {

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
}
