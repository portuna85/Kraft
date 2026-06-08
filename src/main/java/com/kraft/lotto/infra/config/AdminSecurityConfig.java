package com.kraft.lotto.infra.config;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import com.kraft.lotto.feature.admin.application.AdminLoginLockoutService;
import java.util.List;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AdminLoginLockoutService lockoutService;

    public AdminSecurityConfig(KraftAdminProperties adminProperties,
                               AdminAuditLogService auditLogService,
                               AdminLoginLockoutService lockoutService) {
        this.adminProperties = adminProperties;
        this.auditLogService = auditLogService;
        this.lockoutService = lockoutService;
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
                        .requestMatchers("/admin/ops/cache", "/admin/ops/cache/**")
                            .hasAnyRole("ADMIN_OPERATOR", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/ops", "/admin/ops/**")
                            .hasAnyRole("ADMIN_VIEWER", "ADMIN_OPERATOR",
                                        "ADMIN_NEWS_MANAGER", "ADMIN_AUDITOR", "ADMIN_SUPER_ADMIN")
                        .requestMatchers("/admin/login", "/admin/login/**").permitAll()
                        .requestMatchers("/actuator", "/actuator/**").permitAll()
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
            AdminAuthSuccessHandler successHandler = new AdminAuthSuccessHandler(auditLogService, lockoutService);
            AdminAuthFailureHandler failureHandler = new AdminAuthFailureHandler(auditLogService, lockoutService);

            http.addFilterBefore(new AdminLoginLockoutFilter(lockoutService),
                    UsernamePasswordAuthenticationFilter.class);

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
    public PasswordEncoder adminPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService adminUserDetailsService(PasswordEncoder passwordEncoder) {
        if (adminProperties.hasConfiguredUsers()) {
            List<UserDetails> users = adminProperties.users().stream()
                    .map(u -> User.builder()
                            .username(u.username())
                            .password(requireDelegatingHash(u.passwordHash(), "admin user " + u.username()))
                            .roles(u.roles().toArray(new String[0]))
                            .build())
                    .collect(Collectors.toList());
            log.info("[ADMIN] {}명의 관리자 계정을 설정에서 로드합니다.", users.size());
            return new InMemoryUserDetailsManager(users);
        }

        boolean hasExplicitPasswordHash = adminProperties.adminPasswordHash() != null
                && !adminProperties.adminPasswordHash().isBlank();

        if (adminProperties.enabled() && !hasExplicitPasswordHash) {
            throw new IllegalStateException("Admin password hash is required when admin is enabled");
        }

        String passwordHash = hasExplicitPasswordHash
                ? requireDelegatingHash(adminProperties.adminPasswordHash(), "default admin")
                : passwordEncoder.encode(UUID.randomUUID().toString());

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordHash)
                .roles("ADMIN_SUPER_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    private static String requireDelegatingHash(String passwordHash, String owner) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalStateException("Admin password hash is required for " + owner);
        }
        String trimmed = passwordHash.trim();
        if (!trimmed.startsWith("{") || trimmed.indexOf('}') <= 1) {
            throw new IllegalStateException("Admin password hash must include a Spring Security id prefix for " + owner);
        }
        return trimmed;
    }
}
