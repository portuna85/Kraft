package com.kraft.common.web;

import com.kraft.Application;
import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("공개 API 캐시 제어 필터 테스트")
class PublicApiCacheControlFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @BeforeEach
    void seedRound1() {
        winningNumberRepository.deleteAll();
        winningNumberRepository.save(new WinningNumber(
                1,
                LocalDate.of(2002, 12, 7),
                10, 23, 29, 33, 37, 40,
                16,
                857_956_000L,
                0L, 0, 0L, 0L,
                OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
        ));
    }

    @Test
    @DisplayName("If-None-Match가 일치하면 304를 빈 본문으로 반환한다")
    void matchingIfNoneMatch_returnsNotModifiedWithEmptyBody() throws Exception {
        MvcResult first = mockMvc.perform(get("/api/v1/stats/frequency"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Cache-Control", "public, max-age=60, must-revalidate"))
                .andReturn();

        String etag = first.getResponse().getHeader("ETag");

        mockMvc.perform(get("/api/v1/stats/frequency").header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(header().string("Cache-Control", "public, max-age=60, must-revalidate"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEmpty());
    }

    @Test
    @DisplayName("과거 회차 조회는 통계/최신 회차보다 긴 캐시를 받는다")
    void historicalRoundPath_getsLongerCacheControl() throws Exception {
        mockMvc.perform(get("/api/v1/rounds/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=86400, stale-while-revalidate=3600"));
    }
}
