package com.kraft.admin;

import com.kraft.winningnumber.WinningNumberCommandService;
import com.kraft.winningnumber.WinningNumberRepository;
import com.kraft.winningnumber.WinningNumberUpsertRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    void dashboard_returns200() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void rounds_returns200() throws Exception {
        mockMvc.perform(get("/admin/rounds").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void loginPost_withoutCsrf_redirectsToExpiredLoginPage() throws Exception {
        mockMvc.perform(post("/admin/login")
                        .param("username", "admin")
                        .param("password", "unused"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?expired"));
    }
}
