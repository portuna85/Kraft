package com.kraft.lotto.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import com.kraft.lotto.feature.admin.application.AdminAuditLogService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AdminSecurityConfig")
class AdminSecurityConfigTest {

    @Test
    @DisplayName("기본 관리자 계정은 설정된 해시 비밀번호를 사용한다")
    void defaultAdminUsesConfiguredPasswordHash() {
        AdminSecurityConfig config = config(new KraftAdminProperties(
                true,
                "admin.example.test",
                encoder().encode("secret-password"),
                null
        ));
        PasswordEncoder passwordEncoder = config.adminPasswordEncoder();

        UserDetails user = config.adminUserDetailsService(passwordEncoder).loadUserByUsername("admin");

        assertThat(user.getPassword()).doesNotStartWith("{noop}");
        assertThat(passwordEncoder.matches("secret-password", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("관리자가 활성화되었는데 해시가 없으면 실패한다")
    void enabledAdminRequiresPasswordHash() {
        AdminSecurityConfig config = config(new KraftAdminProperties(
                true,
                "admin.example.test",
                "",
                null
        ));

        assertThatIllegalStateException()
                .isThrownBy(() -> config.adminUserDetailsService(config.adminPasswordEncoder()))
                .withMessageContaining("Admin password hash is required");
    }

    @Test
    @DisplayName("관리자 계정 목록은 사용자별 해시와 권한을 사용한다")
    void configuredUsersUsePasswordHashesAndRoles() {
        String passwordHash = encoder().encode("operator-password");
        AdminSecurityConfig config = config(new KraftAdminProperties(
                true,
                "admin.example.test",
                null,
                List.of(new KraftAdminProperties.AdminUser("operator", passwordHash, List.of("ADMIN_OPERATOR")))
        ));
        PasswordEncoder passwordEncoder = config.adminPasswordEncoder();

        UserDetailsService userDetailsService = config.adminUserDetailsService(passwordEncoder);
        UserDetails user = userDetailsService.loadUserByUsername("operator");

        assertThat(user.getPassword()).isEqualTo(passwordHash);
        assertThat(passwordEncoder.matches("operator-password", user.getPassword())).isTrue();
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN_OPERATOR");
    }

    @Test
    @DisplayName("Spring Security id prefix가 없는 해시는 거부한다")
    void rejectsPasswordHashWithoutDelegatingPrefix() {
        AdminSecurityConfig config = config(new KraftAdminProperties(
                true,
                "admin.example.test",
                "$2a$12$missing-prefix",
                null
        ));

        assertThatIllegalStateException()
                .isThrownBy(() -> config.adminUserDetailsService(config.adminPasswordEncoder()))
                .withMessageContaining("Spring Security id prefix");
    }

    private static AdminSecurityConfig config(KraftAdminProperties properties) {
        return new AdminSecurityConfig(properties, mock(AdminAuditLogService.class));
    }

    private static PasswordEncoder encoder() {
        return new AdminSecurityConfig(
                new KraftAdminProperties(false, "admin.example.test", null, null),
                mock(AdminAuditLogService.class)
        ).adminPasswordEncoder();
    }
}
