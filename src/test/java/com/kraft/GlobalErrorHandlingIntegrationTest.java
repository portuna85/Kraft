package com.kraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("전역 예외 핸들러 — 파라미터 오류 400 변환 회귀 테스트")
class GlobalErrorHandlingIntegrationTest extends BaseApiIntegrationTest {

    @Test
    @DisplayName("필수 쿼리 파라미터 누락 시 400 오류를 반환한다")
    void missingRequiredQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/numbers/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MISSING_PARAMETER")));
    }

    @Test
    @DisplayName("쿼리 파라미터 타입 불일치 시 400 오류를 반환한다")
    void queryParamTypeMismatch_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/numbers/check").param("numbers", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER_TYPE")));
    }

    @Test
    @DisplayName("경로 변수 타입 불일치 시 400 오류를 반환한다")
    void pathVariableTypeMismatch_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER_TYPE")));
    }
}
