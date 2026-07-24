package com.kraft.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

@DisplayName("커뮤니티 OAuth client 설정")
class CommunityOAuthClientConfigTest {

    private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration.";

    @Test
    @DisplayName("Google은 profile만 요청하고 Naver에는 불필요한 scope를 전송하지 않는다")
    void requestsOnlyRequiredProfileScopes() throws IOException {
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));

        assertThat(values(documents, REGISTRATION_PREFIX + "google.scope"))
                .containsExactly("profile");
        assertThat(values(documents, REGISTRATION_PREFIX + "naver.scope"))
                .isEmpty();
    }

    private static List<Object> values(List<PropertySource<?>> documents, String propertyName) {
        return documents.stream()
                .map(document -> document.getProperty(propertyName))
                .filter(Objects::nonNull)
                .toList();
    }
}
