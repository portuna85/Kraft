package com.kraft.community.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kraft.Application;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = Application.class,
        properties = {
            "spring.profiles.active=oauth-registration-test",
            "spring.profiles.include=community-google-oauth,community-naver-oauth",
            "spring.datasource.url=jdbc:h2:mem:oauth-registration;MODE=MySQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.sql.init.mode=never",
            "kraft.ops.token=test-ops-token",
            "KRAFT_COMMUNITY_GOOGLE_CLIENT_ID=test-google-client-id",
            "KRAFT_COMMUNITY_GOOGLE_CLIENT_SECRET=test-google-client-secret",
            "KRAFT_COMMUNITY_NAVER_CLIENT_ID=test-naver-client-id",
            "KRAFT_COMMUNITY_NAVER_CLIENT_SECRET=test-naver-client-secret"
        })
@AutoConfigureMockMvc
@DisplayName("운영 방식 OAuth registration 활성화")
class CommunityOAuthRegistrationConfigTest {

    @Autowired
    private ClientRegistrationRepository registrations;

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("provider profile과 ID·secret이 있으면 Google과 Naver가 모두 등록된다")
    void registersConfiguredProviders() {
        assertThat(environment.getProperty(
                "spring.security.oauth2.client.registration.google.client-id"))
                .isEqualTo("test-google-client-id");
        assertThat(environment.getProperty(
                "spring.security.oauth2.client.registration.naver.client-id"))
                .isEqualTo("test-naver-client-id");
        assertThat(registrations.findByRegistrationId("google")).isNotNull();
        assertThat(registrations.findByRegistrationId("naver")).isNotNull();
    }

    @Test
    @DisplayName("Google과 Naver 로그인 진입점이 provider authorization URL로 리다이렉트된다")
    void redirectsToProviderAuthorizationEndpoints() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        startsWith("https://accounts.google.com/o/oauth2/v2/auth?")));

        mockMvc.perform(get("/oauth2/authorization/naver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        startsWith("https://nid.naver.com/oauth2.0/authorize?")));
    }
}
