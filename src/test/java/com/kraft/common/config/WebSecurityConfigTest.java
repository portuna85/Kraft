package com.kraft.common.config;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("상태 점검 엔드포인트 접근 제어 테스트")
class WebSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("신뢰 대역 주소는 상태 점검 지표 경로에 접근할 수 있다")
    void trustedCidrRemoteAddr_canAccessPrometheus() throws Exception {
        mockMvc.perform(withRemoteAddr(get("/actuator/prometheus"), "172.28.0.5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("신뢰 대역 밖 주소는 상태 점검 지표 경로 접근 시 403을 받는다")
    void untrustedRemoteAddr_isForbiddenFromPrometheus() throws Exception {
        mockMvc.perform(withRemoteAddr(get("/actuator/prometheus"), "8.8.8.8"))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder withRemoteAddr(MockHttpServletRequestBuilder builder, String remoteAddr) {
        return builder.with(request -> {
            request.setRemoteAddr(remoteAddr);
            return request;
        });
    }
}
