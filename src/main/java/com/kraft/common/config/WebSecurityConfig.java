package com.kraft.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    // CSRF is intentionally disabled for this stateless REST API chain.
    // SessionCreationPolicy.STATELESS means no session cookie is ever issued,
    // so cross-site requests cannot piggyback on an authenticated session —
    // the precondition for a CSRF attack does not exist here.
    // The admin UI chain (Order 1) uses form-login + sessions and keeps CSRF enabled.
    @Bean
    @Order(2)
    SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**", "/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
