package com.kraft.community.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
        classes = Application.class,
        properties = {
            "spring.profiles.active=oauth-registration-test",
            "spring.profiles.include=community-naver-oauth",
            "spring.datasource.url=jdbc:h2:mem:oauth-registration-naver-only;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.sql.init.mode=never",
            "kraft.ops.token=test-ops-token",
            "KRAFT_COMMUNITY_NAVER_CLIENT_ID=test-naver-client-id",
            "KRAFT_COMMUNITY_NAVER_CLIENT_SECRET=test-naver-client-secret"
        })
@AutoConfigureMockMvc
@DisplayName("Naver provider만 활성화된 OAuth registration")
class CommunityOAuthRegistrationNaverOnlyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Naver만 활성화되면 세션 응답의 activeProviders에 naver만 담긴다")
    void sessionResponse_listsNaverOnly() throws Exception {
        mockMvc.perform(get("/api/v1/community/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProviders", org.hamcrest.Matchers.contains("naver")));
    }
}
