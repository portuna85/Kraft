package com.kraft.common.config;

import jakarta.servlet.http.HttpServlet;
import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

// h2database는 compileOnly/developmentOnly로 선언되어 있어 bootJar(컨테이너 런타임)에는
// 포함되지 않는다(build.gradle.kts 참고) — IntelliJ bootRun이나 테스트에서는 클래스패스에
// 있지만, "local" 프로필로 컨테이너를 띄우면(docker-compose.yml 기본값) 이 클래스가 없어
// 기동 자체가 실패했다. h2 콘솔 클래스가 실제로 있을 때만 이 설정을 활성화해 방지한다.
@Configuration
@Profile("local")
@ConditionalOnClass(JakartaWebServlet.class)
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
