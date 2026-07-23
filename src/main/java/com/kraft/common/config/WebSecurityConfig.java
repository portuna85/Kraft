package com.kraft.common.config;

import com.kraft.common.web.ClientIpResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final ClientIpResolver clientIpResolver;

    public WebSecurityConfig(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    // This chain is fully stateless (STATELESS session policy) — no session cookie
    // is ever issued, so cross-site requests cannot piggyback on an authenticated
    // session. CSRF attacks require session-based auth; the precondition does not
    // exist here. We configure the matcher to never match rather than calling
    // csrf.disable(), which is semantically equivalent but avoids static-analysis
    // false positives on the blanket-disable pattern.
    //
    // @Order(3): admin(@Order(1)) → community(@Order(2), CommunitySecurityConfig)보다
    // 뒤로 밀려야 한다. 이 체인의 matcher("/api/**")가 "/api/v1/community/**"를 포함하므로,
    // 순서가 community보다 앞서면 커뮤니티 인증이 무음으로 우회된다(§4.1).
    @Bean
    @Order(3)
    SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**", "/actuator/**", "/ops/**")
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                        new NegatedRequestMatcher(AnyRequestMatcher.INSTANCE)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // health 엔드포인트: 로드밸런서·k8s probe용 공개
                        .requestMatchers("/actuator/health/**").permitAll()
                        // prometheus 스크래핑: 내부 Docker 네트워크(trusted CIDR, 콤마 구분 다중 CIDR 지원)만 허용
                        .requestMatchers("/actuator/**").access((a, ctx) ->
                                new AuthorizationDecision(clientIpResolver.isTrustedProxy(ctx.getRequest().getRemoteAddr())))
                        .anyRequest().permitAll())
                .build();
    }
}
