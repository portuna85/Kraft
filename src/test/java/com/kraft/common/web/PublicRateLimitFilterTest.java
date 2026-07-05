package com.kraft.common.web;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = Application.class,
        properties = {
                "kraft.security.rate-limit-per-minute=1",
                "kraft.security.rate-limit-max-keys=100"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("공개 요청 제한 필터 테스트")
class PublicRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("429 응답에 재시도 안내 헤더가 포함되는지 확인")
    void rateLimitExceeded_returnsRetryAfterHeader() throws Exception {
        mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(header().string("X-RateLimit-Limit", "1"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }
}
