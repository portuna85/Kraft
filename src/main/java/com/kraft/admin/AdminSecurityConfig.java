package com.kraft.admin;

import com.kraft.common.web.ClientIpResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class AdminSecurityConfig {

    private final AdminUserDetailsService userDetailsService;
    private final AdminLoginHandler loginHandler;
    private final AdminLoginAttemptService lockout;
    private final ClientIpResolver ipResolver;

    public AdminSecurityConfig(AdminUserDetailsService userDetailsService,
                               AdminLoginHandler loginHandler,
                               AdminLoginAttemptService lockout,
                               ClientIpResolver ipResolver) {
        this.userDetailsService = userDetailsService;
        this.loginHandler = loginHandler;
        this.lockout = lockout;
        this.ipResolver = ipResolver;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager adminAuthManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .addFilterBefore(new AdminLockoutFilter(lockout, ipResolver),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .successHandler(loginHandler)
                        .failureHandler(loginHandler))
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(sm -> sm.maximumSessions(1))
                .csrf(Customizer.withDefaults())
                .build();
    }
}
