package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class AdminSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityConfig.class);

    private final KraftAdminProperties adminProperties;
    private final AdminAuditLogService auditLogService;

    public AdminSecurityConfig(KraftAdminProperties adminProperties,
                               AdminAuditLogService auditLogService) {
        this.adminProperties = adminProperties;
        this.auditLogService = auditLogService;
    }

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/ops/collection", "/admin/ops/collection/**")
                            .hasAnyRole("ADMIN_OPERATOR", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/ops/news", "/admin/ops/news/**")
                            .hasAnyRole("ADMIN_NEWS_MANAGER", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/ops/audit", "/admin/ops/audit/**")
                            .hasAnyRole("ADMIN_AUDITOR", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/ops", "/admin/ops/**")
                            .hasAnyRole("ADMIN_VIEWER", "ADMIN_OPERATOR",
                                        "ADMIN_NEWS_MANAGER", "ADMIN_AUDITOR", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/login", "/admin/login/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ops/**")
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                )
                .requestCache(cache -> cache
                        .requestCache(new NullRequestCache())
                );

        if (adminProperties.enabled()) {
            AdminAuthSuccessHandler successHandler = new AdminAuthSuccessHandler(auditLogService);
            AdminAuthFailureHandler failureHandler = new AdminAuthFailureHandler(auditLogService);

            http.formLogin(form -> form
                    .loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            ).logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
            );
        }

        return http.build();
    }

    @Bean
    public UserDetailsService adminUserDetailsService() {
        if (adminProperties.hasConfiguredUsers()) {
            List<UserDetails> users = adminProperties.users().stream()
                    .map(u -> User.builder()
                            .username(u.username())
                            .password("{noop}" + u.password())
                            .roles(u.roles().toArray(new String[0]))
                            .build())
                    .collect(Collectors.toList());
            log.info("[ADMIN] {}명의 관리자 계정을 설정에서 로드합니다.", users.size());
            return new InMemoryUserDetailsManager(users);
        }

        boolean hasExplicitPassword = adminProperties.enabled()
                && adminProperties.adminPassword() != null
                && !adminProperties.adminPassword().isBlank();

        if (adminProperties.enabled() && !hasExplicitPassword) {
            log.warn("[ADMIN] KRAFT_ADMIN_PASSWORD 미설정 — 랜덤 패스워드로 기동합니다. "
                    + "운영 환경에서는 반드시 명시적으로 설정하세요.");
        }

        String rawPassword = hasExplicitPassword
                ? adminProperties.adminPassword()
                : UUID.randomUUID().toString();

        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}" + rawPassword)
                .roles("ADMIN_SUPER_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
