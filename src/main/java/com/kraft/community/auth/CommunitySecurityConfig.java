package com.kraft.community.auth;

import com.kraft.common.config.CommunityProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
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
    private final MeterRegistry meterRegistry;

    public CommunitySecurityConfig(CommunityOAuth2UserService communityOAuth2UserService,
                                    CommunityAuthEntryPoint communityAuthEntryPoint,
                                    CommunityProperties communityProperties,
                                    MeterRegistry meterRegistry) {
        this.communityOAuth2UserService = communityOAuth2UserService;
        this.communityAuthEntryPoint = communityAuthEntryPoint;
        this.communityProperties = communityProperties;
        this.meterRegistry = meterRegistry;
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
                        .authenticationEntryPoint(communityAuthEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    // CSRF 거부율 관측(§7 5단계: "409/CSRF 거부율" 메트릭). CsrfException 외의 접근 거부는
    // 카운트 없이 그대로 403으로 내려보낸다.
    private AccessDeniedHandler accessDeniedHandler() {
        Counter csrfRejectedCounter = Counter.builder("kraft_community_csrf_rejected_total")
                .description("커뮤니티 체인에서 CSRF 토큰 불일치로 거부된 요청 수")
                .register(meterRegistry);
        return (request, response, accessDeniedException) -> {
            if (accessDeniedException instanceof CsrfException) {
                csrfRejectedCounter.increment();
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
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
