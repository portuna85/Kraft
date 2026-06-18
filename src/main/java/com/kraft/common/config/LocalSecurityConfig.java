package com.kraft.common.config;

import jakarta.servlet.http.HttpServlet;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        // H2 웹 콘솔은 iframe 기반이므로 CSRF 토큰 전송이 불가능하다.
        // 이 설정은 @Profile("local") 에서만 활성화되며 /h2-console/** 경로에만 적용된다.
        return http
                .securityMatcher("/h2-console/**")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    ServletRegistrationBean<HttpServlet> h2ConsoleServlet() {
        var reg = new ServletRegistrationBean<HttpServlet>(new JakartaWebServlet(), "/h2-console/*");
        reg.setLoadOnStartup(1);
        return reg;
    }
}
