package com.kraft.community.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * google/naver provider profile이 모두 비활성화되면 Spring Boot의 OAuth2ClientAutoConfiguration이
 * 통째로 백오프해 ClientRegistrationRepository 빈 자체가 없다. CommunitySecurityConfig의
 * oauth2Login()이 이 빈을 요구하므로 등록이 0개인 리포지토리를 폴백으로 제공한다.
 *
 * provider profile이 하나라도 활성화된 경우에는 이 빈을 만들면 안 된다. 사용자 구성의 fallback
 * 빈이 먼저 등록되면 Boot의 실제 ClientRegistrationRepository 자동 구성이 백오프하기 때문이다.
 */
@Configuration
public class CommunityOAuth2FallbackConfig {

    @Bean
    @Profile("!community-google-oauth & !community-naver-oauth")
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository emptyClientRegistrationRepository() {
        return registrationId -> null;
    }
}
