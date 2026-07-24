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

/**
 * 두 provider 모두 profile이 비활성화된 경우. "부분 설정"(id만 있고 secret이 없는 경우 등)은
 * 이 저장소에서는 provider profile 자체가 활성화되지 않는 것으로 귀결되므로 이 클래스가
 * 그 케이스도 대표한다 — profile이 없으면 CommunityOAuth2FallbackConfig의 빈 repository가
 * 사용되어 실제 자격 증명 완전성과 무관하게 registration이 없는 것과 동일하게 동작한다.
 */
@SpringBootTest(
        classes = Application.class,
        properties = {
            "spring.profiles.active=oauth-registration-test",
            "spring.datasource.url=jdbc:h2:mem:oauth-registration-neither;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.sql.init.mode=never",
            "kraft.ops.token=test-ops-token"
        })
@AutoConfigureMockMvc
@DisplayName("어떤 provider도 활성화되지 않은 OAuth registration")
class CommunityOAuthRegistrationNeitherTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("아무 provider도 활성화되지 않으면 세션 응답의 activeProviders가 빈 배열이다")
    void sessionResponse_listsNoProviders() throws Exception {
        mockMvc.perform(get("/api/v1/community/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProviders", org.hamcrest.Matchers.empty()));
    }
}
