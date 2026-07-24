package com.kraft.community.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * google/naver 둘 다 미설정(KRAFT_COMMUNITY_*_OAUTH_ENABLED 둘 다 꺼짐)이면 Spring Boot의
 * OAuth2ClientAutoConfiguration이 통째로 백오프해 ClientRegistrationRepository 빈 자체가
 * 없다 — CommunitySecurityConfig의 oauth2Login()이 이 빈을 요구하므로 그대로면 기동이
 * 실패한다. 등록이 0개인 리포지토리를 폴백으로 제공해 로그인 자체는 불가능하되(존재하지
 * 않는 provider 요청 시 404) 앱은 정상 기동하게 한다.
 */
@Configuration
public class CommunityOAuth2FallbackConfig {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository emptyClientRegistrationRepository() {
        return registrationId -> null;
    }
}
