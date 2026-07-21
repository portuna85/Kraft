package com.kraft.recommend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("번호 조합 조회 컨트롤러 테스트")
class RecommendApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("유효한 6개 번호는 200을 반환한다")
    void check_validSixNumbers_returnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/numbers/check")
                        .param("numbers", "1", "2", "3", "4", "5", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wonFirstPrize").exists());
    }

    @Test
    @DisplayName("45 초과 번호가 섞이면 400을 반환한다")
    void check_numberOutOfRange_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/numbers/check")
                        .param("numbers", "1", "2", "3", "4", "5", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("6개가 아닌 개수가 오면 400을 반환한다")
    void check_wrongCount_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/numbers/check")
                        .param("numbers", "1", "2", "3"))
                .andExpect(status().isBadRequest());
    }
}
