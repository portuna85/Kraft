package com.kraft.admin;

import com.kraft.winningnumber.WinningNumberCommandService;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("관리자 컨트롤러 스모크 테스트")
class AdminControllerSmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    WinningNumberCommandService commandService;

    @Autowired
    WinningNumberRepository winningNumberRepository;

    @BeforeEach
    void setUp() {
        winningNumberRepository.deleteAll();
        commandService.upsert(new WinningNumberUpsertRequest(
                1, LocalDate.of(2024, 1, 6),
                List.of(1, 2, 3, 4, 5, 6), 7,
                1_000_000_000L, null, null, null, null
        ));
    }

    @AfterEach
    void tearDown() {
        winningNumberRepository.deleteAll();
    }

    @Test
    @DisplayName("대시보드 조회 시 200 응답을 반환한다")
    void dashboard_returns200() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회차 관리 페이지 조회 시 200 응답을 반환한다")
    void rounds_returns200() throws Exception {
        mockMvc.perform(get("/admin/rounds").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("위조 요청 방지 토큰 없이 로그인 시도 시 만료 페이지로 리다이렉트된다")
    void loginPost_withoutCsrf_redirectsToExpiredLoginPage() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "unused"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?expired"));
    }
}
