package com.kraft.common.web;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
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

    @Test
    @DisplayName("preflight OPTIONS 요청은 레이트리밋 카운트를 소모하지 않는다")
    void preflightOptions_doesNotConsumeRateLimit() throws Exception {
        // limit=1이므로 OPTIONS가 카운트를 소모하면 뒤따르는 GET이 429가 된다
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(options("/api/v1/stats/frequency")
                            .with(remoteAddr("10.9.9.1"))
                            .header("Origin", "https://example.com")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/stats/frequency")
                        .with(remoteAddr("10.9.9.1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("한도 초과 429 응답에도 CORS 헤더가 포함된다")
    void rateLimitExceeded_includesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/stats/frequency")
                        .with(remoteAddr("10.9.9.2"))
                        .header("Origin", "https://example.com"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/stats/frequency")
                        .with(remoteAddr("10.9.9.2"))
                        .header("Origin", "https://example.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    private static RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
