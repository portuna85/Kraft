package com.kraft.community.auth;

import com.kraft.common.config.CommunityProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 커뮤니티 OAuth2 로그인 체인. admin(@Order(1))보다 뒤, public API(@Order(2)→(3)으로
 * 재번호)보다 앞에 위치해야 /api/v1/community/**가 public 체인의 넓은 /api/** matcher에
 * 선점당하지 않는다(§4.1). ADR-0001: Phase 1 servlet 세션. ADR-0002: CookieCsrfTokenRepository
 * double-submit.
 */
@Configuration
public class CommunitySecurityConfig {

    @Value("${kraft.public-base-url:}")
    private String publicBaseUrl;

    private final CommunityOAuth2UserService communityOAuth2UserService;
    private final CommunityAuthEntryPoint communityAuthEntryPoint;
    private final CommunityProperties communityProperties;

    public CommunitySecurityConfig(CommunityOAuth2UserService communityOAuth2UserService,
                                    CommunityAuthEntryPoint communityAuthEntryPoint,
                                    CommunityProperties communityProperties) {
        this.communityOAuth2UserService = communityOAuth2UserService;
        this.communityAuthEntryPoint = communityAuthEntryPoint;
        this.communityProperties = communityProperties;
    }

    @Bean
    @Order(2)
    SecurityFilterChain communityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        return http
                .securityMatcher("/api/v1/community/**", "/oauth2/**", "/login/**", "/logout")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/community/session").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/logout").permitAll()
                        // 게시글/댓글 조회는 로그인 없이 공개, 쓰기(POST/PUT/DELETE)만 인증 요구.
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/posts/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterAfter(new CommunityWriteRateLimitFilter(communityProperties), AuthorizationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(communityOAuth2UserService))
                        .successHandler(successHandler())
                        .failureHandler(failureHandler()))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(redirectTarget())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(communityAuthEntryPoint))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    private AuthenticationSuccessHandler successHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler(redirectTarget());
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    private AuthenticationFailureHandler failureHandler() {
        return new SimpleUrlAuthenticationFailureHandler(redirectTarget() + "?login_error=1");
    }

    // 프런트(Next.js)가 별도로 존재하므로(4단계 이식 예정) 로그인/로그아웃 완료 후에는
    // kraft.public-base-url로 돌려보낸다 — 로컬/테스트처럼 값이 없으면 "/"로 대체.
    private String redirectTarget() {
        return (publicBaseUrl == null || publicBaseUrl.isBlank()) ? "/" : publicBaseUrl;
    }
}
