package com.kraft.lotto.infra.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class AdminSecurityConfig {

    private final KraftAdminProperties adminProperties;
    private final AdminAuditLogService auditLogService;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public AdminSecurityConfig(KraftAdminProperties adminProperties,
                               ObjectProvider<AdminAuditLogService> auditLogServiceProvider) {
        this.adminProperties = adminProperties;
        this.auditLogService = auditLogServiceProvider.getIfAvailable();
    }

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/ops", "/admin/ops/**").authenticated()
                        .requestMatchers("/admin/login", "/admin/login/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
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
            AdminAuthSuccessHandler successHandler =
                    new AdminAuthSuccessHandler(adminProperties.allowedEmails(), auditLogService);
            AdminAuthFailureHandler failureHandler =
                    new AdminAuthFailureHandler(auditLogService);

            http.oauth2Login(oauth -> oauth
                    .loginPage("/admin/login")
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            ).logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
            );
        }

        return http.build();
    }
}
